package com.noki.vpn

import com.noki.vpn.data.AppLanguage

object AccountRecoveryStateReducer {
    sealed interface CodeRequestTransition {
        data object Noop : CodeRequestTransition
        data class UpdateState(val state: AppUiState) : CodeRequestTransition
        data class Send(
            val state: AppUiState,
            val form: PasswordRecoveryFormState,
        ) : CodeRequestTransition
    }

    sealed interface SubmitTransition {
        data class UpdateState(val state: AppUiState) : SubmitTransition
        data class VerifyCode(
            val state: AppUiState,
            val form: PasswordRecoveryFormState,
        ) : SubmitTransition

        data class ResetPassword(
            val state: AppUiState,
            val form: PasswordRecoveryFormState,
        ) : SubmitTransition
    }

    fun requestCode(
        current: AppUiState,
        language: AppLanguage,
        isValidEmail: (String) -> Boolean,
    ): CodeRequestTransition {
        val form = current.passwordRecoveryForm
        if (!form.showCodeField) return CodeRequestTransition.Noop
        val validationError = AccountRecoveryCoordinator.validateEmail(
            email = form.email,
            language = language,
            isValidEmail = isValidEmail,
        )
        if (validationError != null) {
            return CodeRequestTransition.UpdateState(
                current.copy(passwordRecoveryForm = form.copy(error = validationError)),
            )
        }
        if (form.codeCooldownSeconds > 0 || form.isCodeSending || form.isSubmitting) {
            return CodeRequestTransition.Noop
        }
        return CodeRequestTransition.Send(
            state = current.copy(
                passwordRecoveryForm = form.copy(isCodeSending = true, error = null),
                inlineMessage = null,
            ),
            form = form,
        )
    }

    fun codeRequestSent(
        current: AppUiState,
        cooldownSeconds: Int,
        language: AppLanguage,
    ): AppUiState {
        return current.copy(
            passwordRecoveryForm = current.passwordRecoveryForm.copy(
                isCodeSending = false,
                codeSent = true,
                codeCooldownSeconds = cooldownSeconds.coerceAtLeast(0),
                error = null,
            ),
            inlineMessage = tr(language, "Код отправлен на почту", "The code was sent to your email"),
        )
    }

    fun codeRequestFailed(
        current: AppUiState,
        error: String,
    ): AppUiState {
        return current.copy(
            passwordRecoveryForm = current.passwordRecoveryForm.copy(
                isCodeSending = false,
                error = error,
            ),
        )
    }

    fun submit(
        current: AppUiState,
        language: AppLanguage,
        isValidEmail: (String) -> Boolean,
    ): SubmitTransition {
        val form = current.passwordRecoveryForm
        if (form.isSubmitting || form.isCodeSending) {
            return SubmitTransition.UpdateState(current)
        }
        if (!form.showCodeField) {
            val error = AccountRecoveryCoordinator.validateEmail(
                email = form.email,
                language = language,
                isValidEmail = isValidEmail,
            )
            return SubmitTransition.UpdateState(
                if (error != null) {
                    current.copy(passwordRecoveryForm = form.copy(error = error))
                } else {
                    current.copy(passwordRecoveryForm = form.copy(showCodeField = true, error = null))
                },
            )
        }

        if (!form.passwordStepVisible) {
            val error = AccountRecoveryCoordinator.validateCode(form, language)
            if (error != null) {
                return SubmitTransition.UpdateState(
                    current.copy(passwordRecoveryForm = form.copy(error = error)),
                )
            }
            return SubmitTransition.VerifyCode(
                state = current.copy(passwordRecoveryForm = form.copy(isSubmitting = true, error = null)),
                form = form,
            )
        }

        val error = AccountRecoveryCoordinator.validatePassword(form, language)
        if (error != null) {
            return SubmitTransition.UpdateState(
                current.copy(passwordRecoveryForm = form.copy(error = error)),
            )
        }
        return SubmitTransition.ResetPassword(
            state = current.copy(passwordRecoveryForm = form.copy(isSubmitting = true, error = null)),
            form = form,
        )
    }

    fun codeVerified(current: AppUiState): AppUiState {
        return current.copy(
            passwordRecoveryForm = current.passwordRecoveryForm.copy(
                passwordStepVisible = true,
                codeCooldownSeconds = 0,
                isCodeSending = false,
                isSubmitting = false,
                error = null,
            ),
        )
    }

    fun codeVerificationFailed(
        current: AppUiState,
        error: String,
    ): AppUiState {
        return current.copy(
            passwordRecoveryForm = current.passwordRecoveryForm.copy(
                isSubmitting = false,
                error = error,
            ),
        )
    }

    fun passwordReset(
        current: AppUiState,
        language: AppLanguage,
    ): AppUiState {
        return current.copy(
            passwordRecoveryForm = current.passwordRecoveryForm.copy(isSubmitting = false),
            screenStack = listOf(AppDestination.LOGIN),
            loginForm = LoginFormState(),
            inlineMessage = tr(language, "Пароль обновлен", "Password updated"),
        )
    }

    fun passwordResetFailed(
        current: AppUiState,
        error: String,
    ): AppUiState {
        return current.copy(
            passwordRecoveryForm = current.passwordRecoveryForm.copy(
                isSubmitting = false,
                error = error,
            ),
        )
    }

    private fun tr(
        language: AppLanguage,
        russian: String,
        english: String,
    ): String {
        return if (language == AppLanguage.RU) russian else english
    }
}
