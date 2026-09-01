package com.noki.vpn.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnLifecycleGenerationTest {
    @Test
    fun newerOperationInvalidatesPreviousCommit() {
        val generation = VpnLifecycleGeneration()
        val first = generation.begin()
        val second = generation.begin()

        assertFalse(generation.isCurrent(first))
        assertTrue(generation.isCurrent(second))
    }

    @Test
    fun stopInvalidationRejectsRunningOperation() {
        val generation = VpnLifecycleGeneration()
        val running = generation.begin()

        generation.invalidate()

        assertFalse(generation.isCurrent(running))
    }
}
