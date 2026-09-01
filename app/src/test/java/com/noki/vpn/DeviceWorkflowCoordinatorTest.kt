package com.noki.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceWorkflowCoordinatorTest {
    @Test
    fun `late device result cannot apply after session replacement`() {
        val coordinator = DeviceWorkflowCoordinator()
        val first = coordinator.begin(DeviceSessionSnapshot("one", "key-1", "owner"))

        val second = coordinator.begin(DeviceSessionSnapshot("two", "key-2", "invited"))

        assertFalse(coordinator.accepts(first))
        assertTrue(coordinator.accepts(second))

        coordinator.invalidate()

        assertFalse(coordinator.accepts(second))
    }
}
