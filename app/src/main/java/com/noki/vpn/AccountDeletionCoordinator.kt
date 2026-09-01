package com.noki.vpn

import com.noki.vpn.data.AccountApi
import com.noki.vpn.data.AuthRefreshRejectedException
import com.noki.vpn.data.BackendException
import kotlinx.coroutines.CancellationException

internal class AccountDeletionCoordinator(
    private val authRunner: AuthenticatedCallRunner,
    private val api: AccountApi,
) {
    sealed interface Result {
        data object Success : Result
        data object LogoutRequired : Result
        data class Failure(val error: Throwable) : Result
    }

    suspend fun deleteAccount(): Result {
        return try {
            authRunner.run { token -> api.deleteAccount(token) }
            Result.Success
        } catch (error: CancellationException) {
            throw error
        } catch (_: AuthRefreshRejectedException) {
            Result.LogoutRequired
        } catch (error: BackendException) {
            if (error.statusCode == 404) {
                Result.LogoutRequired
            } else {
                Result.Failure(error)
            }
        } catch (error: Throwable) {
            Result.Failure(error)
        }
    }
}
