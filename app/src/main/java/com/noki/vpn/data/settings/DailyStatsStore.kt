package com.noki.vpn.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal object DailyStatsStore {
    fun decode(raw: String?, today: LocalDate = LocalDate.now()): List<DailyStats> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val date = json.optString("date").takeIf { it.isNotBlank() } ?: continue
                    add(DailyStats(
                        date = date,
                        rxBytes = json.optLong("rxBytes", 0L).coerceAtLeast(0L),
                        txBytes = json.optLong("txBytes", 0L).coerceAtLeast(0L),
                        onlineSeconds = json.optLong("onlineSeconds", 0L).coerceAtLeast(0L),
                        sessions = json.optInt("sessions", 0).coerceAtLeast(0),
                        pingSumMs = json.optLong("pingSumMs", 0L).coerceAtLeast(0L),
                        pingSamples = json.optInt("pingSamples", 0).coerceAtLeast(0),
                    ))
                }
            }
        }.getOrDefault(emptyList()).let { prune(it, today) }
    }

    fun encode(stats: List<DailyStats>): String = JSONArray().apply {
        stats.sortedBy { it.date }.forEach { day ->
            put(JSONObject()
                .put("date", day.date)
                .put("rxBytes", day.rxBytes)
                .put("txBytes", day.txBytes)
                .put("onlineSeconds", day.onlineSeconds)
                .put("sessions", day.sessions)
                .put("pingSumMs", day.pingSumMs)
                .put("pingSamples", day.pingSamples))
        }
    }.toString()

    fun prune(stats: List<DailyStats>, today: LocalDate = LocalDate.now()): List<DailyStats> {
        val cutoff = today.minusDays(RETENTION_DAYS)
        return stats.filter { day ->
            runCatching { !LocalDate.parse(day.date).isBefore(cutoff) }.getOrDefault(false)
        }.sortedBy { it.date }
    }

    private const val RETENTION_DAYS = 370L
}
