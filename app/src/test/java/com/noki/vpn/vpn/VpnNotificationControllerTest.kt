package com.noki.vpn.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnNotificationControllerTest {
    @Test
    fun `foreground notification contract keeps low-noise channel and disconnect restart actions`() {
        assertEquals("noki_vpn_channel", VpnNotificationContract.CHANNEL_ID)
        assertEquals(true, VpnNotificationContract.ONGOING)
        assertEquals(false, VpnNotificationContract.AUTO_CANCEL)
        assertEquals(listOf("Отключить", "Рестарт"), VpnNotificationContract.ACTION_LABELS)
    }

    @Test
    fun `screen off stops speed updates but keeps polling and screen on resumes`() {
        val scheduler = NotificationScheduler()
        var speedUpdates = 0
        var polls = 0
        var pollCancellations = 0
        val controller = VpnNotificationController(
            scheduler = scheduler,
            speedUpdateIntervalMillis = 1_000L,
            pollIntervalMillis = 60_000L,
            updateActiveNotification = { speedUpdates += 1 },
            pollNotifications = {
                polls += 1
                CancelableTask { pollCancellations += 1 }
            },
        )

        controller.setScreenOn(true)
        controller.start()
        scheduler.runReady()
        assertEquals(1, speedUpdates)
        assertEquals(1, polls)

        controller.setScreenOn(false)
        scheduler.runAllOnce()
        assertEquals(1, speedUpdates)
        assertEquals(2, polls)

        controller.setScreenOn(true)
        scheduler.runReady()
        assertEquals(2, speedUpdates)

        controller.stop()
        scheduler.runAllOnce()
        assertEquals(2, speedUpdates)
        assertEquals(2, polls)
        assertEquals(2, pollCancellations)
    }
}

private class NotificationScheduler : DelayedTaskScheduler {
    private data class Pending(val delayMillis: Long, val task: () -> Unit)

    private val pending = mutableMapOf<Any, Pending>()

    override fun schedule(owner: Any, delayMillis: Long, task: () -> Unit) {
        pending[owner] = Pending(delayMillis, task)
    }

    override fun cancel(owner: Any) {
        pending.remove(owner)
    }

    fun runReady() {
        runMatching { it.delayMillis == 0L }
    }

    fun runAllOnce() {
        runMatching { true }
    }

    private fun runMatching(predicate: (Pending) -> Boolean) {
        val ready = pending.filterValues(predicate).values.toList()
        pending.entries.removeAll { predicate(it.value) }
        ready.forEach { it.task() }
    }
}
