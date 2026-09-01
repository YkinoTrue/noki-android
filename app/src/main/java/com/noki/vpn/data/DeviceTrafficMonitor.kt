package com.noki.vpn.data

import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceTrafficSnapshot(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
)

object DeviceTrafficMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _snapshot = MutableStateFlow(DeviceTrafficSnapshot())
    val snapshot: StateFlow<DeviceTrafficSnapshot> = _snapshot.asStateFlow()

    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            val uid = Process.myUid()
            var lastRxBytes = TrafficStats.getUidRxBytes(uid)
            var lastTxBytes = TrafficStats.getUidTxBytes(uid)
            var lastTimestamp = SystemClock.elapsedRealtime()

            if (lastRxBytes == TrafficStats.UNSUPPORTED.toLong() ||
                lastTxBytes == TrafficStats.UNSUPPORTED.toLong()
            ) {
                _snapshot.value = DeviceTrafficSnapshot()
                return@launch
            }

            _snapshot.value = DeviceTrafficSnapshot(downloadMbps = 0.0, uploadMbps = 0.0)

            while (true) {
                delay(1_000L)

                val currentRxBytes = TrafficStats.getUidRxBytes(uid)
                val currentTxBytes = TrafficStats.getUidTxBytes(uid)
                val currentTimestamp = SystemClock.elapsedRealtime()
                if (currentRxBytes == TrafficStats.UNSUPPORTED.toLong() ||
                    currentTxBytes == TrafficStats.UNSUPPORTED.toLong()
                ) {
                    _snapshot.value = DeviceTrafficSnapshot()
                    continue
                }

                val elapsedSeconds = ((currentTimestamp - lastTimestamp).coerceAtLeast(1L)) / 1_000.0
                val rxDelta = (currentRxBytes - lastRxBytes).coerceAtLeast(0L)
                val txDelta = (currentTxBytes - lastTxBytes).coerceAtLeast(0L)
                _snapshot.value = DeviceTrafficSnapshot(
                    downloadMbps = rxDelta * 8.0 / 1_000_000.0 / elapsedSeconds,
                    uploadMbps = txDelta * 8.0 / 1_000_000.0 / elapsedSeconds,
                )

                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTimestamp = currentTimestamp
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        _snapshot.value = DeviceTrafficSnapshot()
    }
}
