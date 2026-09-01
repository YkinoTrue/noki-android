package com.noki.vpn

import com.noki.vpn.data.AuthRefreshApi
import com.noki.vpn.data.AuthTokenRefresher
import com.noki.vpn.data.BackendAuthTokens
import com.noki.vpn.data.BackendDevice
import com.noki.vpn.data.DefaultStoredSettingsFactory
import com.noki.vpn.data.GoogleAuthApi
import com.noki.vpn.data.InMemoryAtomicStoredSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthCoordinatorTest {
    @Test
    fun `google token is exchanged once and only Noki tokens are persisted`() = runBlocking {
        val idToken = "google-id-token-that-must-not-be-persisted"
        val api = FakeGoogleAuthApi(tokens = tokens())
        val store = InMemoryAtomicStoredSettingsStore(DefaultStoredSettingsFactory.create())
        val coordinator = GoogleAuthCoordinator(api, authenticatedSession(store))

        val result = coordinator.login(
            idToken = idToken,
            deviceId = "known-device",
            preparedState = AppUiState(isAuthenticated = true),
        )

        assertEquals(idToken, api.receivedIdToken)
        assertEquals("known-device", api.receivedDeviceId)
        assertEquals("noki-access", result.tokens.accessToken)
        assertEquals("noki-access", store.load().backendAccessToken)
        assertFalse(store.load().toString().contains(idToken))
    }

    @Test
    fun `failed Google exchange keeps current temporary connection available`() = runBlocking {
        val api = FakeGoogleAuthApi(error = IllegalStateException("backend unavailable"))
        val store = InMemoryAtomicStoredSettingsStore(DefaultStoredSettingsFactory.create())
        var temporaryVpnRevoked = false
        val coordinator = GoogleAuthCoordinator(
            authApi = api,
            authenticatedSessionCoordinator = authenticatedSession(
                store = store,
                stageTemporaryVpnForRevoke = { temporaryVpnRevoked = true },
            ),
        )

        val result = runCatching {
            coordinator.login(
                idToken = "google-id-token-that-must-not-be-persisted",
                deviceId = null,
                preparedState = AppUiState(),
            )
        }

        assertTrue(result.isFailure)
        assertFalse(store.load().isAuthenticated)
        assertFalse(temporaryVpnRevoked)
        assertNull(store.load().backendAccessToken)
    }

    private fun authenticatedSession(
        store: InMemoryAtomicStoredSettingsStore,
        stageTemporaryVpnForRevoke: suspend () -> Unit = {},
    ): AuthenticatedSessionCoordinator {
        val authSession = AuthSessionCoordinator(store, AuthTokenRefresher(store, NoRefreshApi))
        return AuthenticatedSessionCoordinator(
            authSessionCoordinator = authSession,
            registerCurrentDevice = { device() },
            bindTokensToRegisteredDevice = { tokens, _ -> tokens },
            stageTemporaryVpnForRevoke = stageTemporaryVpnForRevoke,
            stopTemporaryVpn = {},
            syncBootstrap = { _, state -> state },
            publishAuthenticatedState = {},
        )
    }

    private class FakeGoogleAuthApi(
        private val tokens: BackendAuthTokens? = null,
        private val error: Throwable? = null,
    ) : GoogleAuthApi {
        var receivedIdToken: String? = null
        var receivedDeviceId: String? = null

        override suspend fun googleLogin(idToken: String, deviceId: String?): BackendAuthTokens {
            receivedIdToken = idToken
            receivedDeviceId = deviceId
            error?.let { throw it }
            return requireNotNull(tokens)
        }
    }

    private object NoRefreshApi : AuthRefreshApi {
        override suspend fun refreshAuthToken(
            refreshToken: String,
            deviceId: String?,
            requestId: String?,
        ): BackendAuthTokens = error("refresh is not expected")
    }

    private fun tokens() = BackendAuthTokens(
        accessToken = "noki-access",
        refreshToken = "noki-refresh",
        expiresInSeconds = 3_600,
        refreshExpiresAt = "2026-08-30T00:00:00Z",
    )

    private fun device() = BackendDevice(
        id = "device-1",
        deviceKey = "device-key",
        deviceName = "Phone",
        platform = "android",
        accessRole = "owner",
        isActive = true,
        lastSeenAt = null,
    )
}
