package com.noki.vpn

import com.noki.vpn.data.GoogleAuthApi

internal sealed interface GoogleLoginResult {
    data class Success(val idToken: String) : GoogleLoginResult
    data object Cancelled : GoogleLoginResult
    data class Failure(val code: String) : GoogleLoginResult
}

internal class GoogleAuthCoordinator(
    private val authApi: GoogleAuthApi,
    private val authenticatedSessionCoordinator: AuthenticatedSessionCoordinator,
) {
    suspend fun login(
        idToken: String,
        deviceId: String?,
        preparedState: AppUiState,
    ): AuthenticatedSessionCoordinator.Result {
        require(idToken.isNotBlank()) { "google_id_token_missing" }
        val tokens = authApi.googleLogin(
            idToken = idToken,
            deviceId = deviceId?.takeIf { it.isNotBlank() },
        )
        return authenticatedSessionCoordinator.complete(tokens, preparedState)
    }
}
