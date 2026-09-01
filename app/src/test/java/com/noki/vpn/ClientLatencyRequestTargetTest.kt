package com.noki.vpn

import com.noki.vpn.data.clientLatencyTargetKey
import com.noki.vpn.data.ServerLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientLatencyRequestTargetTest {
    @Test
    fun `latency target ignores order but changes with endpoint host`() {
        val pl = location("pl", "pl.example")
        val lv = location("lv", "lv.example")

        assertEquals(
            clientLatencyRequestTarget(listOf(pl, lv)),
            clientLatencyRequestTarget(listOf(lv, pl)),
        )
        assertNotEquals(
            clientLatencyRequestTarget(listOf(pl)),
            clientLatencyRequestTarget(listOf(location("pl", "new-pl.example"))),
        )
    }

    @Test
    fun `latency cache belongs to exact code and host`() {
        val oldHost = location("pl", "old-pl.example")
        val newHost = location("pl", "new-pl.example")
        val cache = mapOf(requireNotNull(clientLatencyTargetKey(oldHost)) to 20)

        val sameHostState = BackendSyncCoordinator.withCachedClientLatencies(
            state = AppUiState(locations = listOf(oldHost)),
            clientLatencyByTarget = cache,
        )
        val newHostState = BackendSyncCoordinator.withCachedClientLatencies(
            state = AppUiState(locations = listOf(newHost)),
            clientLatencyByTarget = cache,
        )

        assertEquals(20, sameHostState.locations.single().latencyMs)
        assertFalse(BackendSyncCoordinator.hasMissingClientLatency(sameHostState.locations, cache))
        assertNull(newHostState.locations.single().latencyMs)
        assertTrue(BackendSyncCoordinator.hasMissingClientLatency(newHostState.locations, cache))
    }

    private fun location(code: String, host: String) = ServerLocation(
        code = code,
        country = code.uppercase(),
        city = code.uppercase(),
        host = host,
        isOnline = true,
    )
}
