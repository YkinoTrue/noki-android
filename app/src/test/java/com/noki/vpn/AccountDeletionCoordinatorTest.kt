package com.noki.vpn

import com.noki.vpn.data.AccountApi
import com.noki.vpn.data.AuthRefreshRejectedException
import com.noki.vpn.data.BackendException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionCoordinatorTest {
    @Test
    fun successUsesAuthenticatedRunnerToken() = runBlocking {
        val api = FakeAccountApi()
        val coordinator = AccountDeletionCoordinator(tokenRunner("rotated-token"), api)

        val result = coordinator.deleteAccount()

        assertTrue(result is AccountDeletionCoordinator.Result.Success)
        assertEquals(listOf("rotated-token"), api.tokens)
    }

    @Test
    fun resourceUnauthorizedPreservesSession() = runBlocking {
        val coordinator = AccountDeletionCoordinator(
            tokenRunner(),
            FakeAccountApi(error = BackendException("unauthorized", 401)),
        )

        val result = coordinator.deleteAccount()

        assertTrue(result is AccountDeletionCoordinator.Result.Failure)
    }

    @Test
    fun confirmedRefreshRejectionRequiresLogout() = runBlocking {
        val rejection = AuthRefreshRejectedException(BackendException("invalid_refresh_token", 401))
        val coordinator = AccountDeletionCoordinator(failingRunner(rejection), FakeAccountApi())

        val result = coordinator.deleteAccount()

        assertTrue(result is AccountDeletionCoordinator.Result.LogoutRequired)
    }

    @Test
    fun transientFailurePreservesSessionForRetry() = runBlocking {
        val error = IOException("offline")
        val coordinator = AccountDeletionCoordinator(tokenRunner(), FakeAccountApi(error = error))

        val result = coordinator.deleteAccount()

        assertTrue(result is AccountDeletionCoordinator.Result.Failure)
        assertEquals(error, (result as AccountDeletionCoordinator.Result.Failure).error)
    }

    @Test
    fun cancellationPropagates() = runBlocking {
        val coordinator = AccountDeletionCoordinator(
            tokenRunner(),
            FakeAccountApi(error = CancellationException("cancelled")),
        )

        val result = runCatching { coordinator.deleteAccount() }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    private fun tokenRunner(token: String = "access-token") = object : AuthenticatedCallRunner {
        override suspend fun <T> run(block: suspend (String) -> T): T = block(token)
    }

    private fun failingRunner(error: Throwable) = object : AuthenticatedCallRunner {
        override suspend fun <T> run(block: suspend (String) -> T): T = throw error
    }

    private class FakeAccountApi(
        private val error: Throwable? = null,
    ) : AccountApi {
        val tokens = mutableListOf<String>()

        override suspend fun deleteAccount(token: String) {
            tokens += token
            error?.let { throw it }
        }
    }
}
