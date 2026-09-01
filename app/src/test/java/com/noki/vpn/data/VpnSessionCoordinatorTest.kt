package com.noki.vpn.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VpnSessionCoordinatorTest {
    @Test
    fun `fresh prepare sends country and keeps concrete server runtime only`() = runBlocking {
        val api = FakeVpnSessionApi()
        val device = BackendDevice(
            id = "device-id",
            deviceKey = "device-key",
            deviceName = "Phone",
            platform = "android",
            accessRole = "owner",
            isActive = true,
            lastSeenAt = null,
        )
        val settings = DefaultStoredSettingsFactory.create().copy(
            userProfile = UserProfile(
                selectedCountryCode = "DE",
                selectedServerCode = "lv-1",
            ),
            backendDeviceId = device.id,
            backendDeviceKey = device.deviceKey,
        )
        val coordinator = VpnSessionCoordinator(
            context = null,
            repository = FakeVpnSessionStore(),
            backendApi = api,
            challengeSigner = { "signature" },
            endpointSelectionProvider = { _, _, _, _, _ ->
                EndpointSelector.EndpointSelectionResult(
                    profile = VlessProfile(endpointCode = "de-endpoint"),
                    endpointCode = "de-endpoint",
                    endpointRating = "healthy",
                )
            },
        )

        val result = coordinator.prepare("token", settings, knownDevices = listOf(device))

        assertEquals("DE", api.countryCode)
        assertNull(api.locationCode)
        assertEquals("DE", result.settings.userProfile.selectedCountryCode)
        assertEquals("de-2", result.settings.userProfile.selectedServerCode)
    }

    @Test
    fun `country request falls back to stored concrete server for legacy backend`() = runBlocking {
        val api = FakeVpnSessionApi(rejectCountryRequest = true)
        val device = BackendDevice(
            id = "device-id",
            deviceKey = "device-key",
            deviceName = "Phone",
            platform = "android",
            accessRole = "owner",
            isActive = true,
            lastSeenAt = null,
        )
        val settings = DefaultStoredSettingsFactory.create().copy(
            userProfile = UserProfile(
                selectedCountryCode = "LV",
                selectedServerCode = "LV",
            ),
            backendDeviceId = device.id,
            backendDeviceKey = device.deviceKey,
        )
        val coordinator = VpnSessionCoordinator(
            context = null,
            repository = FakeVpnSessionStore(),
            backendApi = api,
            challengeSigner = { "signature" },
            endpointSelectionProvider = { _, _, _, _, _ ->
                EndpointSelector.EndpointSelectionResult(
                    profile = VlessProfile(endpointCode = "de-endpoint"),
                    endpointCode = "de-endpoint",
                    endpointRating = "healthy",
                )
            },
        )

        coordinator.prepare("token", settings, knownDevices = listOf(device))

        assertEquals(listOf("LV" to null, null to "lv"), api.requests)
    }

    @Test
    fun `blank country uses stored concrete server without invalid request`() = runBlocking {
        val api = FakeVpnSessionApi()
        val device = BackendDevice(
            id = "device-id",
            deviceKey = "device-key",
            deviceName = "Phone",
            platform = "android",
            accessRole = "owner",
            isActive = true,
            lastSeenAt = null,
        )
        val settings = DefaultStoredSettingsFactory.create().copy(
            userProfile = UserProfile(
                selectedCountryCode = "",
                selectedServerCode = "lv2",
            ),
            backendDeviceId = device.id,
            backendDeviceKey = device.deviceKey,
        )
        val coordinator = VpnSessionCoordinator(
            context = null,
            repository = FakeVpnSessionStore(),
            backendApi = api,
            challengeSigner = { "signature" },
            endpointSelectionProvider = { _, _, _, _, _ ->
                EndpointSelector.EndpointSelectionResult(
                    profile = VlessProfile(endpointCode = "de-endpoint"),
                    endpointCode = "de-endpoint",
                    endpointRating = "healthy",
                )
            },
        )

        coordinator.prepare("token", settings, knownDevices = listOf(device))

        assertEquals(listOf(null to "lv2"), api.requests)
    }

    private class FakeVpnSessionStore : VpnSessionStore {
        override fun ensureBackendDeviceKey(existing: String): String = existing
        override fun loadEndpointHealth(): Map<String, EndpointHealth> = emptyMap()
        override fun nextEndpointRotationIndex(rotationKey: String): Int = 0
    }

    private class FakeVpnSessionApi(
        private val rejectCountryRequest: Boolean = false,
    ) : VpnSessionApi {
        var countryCode: String? = null
        var locationCode: String? = null
        val requests = mutableListOf<Pair<String?, String?>>()

        override suspend fun vpnAccess(
            token: String,
            deviceId: String?,
            deviceKey: String?,
        ) = BackendVpnAccess(canConnect = true, reason = null, planCode = null)

        override suspend fun registerDevice(
            token: String,
            deviceKey: String?,
            deviceId: String?,
            deviceName: String,
            publicKey: String,
            deviceClaims: List<String>,
            platform: String,
        ): BackendDevice = error("not used")

        override suspend fun createDeviceChallenge(
            token: String,
            deviceId: String,
        ) = BackendDeviceChallenge(deviceId = deviceId, nonce = "nonce", expiresAt = null)

        override suspend fun createVpnSession(
            token: String,
            deviceId: String,
            deviceKey: String?,
            deviceNonce: String,
            deviceSignature: String,
            countryCode: String?,
            locationCode: String?,
            excludeLocationCode: String?,
            profileCode: String,
        ): BackendVpnSession {
            this.countryCode = countryCode
            this.locationCode = locationCode
            requests += countryCode to locationCode
            if (rejectCountryRequest && countryCode != null && locationCode == null) {
                throw BackendException("Location not found", 503)
            }
            return BackendVpnSession(
                canConnect = true,
                profileCode = "tls",
                locationCode = "de-2",
                locationName = "Germany",
                endpointCode = "de-endpoint",
                entryHost = "de.example.com",
                entryPort = 443,
                serverName = "de.example.com",
                proxyType = "vless",
                transport = "tcp",
                transportMode = null,
                security = "tls",
                fingerprint = null,
                requestHost = null,
                path = null,
                alpn = null,
                allowInsecure = false,
                enableMux = false,
                randomUserAgent = false,
                publicKey = null,
                shortId = null,
                vpnUsername = "owner",
                vpnSecret = "secret",
                flow = null,
                planCode = null,
                endpointCandidates = emptyList(),
            )
        }
    }
}
