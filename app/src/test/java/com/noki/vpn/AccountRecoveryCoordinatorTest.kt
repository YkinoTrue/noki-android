package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.PasswordRecoveryApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountRecoveryCoordinatorTest {
    @Test
    fun validatesEmailCodeAndPasswordSteps() {
        assertEquals(
            "Enter a valid email",
            AccountRecoveryCoordinator.validateEmail(
                email = "bad",
                language = AppLanguage.EN,
                isValidEmail = { false },
            ),
        )
        assertEquals(
            "Введите код подтверждения",
            AccountRecoveryCoordinator.validateCode(
                form = PasswordRecoveryFormState(verificationCode = ""),
                language = AppLanguage.RU,
            ),
        )
        assertEquals(
            "Пароли не совпадают",
            AccountRecoveryCoordinator.validatePassword(
                form = PasswordRecoveryFormState(password = "00000000", passwordRepeat = "11111111"),
                language = AppLanguage.RU,
            ),
        )
    }

    @Test
    fun resetTrimsEmailAndVerificationCode() {
        val api = FakePasswordRecoveryApi()
        val coordinator = AccountRecoveryCoordinator(api)

        runBlocking {
            coordinator.resetPassword(
                form = PasswordRecoveryFormState(
                    email = " user@example.com ",
                    verificationCode = " 1234 ",
                    password = "00000000",
                ),
            )
        }

        assertEquals("user@example.com", api.resetEmail)
        assertEquals("1234", api.resetCode)
        assertEquals("00000000", api.resetPassword)
    }

    private class FakePasswordRecoveryApi : PasswordRecoveryApi {
        var resetEmail: String? = null
        var resetCode: String? = null
        var resetPassword: String? = null

        override suspend fun sendPasswordRecoveryCode(email: String): Int = 60

        override suspend fun verifyPasswordRecoveryCode(
            email: String,
            verificationCode: String,
        ) = Unit

        override suspend fun resetPassword(
            email: String,
            verificationCode: String,
            newPassword: String,
        ) {
            resetEmail = email
            resetCode = verificationCode
            resetPassword = newPassword
        }
    }
}

