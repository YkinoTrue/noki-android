package com.noki.vpn.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnWarmupControllerTest {
    @Test
    fun `replacement cancels pending and active warmup owned by previous core`() {
        val scheduler = WarmupScheduler()
        val events = mutableListOf<String>()
        val controller = VpnWarmupController<String>(scheduler, delayMillis = 100L)
        val first = RuntimeOwner(1L, 1L)
        val second = RuntimeOwner(2L, 2L)
        controller.setPending("first")
        controller.start(first) { value ->
            events += "launch:$value"
            CancelableTask { events += "cancel:$value" }
        }
        scheduler.runPending()

        controller.setPending("second")
        controller.start(second) { value ->
            events += "launch:$value"
            CancelableTask { events += "cancel:$value" }
        }
        scheduler.runPending()
        controller.stop(second)

        assertEquals(
            listOf("launch:first", "cancel:first", "launch:second", "cancel:second"),
            events,
        )
    }
}

private class WarmupScheduler : DelayedTaskScheduler {
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
