package com.noki.vpn.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DelayedTaskSchedulerTest {
    @Test
    fun `dequeued callback replaced before execution does not run`() {
        val queued = mutableListOf<Runnable>()
        val scheduler = HandlerDelayedTaskScheduler(
            postDelayed = { runnable, _ ->
                queued += runnable
                true
            },
            removeCallbacks = { runnable -> queued.remove(runnable) },
        )
        val owner = Any()
        val executed = mutableListOf<String>()

        scheduler.schedule(owner, 1L) { executed += "stale" }
        val dequeuedStaleRunnable = queued.single()
        scheduler.schedule(owner, 1L) { executed += "current" }

        dequeuedStaleRunnable.run()
        queued.single().run()

        assertEquals(listOf("current"), executed)
    }

    @Test
    fun `dequeued callback cancelled before execution does not run`() {
        val queued = mutableListOf<Runnable>()
        val scheduler = HandlerDelayedTaskScheduler(
            postDelayed = { runnable, _ ->
                queued += runnable
                true
            },
            removeCallbacks = { runnable -> queued.remove(runnable) },
        )
        val owner = Any()
        val executed = mutableListOf<String>()

        scheduler.schedule(owner, 1L) { executed += "cancelled" }
        val dequeuedRunnable = queued.single()
        scheduler.cancel(owner)

        dequeuedRunnable.run()

        assertEquals(emptyList<String>(), executed)
    }

    @Test
    fun `cancelling a running callback never waits for its completion`() {
        val queued = mutableListOf<Runnable>()
        val scheduler = HandlerDelayedTaskScheduler(
            postDelayed = { runnable, _ ->
                queued += runnable
                true
            },
            removeCallbacks = { runnable -> queued.remove(runnable) },
        )
        val owner = Any()
        val taskStarted = CountDownLatch(1)
        val releaseTask = CountDownLatch(1)
        val cancelReturned = CountDownLatch(1)

        scheduler.schedule(owner, 1L) {
            taskStarted.countDown()
            releaseTask.await()
        }
        val taskThread = Thread { queued.single().run() }
        taskThread.start()
        assertTrue(taskStarted.await(1, TimeUnit.SECONDS))

        val cancelThread = Thread {
            scheduler.cancel(owner)
            cancelReturned.countDown()
        }
        cancelThread.start()
        val returnedWithoutWaiting = cancelReturned.await(1, TimeUnit.SECONDS)

        releaseTask.countDown()
        taskThread.join(1_000L)
        cancelThread.join(1_000L)

        assertTrue(returnedWithoutWaiting)
        assertTrue(!taskThread.isAlive)
        assertTrue(!cancelThread.isAlive)
    }

    @Test
    fun `cancel concurrent with post removes the newly queued callback`() {
        val queued = mutableListOf<Runnable>()
        val postEntered = CountDownLatch(1)
        val releasePost = CountDownLatch(1)
        val cancelStarted = CountDownLatch(1)
        val cancelReturned = CountDownLatch(1)
        val scheduler = HandlerDelayedTaskScheduler(
            postDelayed = { runnable, _ ->
                postEntered.countDown()
                releasePost.await()
                queued += runnable
                true
            },
            removeCallbacks = { runnable -> queued.remove(runnable) },
        )
        val owner = Any()
        val scheduleThread = Thread {
            scheduler.schedule(owner, 60_000L) { error("cancelled callback must not run") }
        }
        scheduleThread.start()
        assertTrue(postEntered.await(1, TimeUnit.SECONDS))

        val cancelThread = Thread {
            cancelStarted.countDown()
            scheduler.cancel(owner)
            cancelReturned.countDown()
        }
        cancelThread.start()
        assertTrue(cancelStarted.await(1, TimeUnit.SECONDS))
        val cancelWonBeforePostCompleted = cancelReturned.await(100, TimeUnit.MILLISECONDS)

        releasePost.countDown()
        scheduleThread.join(1_000L)
        cancelThread.join(1_000L)

        assertTrue(!cancelWonBeforePostCompleted)
        assertTrue(!scheduleThread.isAlive)
        assertTrue(!cancelThread.isAlive)
        assertEquals(emptyList<Runnable>(), queued)
    }
}
