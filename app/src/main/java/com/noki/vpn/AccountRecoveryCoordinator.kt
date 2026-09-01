package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.PasswordRecoveryApi

internal class AccountRecoveryCoordinator(
    private val api: PasswordRecoveryApi,
) {
    private var generation = 0L
    private var activeOwner: WorkflowOwner? = null

    @Synchronized
    fun begin(requestKey: String): WorkflowOwner {
        generation += 1L
        return WorkflowOwner(generation, requestKey).also { activeOwner = it }
    }

    @Synchronized
    fun accepts(owner: WorkflowOwner, currentRequestKey: String): Boolean =
        activeOwner == owner && owner.requestKey == currentRequestKey

    @Synchronized
    fun invalidate() {
        generation += 1L
        activeOwner = null
    }

    suspend fun sendCode(email: String): Int {
        return api.sendPasswordRecoveryCode(email.trim())
    }

    suspend fun verifyCode(form: PasswordRecoveryFormState) {
        api.verifyPasswordRecoveryCode(
            email = form.email.trim(),
            verificationCode = form.verificationCode.trim(),
        )
    }

    suspend fun resetPassword(form: PasswordRecoveryFormState) {
        api.resetPassword(
            email = form.email.trim(),
            verificationCode = form.verificationCode.trim(),
            newPassword = form.password,
        )
    }

    companion object {
        fun validateEmail(
            email: String,
            language: AppLanguage,
            isValidEmail: (String) -> Boolean,
        ): String? {
            return when {
                email.isBlank() -> tr(language, "Введите e-mail", "Enter your email")
                !isValidEmail(email) -> tr(language, "Введите корректный e-mail", "Enter a valid email")
                else -> null
            }
        }

        fun validateCode(
            form: PasswordRecoveryFormState,
            language: AppLanguage,
        ): String? {
            return when {
                form.verificationCode.isBlank() -> tr(language, "Введите код подтверждения", "Enter verification code")
                form.verificationCode.trim().length < 4 -> tr(language, "Код должен быть не короче 4 символов", "Code must be at least 4 characters")
                else -> null
            }
        }

        fun validatePassword(
            form: PasswordRecoveryFormState,
            language: AppLanguage,
        ): String? {
            return when {
                form.password.length < 8 -> tr(language, "Пароль должен быть не короче 8 символов", "Password must be at least 8 characters")
                form.password != form.passwordRepeat -> tr(language, "Пароли не совпадают", "Passwords do not match")
                else -> null
            }
        }

        private fun tr(
            language: AppLanguage,
            russian: String,
            english: String,
        ): String {
            return if (language == AppLanguage.RU) russian else english
        }
    }
}
