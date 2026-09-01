package com.noki.vpn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EndpointStartupPreflightPolicyTest {
    @Test
    fun unprovenHysteriaDoesNotValidateEarlierTcpFailures() {
        val result = EndpointStartupPreflightPolicy.selectWithTcpPrecheck(
            candidates = listOf(tcpCandidate("tcp"), hysteriaCandidate("hy2")),
            health = emptyMap(),
            networkKind = EndpointRankingPolicy.NetworkKind.CELLULAR,
            nowMillis = 0L,
            rotationIndex = { 0 },
            canReach = { false },
            allowHysteria = true,
        )

        assertEquals("hy2", result.selected?.code)
        assertFalse(result.shouldPenalizeFailedTcp)
    }
}
