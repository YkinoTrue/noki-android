package com.noki.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerStatsRefreshPolicyTest {
    @Test
    fun `stats throttle uses elapsed time since last successful refresh`() {
        assertTrue(shouldRefreshServerStats(lastSuccessElapsedMs = 0L, nowElapsedMs = 10L, minIntervalMs = 60L))
        assertFalse(shouldRefreshServerStats(lastSuccessElapsedMs = 10L, nowElapsedMs = 69L, minIntervalMs = 60L))
        assertTrue(shouldRefreshServerStats(lastSuccessElapsedMs = 10L, nowElapsedMs = 70L, minIntervalMs = 60L))
        assertTrue(shouldRefreshServerStats(lastSuccessElapsedMs = 100L, nowElapsedMs = 5L, minIntervalMs = 60L))
    }
}
