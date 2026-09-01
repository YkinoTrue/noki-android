package com.noki.vpn

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSecurityUiWorkflowTest {
    @Test
    fun `account username update uses registration input policy`() = runBlocking {
        var state = AppUiState(
            accountSecurityState = AccountSecurityStateReducer.username(
                AccountSecurityUiState(),
                "",
            ),
        )
        val workflow = AccountSecurityUiWorkflow(
            scope = this,
            currentState = { state },
            publishState = { state = it },
            isInvitedDevice = { false },
            currentAuthAttempt = { AuthSessionAttempt("access", 0) },
            sendEmailCode = { _, _ -> error("not used") },
            changeEmail = { _, _, _ -> error("not used") },
            changePassword = { _, _ -> error("not used") },
            changeUsername = { _, _ -> error("not used") },
            applyUser = { _, _ -> error("not used") },
        )

        workflow.updateUsername("new_ник")

        val action = state.accountSecurityState.action as AccountSecurityActionState.Username
        assertEquals("new_", action.username)
    }

    @Test
    fun `invalidated email request cannot overwrite replacement action`() = runBlocking {
        var state = AppUiState(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "old@example.com",
            ),
        )
        val response = CompletableDeferred<Int>()
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val workflow = AccountSecurityUiWorkflow(
            scope = this,
            currentState = { state },
            publishState = { state = it },
            isInvitedDevice = { false },
            currentAuthAttempt = { AuthSessionAttempt("access", 0) },
            sendEmailCode = { _, _ ->
                started.complete(Unit)
                try {
                    response.await()
                } finally {
                    cancelled.complete(Unit)
                }
            },
            changeEmail = { _, _, _ -> error("not used") },
            changePassword = { _, _ -> error("not used") },
            changeUsername = { _, _ -> error("not used") },
            applyUser = { _, _ -> error("not used") },
        )

        workflow.requestEmailCode()
        started.await()
        workflow.invalidate()
        cancelled.await()
        state = state.copy(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "new@example.com",
            ),
        )
        response.complete(30)
        yield()

        val action = state.accountSecurityState.action as AccountSecurityActionState.Email
        assertEquals("new@example.com", action.email)
        assertEquals(false, action.codeSent)
    }

    @Test
    fun `rapid repeated email request starts one operation`() = runBlocking {
        var state = AppUiState(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "user@example.com",
            ),
        )
        val response = CompletableDeferred<Int>()
        val started = CompletableDeferred<Unit>()
        var requestCount = 0
        val workflow = AccountSecurityUiWorkflow(
            scope = this,
            currentState = { state },
            publishState = { state = it },
            isInvitedDevice = { false },
            currentAuthAttempt = { AuthSessionAttempt("access", 0) },
            sendEmailCode = { _, _ ->
                requestCount += 1
                started.complete(Unit)
                response.await()
            },
            changeEmail = { _, _, _ -> error("not used") },
            changePassword = { _, _ -> error("not used") },
            changeUsername = { _, _ -> error("not used") },
            applyUser = { _, _ -> error("not used") },
        )

        workflow.requestEmailCode()
        workflow.requestEmailCode()
        started.await()

        assertEquals(1, requestCount)
        workflow.invalidate()
    }

    @Test
    fun `cancellation does not surface as an account error or apply a user`() = runBlocking {
        var state = AppUiState(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "user@example.com",
            ),
        )
        var appliedUsers = 0
        val workflow = AccountSecurityUiWorkflow(
            scope = this,
            currentState = { state },
            publishState = { state = it },
            isInvitedDevice = { false },
            currentAuthAttempt = { AuthSessionAttempt("access", 0) },
            sendEmailCode = { _, _ -> throw CancellationException("cancelled") },
            changeEmail = { _, _, _ -> error("not used") },
            changePassword = { _, _ -> error("not used") },
            changeUsername = { _, _ -> error("not used") },
            applyUser = { _, _ -> appliedUsers += 1 },
        )

        workflow.requestEmailCode()
        yield()

        val action = state.accountSecurityState.action as AccountSecurityActionState.Email
        assertEquals(false, action.isLoading)
        assertEquals(null, action.error)
        assertEquals(0, appliedUsers)
    }

    @Test
    fun `cancelled operation cleanup cannot release its replacement slot`() = runBlocking {
        var state = AppUiState(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "first@example.com",
            ),
        )
        val firstStarted = CompletableDeferred<Unit>()
        val firstRelease = CompletableDeferred<Unit>()
        val firstReturned = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val secondRelease = CompletableDeferred<Int>()
        var requestCount = 0
        val workflow = AccountSecurityUiWorkflow(
            scope = this,
            currentState = { state },
            publishState = { state = it },
            isInvitedDevice = { false },
            currentAuthAttempt = { AuthSessionAttempt("access", 0) },
            sendEmailCode = { _, _ ->
                requestCount += 1
                when (requestCount) {
                    1 -> {
                        firstStarted.complete(Unit)
                        withContext(NonCancellable) { firstRelease.await() }
                        firstReturned.complete(Unit)
                        30
                    }
                    2 -> {
                        secondStarted.complete(Unit)
                        secondRelease.await()
                    }
                    else -> error("unexpected third request")
                }
            },
            changeEmail = { _, _, _ -> error("not used") },
            changePassword = { _, _ -> error("not used") },
            changeUsername = { _, _ -> error("not used") },
            applyUser = { _, _ -> error("not used") },
        )

        workflow.requestEmailCode()
        firstStarted.await()
        workflow.invalidate()
        state = state.copy(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "second@example.com",
            ),
        )
        workflow.requestEmailCode()
        secondStarted.await()
        firstRelease.complete(Unit)
        firstReturned.await()
        repeat(3) { yield() }

        state = state.copy(
            accountSecurityState = AccountSecurityStateReducer.email(
                AccountSecurityUiState(),
                "third@example.com",
            ),
        )
        workflow.requestEmailCode()

        assertEquals(2, requestCount)
        workflow.invalidate()
    }

    @Test
    fun `loading account action keeps its submitted fields immutable`() = runBlocking {
        var state = AppUiState(
            accountSecurityState = AccountSecurityUiState(
                action = AccountSecurityActionState.Email(
                    email = "submitted@example.com",
                    verificationCode = "123456",
                    isLoading = true,
                ),
            ),
        )
        val workflow = AccountSecurityUiWorkflow(
            scope = this,
            currentState = { state },
            publishState = { state = it },
            isInvitedDevice = { false },
            currentAuthAttempt = { AuthSessionAttempt("access", 0) },
            sendEmailCode = { _, _ -> error("not used") },
            changeEmail = { _, _, _ -> error("not used") },
            changePassword = { _, _ -> error("not used") },
            changeUsername = { _, _ -> error("not used") },
            applyUser = { _, _ -> error("not used") },
        )

        workflow.updateEmail("changed@example.com")
        workflow.updateEmailCode("999999")

        val action = state.accountSecurityState.action as AccountSecurityActionState.Email
        assertEquals("submitted@example.com", action.email)
        assertEquals("123456", action.verificationCode)
    }
}
