package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.AuthFlowApi
import com.noki.vpn.data.BackendAuthTokens
import com.noki.vpn.data.BackendDevice
import com.noki.vpn.data.BackendException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthFlowCoordinatorTest {
    @Test
    fun validateLoginRequiresIdentifierAndPassword() {
        assertEquals(
            "Enter your email or username",
            AuthStepReducer.validateLogin(LoginFormState(email = "", password = "pass"), AppLanguage.EN),
        )
        assertEquals(
            "Введите пароль",
            AuthStepReducer.validateLogin(LoginFormState(email = "ykino", password = ""), AppLanguage.RU),
        )
    }

    @Test
    fun loginBuildsAuthenticatedStateFromBackendTokenDeviceAndBootstrap() {
        val api = FakeAuthApi(
            loginTokens = BackendAuthTokens(
                accessToken = "token-1",
                refreshToken = "refresh-1",
                expiresInSeconds = 3600,
                refreshExpiresAt = "2026-07-16T00:00:00Z",
            ),
        )
        val coordinator = AuthFlowCoordinator(
            authApi = api,
            registerCurrentDevice = { token ->
                assertEquals("token-1", token)
                backendDevice()
            },
            bindTokensToRegisteredDevice = { tokens, device ->
                assertEquals("refresh-1", tokens.refreshToken)
                assertEquals("device-1", device.id)
                tokens.copy(accessToken = "token-1-bound", refreshToken = "refresh-1-bound")
            },
            syncBootstrap = { token, baseState ->
                assertEquals("token-1-bound", token)
                baseState.copy(userProfile = baseState.userProfile.copy(username = "ykino"))
            },
        )

        val result = runBlocking {
            coordinator.login(
                form = LoginFormState(email = " ykino ", password = "00000000"),
                baseState = AppUiState(),
                language = AppLanguage.EN,
                deviceId = "device-known",
            )
        }

        assertEquals("token-1-bound", result.tokens.accessToken)
        assertEquals("refresh-1-bound", result.tokens.refreshToken)
        assertEquals(listOf(AppDestination.HOME), result.state.screenStack)
        assertEquals("", result.state.loginForm.password)
        assertEquals("ykino", result.state.userProfile.username)
        assertEquals("You are signed in to Noki Vpn", result.state.inlineMessage)
        assertEquals("ykino", api.loginEmail)
        assertEquals("device-known", api.loginDeviceId)
    }

    @Test
    fun registrationCreatesAccountThenBuildsAuthenticatedState() {
        val api = FakeAuthApi(
            loginTokens = BackendAuthTokens(
                accessToken = "token-2",
                refreshToken = "refresh-2",
                expiresInSeconds = 3600,
                refreshExpiresAt = "2026-07-16T00:00:00Z",
            ),
        )
        val coordinator = AuthFlowCoordinator(
            authApi = api,
            registerCurrentDevice = { backendDevice() },
            bindTokensToRegisteredDevice = { tokens, device ->
                assertEquals("refresh-2", tokens.refreshToken)
                assertEquals("device-1", device.id)
                tokens.copy(accessToken = "token-2-bound", refreshToken = "refresh-2-bound")
            },
            syncBootstrap = { _, baseState -> baseState },
        )

        val result = runBlocking {
            coordinator.register(
                form = RegistrationFormState(
                    username = " ykino ",
                    email = " ykino@example.com ",
                    verificationCode = " 1234 ",
                    password = "00000000",
                    passwordRepeat = "00000000",
                ),
                baseState = AppUiState(),
                language = AppLanguage.RU,
                deviceId = "device-known",
            )
        }

        assertEquals("token-2-bound", result.tokens.accessToken)
        assertEquals("refresh-2-bound", result.tokens.refreshToken)
        assertEquals("ykino", api.registerUsername)
        assertEquals("ykino@example.com", api.registerEmail)
        assertEquals("1234", api.registerCode)
        assertEquals("device-known", api.loginDeviceId)
        assertEquals("", result.state.registrationForm.password)
        assertEquals("Аккаунт создан", result.state.inlineMessage)
    }

    @Test
    fun `restricted device id retries login once without device id`() {
        val api = FakeAuthApi(
            loginTokens = BackendAuthTokens(
                accessToken = "token",
                refreshToken = "refresh",
                expiresInSeconds = 3600,
            ),
            rejectDeviceStatus = 403,
        )
        val coordinator = AuthFlowCoordinator(
            authApi = api,
            registerCurrentDevice = { backendDevice() },
            syncBootstrap = { _, state -> state },
        )

        runBlocking {
            coordinator.login(
                form = LoginFormState(email = "user@example.com", password = "password"),
                baseState = AppUiState(),
                language = AppLanguage.EN,
                deviceId = "restricted-device",
            )
        }

        assertEquals(listOf("restricted-device", null), api.loginDeviceIds)
    }

    @Test
    fun `authenticated recovery login can return to security without a banner`() {
        val api = FakeAuthApi(
            loginTokens = BackendAuthTokens(
                accessToken = "token",
                refreshToken = "refresh",
                expiresInSeconds = 3600,
            ),
        )
        val coordinator = AuthFlowCoordinator(
            authApi = api,
            registerCurrentDevice = { backendDevice() },
            syncBootstrap = { _, state -> state },
        )
        val returnStack = listOf(
            AppDestination.HOME,
            AppDestination.SETTINGS,
            AppDestination.SECURITY,
        )

        val result = runBlocking {
            coordinator.login(
                form = LoginFormState(email = "user@example.com", password = "new-password"),
                baseState = AppUiState(),
                language = AppLanguage.RU,
                presentation = AuthFlowCoordinator.LoginPresentation(
                    screenStack = returnStack,
                    inlineMessage = null,
                ),
            )
        }

        assertEquals(returnStack, result.state.screenStack)
        assertNull(result.state.inlineMessage)
    }

    private class FakeAuthApi(
        private val loginTokens: BackendAuthTokens,
        private val rejectDeviceStatus: Int? = null,
    ) : AuthFlowApi {
        var loginEmail: String? = null
        var registerUsername: String? = null
        var registerEmail: String? = null
        var registerCode: String? = null
        var loginDeviceId: String? = null
        val loginDeviceIds = mutableListOf<String?>()

        override suspend fun login(email: String, password: String, deviceId: String?): BackendAuthTokens {
            loginEmail = email
            loginDeviceId = deviceId
            loginDeviceIds += deviceId
            if (deviceId != null && rejectDeviceStatus != null && loginDeviceIds.size == 1) {
                throw BackendException("device rejected", rejectDeviceStatus)
            }
            return loginTokens
        }

        override suspend fun register(
            username: String,
            email: String,
            password: String,
            verificationCode: String,
        ) {
            registerUsername = username
            registerEmail = email
            registerCode = verificationCode
        }
    }

    private fun backendDevice(): BackendDevice =
        BackendDevice(
            id = "device-1",
            deviceKey = "device-key",
            deviceName = "Phone",
            platform = "android",
            accessRole = "owner",
            isActive = true,
            lastSeenAt = null,
        )
}
