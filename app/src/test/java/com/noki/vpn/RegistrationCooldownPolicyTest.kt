package com.noki.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationCooldownPolicyTest {
    @Test
    fun `remaining seconds follow elapsed deadline after long pause`() {
        val deadlineElapsedMs = 30_000L

        assertEquals(30, cooldownRemainingSeconds(deadlineElapsedMs, nowElapsedMs = 0L))
        assertEquals(30, cooldownRemainingSeconds(deadlineElapsedMs, nowElapsedMs = 1L))
        assertEquals(29, cooldownRemainingSeconds(deadlineElapsedMs, nowElapsedMs = 1_000L))
        assertEquals(1, cooldownRemainingSeconds(deadlineElapsedMs, nowElapsedMs = 29_999L))
        assertEquals(0, cooldownRemainingSeconds(deadlineElapsedMs, nowElapsedMs = 30_000L))
        assertEquals(0, cooldownRemainingSeconds(deadlineElapsedMs, nowElapsedMs = 60_000L))
    }
}
