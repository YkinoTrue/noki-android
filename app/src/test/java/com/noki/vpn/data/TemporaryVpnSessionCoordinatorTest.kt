package com.noki.vpn.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryVpnSessionCoordinatorTest {
    @Test
    fun `valid encrypted lease is reused without issuing another backend lease`() {
        val cached = lease(expiresAt = 100_000L)
        val store = FakeLeaseStore(cached)
        val api = FakeTemporaryVpnApi(session(expiresAt = 200_000L))
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        val result = runBlocking { coordinator.prepare() }

        assertSame(cached, result)
        assertEquals(0, api.challengeCalls)
        assertEquals(0, api.sessionCalls)
    }

    @Test
    fun `fresh challenge is signed and isolated lease is persisted`() {
        val store = FakeLeaseStore(null)
        val api = FakeTemporaryVpnApi(session(expiresAt = 200_000L))
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        val result = runBlocking { coordinator.prepare() }

        assertEquals(1, api.challengeCalls)
        assertEquals(1, api.sessionCalls)
        assertEquals("signed:nonce", api.lastSignature)
        assertEquals("stable-device-key", api.lastChallengeDeviceKey)
        assertEquals("stable-device-key", api.lastSessionDeviceKey)
        assertEquals("Pixel test", api.lastDeviceName)
        assertEquals("android", api.lastPlatform)
        assertEquals(result, store.lease)
        assertEquals("auth-temp-lv", result.profile.endpointCode)
    }

    @Test
    fun `failed revoke leaves encrypted tombstone and retry clears it after backend recovery`() {
        val store = FakeLeaseStore(lease(expiresAt = 100_000L))
        val api = FakeTemporaryVpnApi(session(expiresAt = 200_000L), revokeError = true)
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        val result = runBlocking { coordinator.revokeStoredLease() }

        assertFalse(result)
        assertEquals(null, store.lease)
        assertEquals("session", store.pendingRevoke?.sessionId)
        assertEquals("control", store.pendingRevoke?.controlToken)
        assertTrue(api.revokeCalled)

        api.revokeError = false

        assertTrue(runBlocking { coordinator.retryPendingRevoke() })
        assertEquals(null, store.pendingRevoke)
    }

    @Test
    fun `staging revoke durably removes active lease without starting network cleanup`() {
        val store = FakeLeaseStore(lease(expiresAt = 100_000L))
        val api = FakeTemporaryVpnApi(session(expiresAt = 200_000L))
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        val staged = runBlocking { coordinator.stageStoredLeaseForRevoke() }

        assertEquals("session", staged?.sessionId)
        assertEquals(null, store.lease)
        assertEquals(staged, store.pendingRevoke)
        assertFalse(api.revokeCalled)
    }

    @Test
    fun `pending revoke blocks a new lease while backend remains unavailable`() {
        val pending = TemporaryVpnPendingRevoke(
            sessionId = "old-session",
            controlToken = "old-control",
            expiresAtEpochMillis = 100_000L,
        )
        val store = FakeLeaseStore(lease = null, pendingRevoke = pending)
        val api = FakeTemporaryVpnApi(session(expiresAt = 200_000L), revokeError = true)
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        val result = runCatching { runBlocking { coordinator.prepare() } }

        assertTrue(result.exceptionOrNull()?.message?.contains("temporary_vpn_revoke_pending") == true)
        assertEquals(0, api.challengeCalls)
        assertEquals(pending, store.pendingRevoke)
    }

    @Test
    fun `expired pending revoke is cleared without a network call`() {
        val store = FakeLeaseStore(
            lease = null,
            pendingRevoke = TemporaryVpnPendingRevoke(
                sessionId = "expired-session",
                controlToken = "expired-control",
                expiresAtEpochMillis = 9_999L,
            ),
        )
        val api = FakeTemporaryVpnApi(session(expiresAt = 200_000L), revokeError = true)
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        assertTrue(runBlocking { coordinator.retryPendingRevoke() })
        assertEquals(null, store.pendingRevoke)
        assertFalse(api.revokeCalled)
    }

    @Test(expected = CancellationException::class)
    fun `pending revoke cancellation propagates without clearing tombstone`() {
        val pending = TemporaryVpnPendingRevoke(
            sessionId = "session",
            controlToken = "control",
            expiresAtEpochMillis = 100_000L,
        )
        val store = FakeLeaseStore(lease = null, pendingRevoke = pending)
        val api = FakeTemporaryVpnApi(
            preparedSession = session(expiresAt = 200_000L),
            revokeCancellation = true,
        )
        val coordinator = coordinator(store, api, nowMillis = 10_000L)

        try {
            runBlocking { coordinator.retryPendingRevoke() }
        } finally {
            assertEquals(pending, store.pendingRevoke)
        }
    }

    private fun coordinator(
        store: FakeLeaseStore,
        api: FakeTemporaryVpnApi,
        nowMillis: Long,
    ) = TemporaryVpnSessionCoordinator(
        store = store,
        api = api,
        publicKeyProvider = { "public-key" },
        deviceKeyProvider = { "stable-device-key" },
        deviceNameProvider = { "Pixel test" },
        platformProvider = { "android" },
        challengeSigner = { "signed:$it" },
        profileSelector = { backend ->
            VlessProfile(
                remark = "Noki ${backend.locationName}",
                endpointCode = backend.endpointCode.orEmpty(),
                host = backend.entryHost,
                port = backend.entryPort.toString(),
                uuid = backend.vpnSecret,
                security = backend.security,
                serverName = backend.serverName,
                publicKey = backend.publicKey.orEmpty(),
                shortId = backend.shortId.orEmpty(),
            )
        },
        nowMillis = { nowMillis },
    )

    private class FakeLeaseStore(
        var lease: TemporaryVpnLease?,
        var pendingRevoke: TemporaryVpnPendingRevoke? = null,
    ) : TemporaryVpnLeaseStore {
        override fun loadTemporaryVpnLease(): TemporaryVpnLease? = lease

        override fun saveTemporaryVpnLease(lease: TemporaryVpnLease) {
            this.lease = lease
        }

        override fun clearTemporaryVpnLease() {
            lease = null
        }

        override fun loadTemporaryVpnPendingRevoke(): TemporaryVpnPendingRevoke? = pendingRevoke

        override fun markTemporaryVpnLeasePendingRevoke(lease: TemporaryVpnLease) {
            pendingRevoke = TemporaryVpnPendingRevoke(
                sessionId = lease.sessionId,
                controlToken = lease.controlToken,
                expiresAtEpochMillis = lease.expiresAtEpochMillis,
            )
            this.lease = null
        }

        override fun clearTemporaryVpnPendingRevoke() {
            pendingRevoke = null
        }
    }

    private class FakeTemporaryVpnApi(
        private val preparedSession: BackendTemporaryVpnSession,
        var revokeError: Boolean = false,
        var revokeCancellation: Boolean = false,
    ) : TemporaryVpnApi {
        var challengeCalls = 0
        var sessionCalls = 0
        var lastSignature = ""
        var lastChallengeDeviceKey = ""
        var lastSessionDeviceKey = ""
        var lastDeviceName = ""
        var lastPlatform = ""
        var revokeCalled = false

        override suspend fun createTemporaryVpnChallenge(
            publicKey: String,
            deviceKey: String,
            deviceName: String,
            platform: String,
        ): BackendTemporaryVpnChallenge {
            challengeCalls += 1
            lastChallengeDeviceKey = deviceKey
            lastDeviceName = deviceName
            lastPlatform = platform
            return BackendTemporaryVpnChallenge("nonce", 60L)
        }

        override suspend fun createTemporaryVpnSession(
            publicKey: String,
            nonce: String,
            signature: String,
            deviceKey: String,
        ): BackendTemporaryVpnSession {
            sessionCalls += 1
            lastSignature = signature
            lastSessionDeviceKey = deviceKey
            return preparedSession
        }

        override suspend fun revokeTemporaryVpnSession(sessionId: String, controlToken: String) {
            revokeCalled = true
            if (revokeCancellation) throw CancellationException("cancelled")
            if (revokeError) error("offline")
        }
    }

    private fun lease(expiresAt: Long) = TemporaryVpnLease(
        sessionId = "session",
        controlToken = "control",
        expiresAtEpochMillis = expiresAt,
        trafficLimitBytes = 100L * 1024L * 1024L,
        locationCode = "lv",
        locationName = "Латвия",
        profile = profile(),
    )

    private fun session(expiresAt: Long) = BackendTemporaryVpnSession(
        mode = "auth_temp",
        sessionId = "session",
        controlToken = "control",
        trafficLimitBytes = 100L * 1024L * 1024L,
        expiresAtEpochMillis = expiresAt,
        vpnSession = BackendVpnSession(
            canConnect = true,
            profileCode = "auto",
            locationCode = "lv",
            locationName = "Латвия",
            endpointCode = "auth-temp-lv",
            entryHost = "vpn.example.test",
            entryPort = 443,
            serverName = "cdn.example.test",
            proxyType = "vless",
            transport = "tcp",
            transportMode = null,
            security = "reality",
            fingerprint = "chrome",
            requestHost = null,
            path = null,
            alpn = null,
            allowInsecure = false,
            enableMux = false,
            randomUserAgent = false,
            publicKey = "server-public-key",
            shortId = "0123456789abcdef",
            vpnUsername = "temporary-user",
            vpnSecret = "b3e85e7a-c160-4dd0-9b26-86f32b9aa5d6",
            flow = null,
            planCode = null,
            endpointCandidates = emptyList(),
        ),
    )

    private fun profile() = VlessProfile(
        remark = "Noki Латвия",
        endpointCode = "auth-temp-lv",
        host = "vpn.example.test",
        port = "443",
        uuid = "b3e85e7a-c160-4dd0-9b26-86f32b9aa5d6",
        security = "reality",
        serverName = "cdn.example.test",
        publicKey = "server-public-key",
        shortId = "0123456789abcdef",
    )

}
