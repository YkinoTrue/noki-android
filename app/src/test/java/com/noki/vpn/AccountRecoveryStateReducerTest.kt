package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRecoveryStateReducerTest {
    @Test
    fun codeRequestShowsValidationErrorWhenEmailIsInvalid() {
        val state = AppUiState(
            passwordRecoveryForm = PasswordRecoveryFormState(
                email = "bad",
                showCodeField = true,
            ),
        )

        val transition = AccountRecoveryStateReducer.requestCode(
            current = state,
            language = AppLanguage.EN,
            isValidEmail = { false },
        )

        val errorState = transition as AccountRecoveryStateReducer.CodeRequestTransition.UpdateState
        assertEquals("Enter a valid email", errorState.state.passwordRecoveryForm.error)
    }

    @Test
    fun codeRequestStartsSendingWhenEmailIsValidAndCooldownIsClear() {
        val state = AppUiState(
            passwordRecoveryForm = PasswordRecoveryFormState(
                email = " user@example.com ",
                showCodeField = true,
            ),
        )

        val transition = AccountRecoveryStateReducer.requestCode(
            current = state,
            language = AppLanguage.EN,
            isValidEmail = { true },
        )

        val send = transition as AccountRecoveryStateReducer.CodeRequestTransition.Send
        assertEquals(" user@example.com ", send.form.email)
        assertTrue(send.state.passwordRecoveryForm.isCodeSending)
        assertEquals(null, send.state.inlineMessage)
    }

    @Test
    fun submitFirstStepShowsCodeFieldForValidEmail() {
        val state = AppUiState(
            passwordRecoveryForm = PasswordRecoveryFormState(email = "user@example.com"),
        )

        val transition = AccountRecoveryStateReducer.submit(
            current = state,
            language = AppLanguage.EN,
            isValidEmail = { true },
        )

        val update = transition as AccountRecoveryStateReducer.SubmitTransition.UpdateState
        assertTrue(update.state.passwordRecoveryForm.showCodeField)
        assertEquals(null, update.state.passwordRecoveryForm.error)
    }

    @Test
    fun submitCodeStepStartsVerificationForValidCode() {
        val form = PasswordRecoveryFormState(
            email = "user@example.com",
            showCodeField = true,
            verificationCode = "1234",
        )
        val state = AppUiState(passwordRecoveryForm = form)

        val transition = AccountRecoveryStateReducer.submit(
            current = state,
            language = AppLanguage.EN,
            isValidEmail = { true },
        )

        val verify = transition as AccountRecoveryStateReducer.SubmitTransition.VerifyCode
        assertEquals(form, verify.form)
        assertTrue(verify.state.passwordRecoveryForm.isSubmitting)
    }

    @Test
    fun activeRecoveryOperationRejectsResendAndDuplicateSubmit() {
        val state = AppUiState(
            passwordRecoveryForm = PasswordRecoveryFormState(
                email = "user@example.com",
                showCodeField = true,
                verificationCode = "1234",
                isSubmitting = true,
            ),
        )

        val resend = AccountRecoveryStateReducer.requestCode(
            current = state,
            language = AppLanguage.EN,
            isValidEmail = { true },
        )
        val submit = AccountRecoveryStateReducer.submit(
            current = state,
            language = AppLanguage.EN,
            isValidEmail = { true },
        )

        assertTrue(resend is AccountRecoveryStateReducer.CodeRequestTransition.Noop)
        val unchanged = submit as AccountRecoveryStateReducer.SubmitTransition.UpdateState
        assertEquals(state, unchanged.state)
    }

    @Test
    fun successfulCodeVerificationShowsPasswordStepAndClearsCooldown() {
        val state = AppUiState(
            passwordRecoveryForm = PasswordRecoveryFormState(
                showCodeField = true,
                isSubmitting = true,
                isCodeSending = true,
                codeCooldownSeconds = 30,
            ),
        )

        val updated = AccountRecoveryStateReducer.codeVerified(state)

        assertTrue(updated.passwordRecoveryForm.passwordStepVisible)
        assertEquals(0, updated.passwordRecoveryForm.codeCooldownSeconds)
        assertEquals(false, updated.passwordRecoveryForm.isCodeSending)
        assertEquals(false, updated.passwordRecoveryForm.isSubmitting)
    }

    @Test
    fun successfulPasswordResetReturnsToLoginWithoutPrefillingEmail() {
        val state = AppUiState(
            screenStack = listOf(AppDestination.PASSWORD_RECOVERY),
            passwordRecoveryForm = PasswordRecoveryFormState(email = "user@example.com", isSubmitting = true),
        )

        val updated = AccountRecoveryStateReducer.passwordReset(state, AppLanguage.EN)

        assertEquals(listOf(AppDestination.LOGIN), updated.screenStack)
        assertEquals("", updated.loginForm.email)
        assertEquals("", updated.loginForm.password)
        assertEquals("Password updated", updated.inlineMessage)
        assertEquals(false, updated.passwordRecoveryForm.isSubmitting)
    }
}
