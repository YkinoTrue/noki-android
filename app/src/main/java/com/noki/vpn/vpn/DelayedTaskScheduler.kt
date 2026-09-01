package com.noki.vpn.vpn

import android.os.Handler
import java.util.IdentityHashMap

internal interface DelayedTaskScheduler {
    fun schedule(owner: Any, delayMillis: Long, task: () -> Unit)

    fun cancel(owner: Any)
}

internal class HandlerDelayedTaskScheduler(
    private val postDelayed: (Runnable, Long) -> Boolean,
    private val removeCallbacks: (Runnable) -> Unit,
) : DelayedTaskScheduler {
    private val tasks = IdentityHashMap<Any, ScheduledTask>()

    internal constructor(handler: Handler) : this(handler::postDelayed, handler::removeCallbacks)

    override fun schedule(owner: Any, delayMillis: Long, task: () -> Unit) {
        cancel(owner)
        lateinit var runnable: Runnable
        lateinit var scheduledTask: ScheduledTask
        runnable = Runnable {
            val shouldRun = synchronized(tasks) {
                if (tasks[owner] === scheduledTask) {
                    tasks.remove(owner)
                    true
                } else {
                    false
                }
            }
            if (shouldRun) task()
        }
        synchronized(tasks) {
            scheduledTask = ScheduledTask(runnable)
            tasks[owner] = scheduledTask
            if (!postDelayed(runnable, delayMillis)) {
                if (tasks[owner] === scheduledTask) tasks.remove(owner)
            }
        }
    }

    override fun cancel(owner: Any) {
        val cancelledTask = synchronized(tasks) { tasks.remove(owner) }
        cancelledTask?.let { task -> removeCallbacks(task.runnable) }
    }

    private class ScheduledTask(val runnable: Runnable)
}
