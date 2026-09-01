package com.noki.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSecurityWorkflowCoordinatorTest {
    @Test
    fun `new action invalidates late response from previous action`() {
        val workflow = AccountSecurityWorkflowCoordinator()
        val oldOwner = workflow.begin()
        val currentOwner = workflow.begin()

        assertFalse(workflow.accepts(oldOwner))
        assertTrue(workflow.accepts(currentOwner))
    }

    @Test
    fun `dismiss invalidates active response owner`() {
        val workflow = AccountSecurityWorkflowCoordinator()
        val owner = workflow.begin()

        workflow.invalidate()

        assertFalse(workflow.accepts(owner))
    }
}
