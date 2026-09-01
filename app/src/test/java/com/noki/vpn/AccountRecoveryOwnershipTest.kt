package com.noki.vpn

import com.noki.vpn.data.PasswordRecoveryApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRecoveryOwnershipTest {
    @Test
    fun `email change rejects late recovery response`() {
        val coordinator = AccountRecoveryCoordinator(NoopRecoveryApi)
        val owner = coordinator.begin("send:old@example.com")

        coordinator.invalidate()

        assertFalse(coordinator.accepts(owner, "send:old@example.com"))
    }

    @Test
    fun `exact recovery request owner is accepted`() {
        val coordinator = AccountRecoveryCoordinator(NoopRecoveryApi)
        val owner = coordinator.begin("verify:user@example.com:1234")

        assertTrue(coordinator.accepts(owner, "verify:user@example.com:1234"))
    }
}

private object NoopRecoveryApi : PasswordRecoveryApi {
    override suspend fun sendPasswordRecoveryCode(email: String): Int = 0
    override suspend fun verifyPasswordRecoveryCode(email: String, verificationCode: String) = Unit
    override suspend fun resetPassword(email: String, verificationCode: String, newPassword: String) = Unit
}
