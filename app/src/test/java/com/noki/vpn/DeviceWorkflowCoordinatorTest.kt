package com.noki.vpn

import com.noki.vpn.data.DeviceSession
import org.junit.Assert.assertEquals
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

    @Test
    fun `local device names affect only this client display state`() {
        val devices = listOf(
            DeviceSession("one", "Galaxy S24", "Android", isCurrent = true, isOnline = true),
            DeviceSession("two", "Pixel 9", "Android", isCurrent = false, isOnline = false),
        )

        val renamed = DeviceLocalNamePolicy.apply(devices, mapOf("two" to "  Рабочий телефон  "))

        assertEquals("Galaxy S24", renamed[0].title)
        assertEquals("Рабочий телефон", renamed[1].title)
        assertEquals("Pixel 9", devices[1].title)
    }
}
