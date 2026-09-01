package com.noki.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

internal fun clientLatencyTargetKey(location: ServerLocation): String? =
    clientLatencyTargetKey(location.code, location.host)

internal fun clientLatencyTargetKey(codeValue: String, hostValue: String): String? {
    val code = codeValue.trim().uppercase(Locale.ROOT)
    val host = hostValue.trim().lowercase(Locale.ROOT)
    if (code.isBlank() || host.isBlank()) return null
    return "$code\u0000$host"
}

class ClientLatencySampler(
    private val tcpConnect: (host: String, port: Int, timeoutMs: Int) -> Int? = DeviceLatency::measureTcpConnectMs,
    private val icmpPing: (host: String, count: Int, timeoutSeconds: Int) -> Int? = DeviceLatency::measureIcmpPingMs,
    private val locationTimeoutMillis: Long = 8_000L,
) {
    suspend fun measure(locations: List<ServerLocation>): Map<String, Int> = coroutineScope {
        val probeSlots = Semaphore(DeviceLatency.MAX_CONCURRENT_DNS_LOOKUPS)
        locations
            .asSequence()
            .filter { location -> location.isOnline }
            .mapNotNull { location ->
                val targetKey = clientLatencyTargetKey(location) ?: return@mapNotNull null
                val host = location.host.trim()
                async {
                    val latency = withTimeoutOrNull(locationTimeoutMillis.coerceAtLeast(1L)) {
                        probeSlots.withPermit {
                            runInterruptible(Dispatchers.IO) {
                                tcpConnectMedian(host)
                                    ?: icmpPing(host, LATENCY_ICMP_COUNT, LATENCY_ICMP_TIMEOUT_SECONDS)
                            }
                        }
                    }
                    latency?.let { targetKey to it }
                }
            }
            .toList()
            .awaitAll()
            .filterNotNull()
            .toMap()
    }

    private fun tcpConnectMedian(host: String): Int? {
        val samples = List(LATENCY_TCP_ATTEMPTS) {
            tcpConnect(host, LATENCY_TCP_PORT, LATENCY_TCP_TIMEOUT_MS)
        }.filterNotNull().sorted()
        if (samples.isEmpty()) return null
        return samples[samples.size / 2]
    }

    private companion object {
        const val LATENCY_TCP_PORT = 443
        const val LATENCY_TCP_ATTEMPTS = 3
        const val LATENCY_TCP_TIMEOUT_MS = 650
        const val LATENCY_ICMP_COUNT = 1
        const val LATENCY_ICMP_TIMEOUT_SECONDS = 1
    }
}
