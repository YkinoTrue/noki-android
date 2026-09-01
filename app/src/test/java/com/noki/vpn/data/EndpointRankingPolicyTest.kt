package com.noki.vpn.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointRankingPolicyTest {
    @Test
    fun staleScoreMovesTowardBaselineBeforeSelection() {
        val dayMillis = 24 * 60 * 60 * 1_000L
        val nowMillis = dayMillis * 2
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(tcpCandidate("stale"), tcpCandidate("fresh")),
            health = mapOf(
                "stale" to EndpointHealth(score = 60, lastUpdatedAtMillis = nowMillis - dayMillis),
                "fresh" to EndpointHealth(score = 65, lastUpdatedAtMillis = nowMillis),
            ),
            networkKind = EndpointRankingPolicy.NetworkKind.WIFI,
            nowMillis = nowMillis,
            rotationIndex = { 0 },
        )

        assertEquals("stale", selected?.candidate?.code)
    }

    @Test
    fun newResultAppliesToRecoveredStaleScore() {
        val dayMillis = 24 * 60 * 60 * 1_000L
        val updated = EndpointRankingPolicy.updateAfterResult(
            previous = EndpointHealth(
                score = 0,
                lastUpdatedAtMillis = dayMillis,
            ),
            success = true,
            nowMillis = dayMillis * 10,
        )

        assertEquals(78, updated.score)
    }

    @Test
    fun warmupKeepsBestAndExploresLeastRecentlyMeasuredEndpoint() {
        val ranked = listOf(
            tcpCandidate("best"),
            tcpCandidate("recent"),
            tcpCandidate("unmeasured"),
            tcpCandidate("old"),
        )

        val selected = EndpointRankingPolicy.selectWarmupCandidates(
            rankedCandidates = ranked,
            health = mapOf(
                "best" to EndpointHealth(lastUpdatedAtMillis = 400L),
                "recent" to EndpointHealth(lastUpdatedAtMillis = 300L),
                "old" to EndpointHealth(lastUpdatedAtMillis = 100L),
            ),
            maxCandidates = 2,
        )

        assertEquals(listOf("best", "unmeasured"), selected.map { it.code })
    }

    @Test
    fun wifiClassOrderRemainsStrongerThanCrossClassScore() {
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(
                tcpCandidate("tcp"),
                tcpCandidate("xhttp").copy(
                    transport = "xhttp",
                    transportMode = "stream-up",
                    flow = null,
                ),
            ),
            health = mapOf(
                "tcp" to EndpointHealth(score = 5, lastUpdatedAtMillis = 1_000L),
                "xhttp" to EndpointHealth(score = 100, lastUpdatedAtMillis = 1_000L),
            ),
            networkKind = EndpointRankingPolicy.NetworkKind.WIFI,
            nowMillis = 1_000L,
            rotationIndex = { 0 },
        )

        assertEquals("tcp", selected?.candidate?.code)
    }

    @Test
    fun cellularHysteriaRemainsBehindRealityFallback() {
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(tcpCandidate("tcp"), hysteriaCandidate("hy2")),
            health = mapOf(
                "tcp" to EndpointHealth(score = 5, lastUpdatedAtMillis = 1_000L),
                "hy2" to EndpointHealth(score = 100, lastUpdatedAtMillis = 1_000L),
            ),
            networkKind = EndpointRankingPolicy.NetworkKind.CELLULAR,
            nowMillis = 1_000L,
            rotationIndex = { 0 },
        )

        assertEquals("tcp", selected?.candidate?.code)
    }

    @Test
    fun successfulEndpointAlwaysClearsExistingCooldown() {
        val updated = EndpointRankingPolicy.updateAfterResult(
            previous = EndpointHealth(score = 10, cooldownUntilMillis = 99_000L),
            success = true,
            nowMillis = 10_000L,
        )

        assertEquals(0L, updated.cooldownUntilMillis)
    }

    @Test
    fun bestCoolingEndpointIsUsedAsLastResort() {
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(tcpCandidate("weak"), tcpCandidate("best")),
            health = mapOf(
                "weak" to EndpointHealth(score = 20, cooldownUntilMillis = 99_000L),
                "best" to EndpointHealth(score = 60, cooldownUntilMillis = 99_000L),
            ),
            networkKind = EndpointRankingPolicy.NetworkKind.WIFI,
            nowMillis = 10_000L,
            rotationIndex = { 0 },
        )

        assertEquals("best", selected?.candidate?.code)
    }

    @Test
    fun successfulSampleUpdatesQuarterWeightEwma() {
        val updated = EndpointRankingPolicy.updateAfterResult(
            previous = EndpointHealth(latencyEwmaMs = 400L),
            success = true,
            nowMillis = 10_000L,
            latencyMs = 800L,
        )

        assertEquals(500L, updated.latencyEwmaMs)
        assertEquals(10_000L, updated.latencyUpdatedAtMillis)
    }

    @Test
    fun freshLowerLatencyBreaksEqualScoreTie() {
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(tcpCandidate("slow"), tcpCandidate("fast")),
            health = mapOf(
                "slow" to EndpointHealth(score = 80, latencyEwmaMs = 2_000L, latencyUpdatedAtMillis = 1_000L),
                "fast" to EndpointHealth(score = 80, latencyEwmaMs = 100L, latencyUpdatedAtMillis = 1_000L),
            ),
            networkKind = EndpointRankingPolicy.NetworkKind.WIFI,
            nowMillis = 2_000L,
            rotationIndex = { 0 },
        )

        assertEquals("fast", selected?.candidate?.code)
    }

    @Test
    fun higherBackendWeightBreaksEqualHealthAndLatencyTie() {
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(tcpCandidate("light", weight = 10), tcpCandidate("heavy", weight = 100)),
            health = emptyMap(),
            networkKind = EndpointRankingPolicy.NetworkKind.WIFI,
            nowMillis = 2_000L,
            rotationIndex = { 0 },
        )

        assertEquals("heavy", selected?.candidate?.code)
    }

    @Test
    fun manualHysteriaIsEligibleOnOtherTransport() {
        val selected = EndpointRankingPolicy.select(
            candidates = listOf(hysteriaCandidate("hy2")),
            health = emptyMap(),
            networkKind = EndpointRankingPolicy.NetworkKind.OTHER,
            nowMillis = 0L,
            rotationIndex = { 0 },
            allowHysteria = true,
        )

        assertEquals("hy2", selected?.candidate?.code)
    }
}
