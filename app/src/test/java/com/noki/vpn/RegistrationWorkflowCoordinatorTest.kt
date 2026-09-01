package com.noki.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationWorkflowCoordinatorTest {
    @Test
    fun `email change rejects late code response`() {
        val coordinator = RegistrationWorkflowCoordinator()
        val old = coordinator.begin("code:old@example.com")

        coordinator.invalidate()

        assertFalse(coordinator.accepts(old, "code:old@example.com"))
    }

    @Test
    fun `username response is accepted only for exact current key`() {
        val coordinator = RegistrationWorkflowCoordinator()
        val owner = coordinator.begin("username:alice")

        assertTrue(coordinator.accepts(owner, "username:alice"))
        assertFalse(coordinator.accepts(owner, "username:bob"))
    }

}
