package com.noki.vpn

import com.noki.vpn.data.BackendBootstrapLoader
import com.noki.vpn.data.BackendDevice
import com.noki.vpn.data.BackendApiClient
import com.noki.vpn.data.BackendSubscription
import com.noki.vpn.data.BackendUser
import com.noki.vpn.data.BootstrapPayload
import com.noki.vpn.data.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException

class BackendSyncCoordinatorTest {
    @Test
    fun malformedDeviceInventoryIsNotAnAuthoritativeEmptyList() = runBlocking {
        for (field in listOf("", ",\"devices\":null", ",\"devices\":{}", ",\"devices\":\"unavailable\"")) {
            val client = clientReturningBootstrap(field)
            val result = runCatching { client.bootstrap("token", "device-1", "key-1") }
            assertTrue("Malformed devices must fail parsing: $field", result.exceptionOrNull() is JSONException)
        }
        assertTrue(clientReturningBootstrap(",\"devices\":[]").bootstrap("token", "device-1", "key-1").devices.isEmpty())
    }

    private fun clientReturningBootstrap(field: String) = BackendApiClient(
        baseUrl = "https://api.example.test",
        client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(("""{"user":{"id":"user-1","username":"user","email":"user@example.com","is_active":true,"is_admin":false}""" + field + "}")
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }.build(),
    )

    @Test
    fun successfulBootstrapWithoutCurrentDeviceEndsItsSession() = runBlocking {
        assertRevoked(emptyList(), "device-1", "key-1")
    }

    @Test
    fun inactiveCurrentDeviceCannotBeReplacedByAnotherActiveDevice() = runBlocking {
        assertRevoked(
            listOf(device("device-1", "key-1", false), device("device-2", "key-2")),
            "device-1", "key-1",
        )
    }

    @Test
    fun matchingKeyOnDifferentDeviceCannotOverridePersistedDeviceId() = runBlocking {
        assertRevoked(listOf(device("device-2", "key-1")), "device-1", "key-1")
    }

    @Test
    fun offlineBootstrapRemainsANetworkErrorNotDeviceRevocation() = runBlocking {
        val offline = java.io.IOException("offline")
        val coordinator = coordinator(BackendBootstrapLoader { _, _, _ -> throw offline })
        val result = runCatching { coordinator.syncState(request()) }
        assertSame(offline, result.exceptionOrNull())
    }

    private suspend fun assertRevoked(devices: List<BackendDevice>, id: String, key: String) {
        val coordinator = coordinator(BackendBootstrapLoader { _, _, _ -> payload(devices) })
        val result = runCatching { coordinator.syncState(request().copy(currentDeviceId = id, currentDeviceKey = key)) }
        assertEquals("current_device_access_revoked", result.exceptionOrNull()?.message)
    }

    private fun coordinator(loader: BackendBootstrapLoader) = BackendSyncCoordinator(
        bootstrapLoader = loader,
        androidUpdateLoader = AndroidUpdateStateLoader { _, _, _ -> error("Update must not load for a revoked device") },
        profileAvatarLoader = ProfileAvatarLoader { _, _, _ -> error("Avatar must not load for a revoked device") },
    )

    private fun device(id: String, key: String, active: Boolean = true) = BackendDevice(
        id = id, deviceKey = key, deviceName = "Phone", platform = "android",
        accessRole = "owner", isActive = active, lastSeenAt = null,
    )

    private fun request() = BackendSyncRequest(
        token = "access-token", baseState = AppUiState(), currentDeviceId = "device-1",
        currentDeviceKey = "key-1", previousDeviceAccessRole = "owner", clientLatencyByTarget = emptyMap(),
    )

    private fun payload(devices: List<BackendDevice>) = BootstrapPayload(
        user = BackendUser("user-1", "user", "user@example.com", null, true, false),
        subscription = BackendSubscription("active", "free", null, 1.0, 5.0),
        plans = emptyList(), locations = emptyList(), devices = devices, robokassaReady = false,
    )

    @Test
    fun syncReturnsBackendPatchInsteadOfRequestStateCopy() = runBlocking {
        val bootstrap = BootstrapPayload(
            user = BackendUser(
                id = "user-1",
                username = "backend-user",
                email = "backend@example.com",
                avatarUrl = "https://example.invalid/avatar.jpg",
                isActive = true,
                isAdmin = false,
            ),
            subscription = BackendSubscription(
                status = "active",
                planCode = "free",
                expiresAt = null,
                trafficUsedGb = 1.0,
                trafficLimitGb = 5.0,
            ),
            plans = emptyList(),
            locations = emptyList(),
            devices = listOf(device("device-1", "device-key-1")),
            robokassaReady = false,
        )
        val update = AndroidUpdateUiState(currentVersionName = "0.9.77")
        val coordinator = BackendSyncCoordinator(
            bootstrapLoader = BackendBootstrapLoader { _, _, _ -> bootstrap },
            androidUpdateLoader = AndroidUpdateStateLoader { _, _, _ -> update },
            profileAvatarLoader = ProfileAvatarLoader { _, _, _ -> "file:///cached-avatar.jpg" },
        )
        val requestState = AppUiState(
            userProfile = UserProfile(username = "request-user", avatarUri = "file:///old-avatar.jpg"),
            inlineMessage = "request-only-message",
        )

        val result = coordinator.syncState(
            BackendSyncRequest(
                token = "access-token",
                baseState = requestState,
                currentDeviceId = "device-1",
                currentDeviceKey = "device-key-1",
                previousDeviceAccessRole = "owner",
                clientLatencyByTarget = emptyMap(),
            ),
        )

        assertEquals("backend-user", result.patch.userProfile.username)
        assertEquals("file:///cached-avatar.jpg", result.patch.userProfile.avatarUri)
        assertEquals(update, result.patch.androidUpdate)
        assertEquals(bootstrap.devices, result.backendDevices)
        assertTrue(result.backendLocations.isEmpty())
        assertTrue(result.backendPlans.isEmpty())

        val legacyResult = coordinator.syncState(
            request().copy(
                baseState = requestState,
                currentDeviceId = "",
                currentDeviceKey = "device-key-1",
            ),
        )
        assertEquals(bootstrap.devices, legacyResult.backendDevices)
    }
}
