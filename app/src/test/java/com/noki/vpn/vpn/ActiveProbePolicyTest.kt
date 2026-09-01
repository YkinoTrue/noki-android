package com.noki.vpn.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveProbePolicyTest {
    @Test
    fun `fallback target success keeps endpoint healthy`() {
        val decision = ActiveProbePolicy.evaluate(
            targetResults = listOf(
                ActiveProbePolicy.TargetResult(delayMs = null),
                ActiveProbePolicy.TargetResult(delayMs = 240L),
            ),
            previousFailures = 1,
        )

        assertTrue(decision.treatAsHealthy)
        assertFalse(decision.scheduleRetry)
        assertFalse(decision.restartVpn)
    }

    @Test
    fun `failed probe cannot be made healthy by unrelated traffic`() {
        val decision = ActiveProbePolicy.evaluate(
            targetResults = listOf(
                ActiveProbePolicy.TargetResult(delayMs = null),
            ),
            previousFailures = 0,
        )

        assertFalse(decision.treatAsHealthy)
        assertTrue(decision.scheduleRetry)
    }

    @Test
    fun `second failed probe requests restart`() {
        val decision = ActiveProbePolicy.evaluate(
            targetResults = listOf(
                ActiveProbePolicy.TargetResult(delayMs = null),
            ),
            previousFailures = 1,
        )

        assertFalse(decision.treatAsHealthy)
        assertTrue(decision.restartVpn)
    }
}
