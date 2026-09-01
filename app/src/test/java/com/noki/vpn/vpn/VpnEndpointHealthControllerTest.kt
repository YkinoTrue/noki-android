package com.noki.vpn.vpn

import com.noki.vpn.data.DefaultStoredSettingsFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnEndpointHealthControllerTest {
    @Test
    fun `replacement cancels previous heartbeat and stale owner cannot apply`() {
        val scheduler = EndpointHealthScheduler()
        val events = mutableListOf<String>()
        val controller = VpnEndpointHealthController(
            scheduler = scheduler,
            intervalMillis = 60_000L,
            launchHeartbeat = { owner, _ ->
                events += "launch:${owner.coreId}"
                CancelableTask { events += "cancel:${owner.coreId}" }
            },
        )
        val settings = DefaultStoredSettingsFactory.create()
        val first = RuntimeOwner(1L, 10L)
        val second = RuntimeOwner(2L, 20L)

        controller.start(first, settings)
        scheduler.runPending()
        controller.start(second, settings)

        assertFalse(controller.accepts(first))
        assertTrue(controller.accepts(second))

        scheduler.runPending()
        controller.stop(first)
        controller.stop(second)

        assertEquals(
            listOf("launch:10", "cancel:10", "launch:20", "cancel:20"),
            events,
        )
    }
}

private class EndpointHealthScheduler : DelayedTaskScheduler {
    private val pending = mutableMapOf<Any, () -> Unit>()

    override fun schedule(owner: Any, delayMillis: Long, task: () -> Unit) {
        pending[owner] = task
    }

    override fun cancel(owner: Any) {
        pending.remove(owner)
    }

    fun runPending() {
        val tasks = pending.values.toList()
        pending.clear()
        tasks.forEach { it() }
    }
}
