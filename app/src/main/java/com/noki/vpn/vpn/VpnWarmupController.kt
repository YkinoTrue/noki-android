package com.noki.vpn.vpn

internal class VpnWarmupController<T>(
    private val scheduler: DelayedTaskScheduler,
    private val delayMillis: Long,
) {
    private val delayOwner = Any()
    private var pending: T? = null
    private var activeOwner: RuntimeOwner? = null
    private var activeTask: CancelableTask? = null

    @Synchronized
    fun setPending(value: T) {
        pending = value
    }

    @Synchronized
    fun start(owner: RuntimeOwner, launch: (T) -> CancelableTask?) {
        scheduler.cancel(delayOwner)
        activeTask?.cancel()
        activeTask = null
        activeOwner = owner
        scheduler.schedule(delayOwner, delayMillis) {
            val value = synchronized(this) {
                if (activeOwner != owner) return@schedule
                pending.also { pending = null }
            } ?: return@schedule
            val task = launch(value)
            synchronized(this) {
                if (activeOwner == owner) {
                    activeTask = task
                } else {
                    task?.cancel()
                }
            }
        }
    }

    @Synchronized
    fun stop(owner: RuntimeOwner?) {
        val current = activeOwner
        if (owner != null && current != owner) return
        scheduler.cancel(delayOwner)
        activeTask?.cancel()
        activeTask = null
        activeOwner = null
        pending = null
    }

    fun clear() = stop(null)
}
