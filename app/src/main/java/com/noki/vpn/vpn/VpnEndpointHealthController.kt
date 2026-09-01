package com.noki.vpn.vpn

import com.noki.vpn.data.StoredSettings

internal class VpnEndpointHealthController(
    private val scheduler: DelayedTaskScheduler,
    private val intervalMillis: Long,
    private val launchHeartbeat: (RuntimeOwner, StoredSettings) -> CancelableTask?,
) {
    private val scheduleOwner = Any()
    private var activeOwner: RuntimeOwner? = null
    private var settings: StoredSettings? = null
    private var activeTask: CancelableTask? = null

    @Synchronized
    fun start(owner: RuntimeOwner, settings: StoredSettings) {
        stop(null)
        activeOwner = owner
        this.settings = settings
        scheduleNext(owner)
    }

    @Synchronized
    fun stop(owner: RuntimeOwner?) {
        val current = activeOwner
        if (owner != null && current != owner) return
        scheduler.cancel(scheduleOwner)
        activeTask?.cancel()
        activeTask = null
        activeOwner = null
        settings = null
    }

    @Synchronized
    fun accepts(owner: RuntimeOwner): Boolean = activeOwner == owner

    @Synchronized
    fun currentSettings(): StoredSettings? = settings

    private fun scheduleNext(owner: RuntimeOwner) {
        scheduler.schedule(scheduleOwner, intervalMillis) {
            val snapshot = synchronized(this) {
                if (activeOwner != owner) return@schedule
                activeTask?.cancel()
                activeTask = null
                settings
            } ?: return@schedule
            val task = launchHeartbeat(owner, snapshot)
            synchronized(this) {
                if (activeOwner == owner) {
                    activeTask = task
                    scheduleNext(owner)
                } else {
                    task?.cancel()
                }
            }
        }
    }
}
