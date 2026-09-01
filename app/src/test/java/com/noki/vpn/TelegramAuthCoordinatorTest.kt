package com.noki.vpn

import com.noki.vpn.data.AuthRefreshApi
import com.noki.vpn.data.AuthTokenRefresher
import com.noki.vpn.data.BackendAuthTokens
import com.noki.vpn.data.BackendDevice
import com.noki.vpn.data.DefaultStoredSettingsFactory
import com.noki.vpn.data.InMemoryAtomicStoredSettingsStore
import com.noki.vpn.data.TelegramAuthApi
import com.noki.vpn.data.TelegramOidcApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthCoordinatorTest {
    @Test
    fun `telegram token is exchanged once and only Noki tokens are persisted`() = runBlocking {
        val idToken = "telegram-id-token-that-must-not-be-persisted"
        val api = FakeTelegramAuthApi(tokens = tokens())
        val store = InMemoryAtomicStoredSettingsStore(DefaultStoredSettingsFactory.create())
        val session = authenticatedSession(store)
        val coordinator = TelegramAuthCoordinator(api, FakeTelegramOidcApi(), session)

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
    fun `failed Telegram exchange keeps current temporary connection available`() = runBlocking {
        val api = FakeTelegramAuthApi(error = IllegalStateException("backend unavailable"))
        val store = InMemoryAtomicStoredSettingsStore(DefaultStoredSettingsFactory.create())
        var temporaryVpnRevoked = false
        val session = authenticatedSession(
            store = store,
            stageTemporaryVpnForRevoke = { temporaryVpnRevoked = true },
        )
        val coordinator = TelegramAuthCoordinator(api, FakeTelegramOidcApi(), session)

        val result = runCatching {
            coordinator.login(
                idToken = "telegram-id-token-that-must-not-be-persisted",
                deviceId = null,
                preparedState = AppUiState(),
            )
        }

        assertTrue(result.isFailure)
        assertFalse(store.load().isAuthenticated)
        assertFalse(temporaryVpnRevoked)
        assertNull(store.load().backendAccessToken)
    }

    @Test
    fun `browser callback exchange is owned by Telegram coordinator`() = runBlocking {
        val oidc = FakeTelegramOidcApi(idToken = "telegram-id-token")
        val store = InMemoryAtomicStoredSettingsStore(DefaultStoredSettingsFactory.create())
        val coordinator = TelegramAuthCoordinator(
            FakeTelegramAuthApi(tokens = tokens()),
            oidc,
            authenticatedSession(store),
        )

        val result = coordinator.resolveCallback(
            TelegramLoginCallbackResult.BrowserState(
                state = "browser-state",
                codeVerifier = "pkce-verifier",
            ),
        )

        assertEquals("telegram-id-token", (result as TelegramLoginResult.Success).idToken)
        assertEquals("browser-state", oidc.receivedState)
        assertEquals("pkce-verifier", oidc.receivedVerifier)
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

    private class FakeTelegramAuthApi(
        private val tokens: BackendAuthTokens? = null,
        private val error: Throwable? = null,
    ) : TelegramAuthApi {
        var receivedIdToken: String? = null
        var receivedDeviceId: String? = null

        override suspend fun telegramLogin(idToken: String, deviceId: String?): BackendAuthTokens {
            receivedIdToken = idToken
            receivedDeviceId = deviceId
            error?.let { throw it }
            return requireNotNull(tokens)
        }
    }

    private class FakeTelegramOidcApi(
        private val idToken: String = "unused",
    ) : TelegramOidcApi {
        var receivedState: String? = null
        var receivedVerifier: String? = null

        override suspend fun startTelegramOidc(codeChallenge: String, clientState: String): String =
            "tg://resolve"

        override suspend fun startTelegramBrowserOidc(codeChallenge: String, clientState: String): String =
            "https://oauth.telegram.org/auth"

        override suspend fun exchangeTelegramOidcCode(code: String, codeVerifier: String): String = idToken

        override suspend fun exchangeTelegramBrowserOidc(state: String, codeVerifier: String): String {
            receivedState = state
            receivedVerifier = codeVerifier
            return idToken
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
        refreshExpiresAt = "2026-07-22T00:00:00Z",
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
