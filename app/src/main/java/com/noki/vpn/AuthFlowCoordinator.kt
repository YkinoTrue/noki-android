package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.AuthFlowApi
import com.noki.vpn.data.BackendAuthTokens
import com.noki.vpn.data.BackendDevice
import com.noki.vpn.data.BackendException

class AuthFlowCoordinator private constructor(
    private val authApi: AuthFlowApi,
    private val completeAuthenticatedSession: suspend (
        BackendAuthTokens,
        AppUiState,
    ) -> AuthenticatedSessionCoordinator.Result,
) {
    data class Result(
        val tokens: BackendAuthTokens,
        val state: AppUiState,
    )

    data class LoginPresentation(
        val screenStack: List<AppDestination>,
        val inlineMessage: String?,
    )

    internal constructor(
        authApi: AuthFlowApi,
        authenticatedSessionCoordinator: AuthenticatedSessionCoordinator,
    ) : this(
        authApi = authApi,
        completeAuthenticatedSession = authenticatedSessionCoordinator::complete,
    )

    constructor(
        authApi: AuthFlowApi,
        registerCurrentDevice: suspend (String) -> BackendDevice,
        bindTokensToRegisteredDevice: suspend (BackendAuthTokens, BackendDevice) -> BackendAuthTokens =
            { tokens, _ -> tokens },
        syncBootstrap: suspend (String, AppUiState) -> AppUiState,
    ) : this(
        authApi = authApi,
        completeAuthenticatedSession = { tokens, preparedState ->
            val device = registerCurrentDevice(tokens.accessToken)
            val boundTokens = bindTokensToRegisteredDevice(tokens, device)
            AuthenticatedSessionCoordinator.Result(
                tokens = boundTokens,
                device = device,
                state = syncBootstrap(boundTokens.accessToken, preparedState),
            )
        },
    )

    suspend fun login(
        form: LoginFormState,
        baseState: AppUiState,
        language: AppLanguage,
        deviceId: String? = null,
        presentation: LoginPresentation? = null,
    ): Result {
        val tokens = loginWithOptionalDevice(form.email.trim(), form.password, deviceId)
        val preparedState = baseState.copy(
            loginForm = baseState.loginForm.copy(
                isLoading = false,
                password = "",
                error = null,
            ),
            isAuthenticated = true,
            screenStack = presentation?.screenStack ?: listOf(AppDestination.HOME),
            inlineMessage = if (presentation != null) {
                presentation.inlineMessage
            } else {
                tr(language, "Вы вошли в Noki Vpn", "You are signed in to Noki Vpn")
            },
        )
        val completed = completeAuthenticatedSession(tokens, preparedState)
        return Result(
            tokens = completed.tokens,
            state = completed.state,
        )
    }

    suspend fun register(
        form: RegistrationFormState,
        baseState: AppUiState,
        language: AppLanguage,
        deviceId: String? = null,
    ): Result {
        authApi.register(
            username = form.username.trim(),
            email = form.email.trim(),
            password = form.password,
            verificationCode = form.verificationCode.trim(),
        )
        val tokens = loginWithOptionalDevice(form.email.trim(), form.password, deviceId)
        val preparedState = baseState.copy(
            registrationForm = baseState.registrationForm.copy(
                isLoading = false,
                verificationCode = "",
                codeSent = false,
                isCodeSending = false,
                password = "",
                passwordRepeat = "",
                error = null,
            ),
            isAuthenticated = true,
            screenStack = listOf(AppDestination.HOME),
            inlineMessage = tr(language, "Аккаунт создан", "Your account is ready"),
        )
        val completed = completeAuthenticatedSession(tokens, preparedState)
        return Result(
            tokens = completed.tokens,
            state = completed.state,
        )
    }

    private suspend fun loginWithOptionalDevice(
        email: String,
        password: String,
        deviceId: String?,
    ): BackendAuthTokens {
        val cleanDeviceId = deviceId?.takeIf { it.isNotBlank() }
        if (cleanDeviceId == null) return authApi.login(email, password, null)
        return try {
            authApi.login(email, password, cleanDeviceId)
        } catch (error: BackendException) {
            if (error.statusCode != 403) throw error
            authApi.login(email, password, null)
        }
    }

    private fun tr(language: AppLanguage, russian: String, english: String): String =
        if (language == AppLanguage.RU) russian else english

}
