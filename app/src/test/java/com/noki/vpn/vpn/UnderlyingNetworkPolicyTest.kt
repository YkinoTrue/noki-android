package com.noki.vpn.vpn

import com.noki.vpn.data.EndpointRankingPolicy.NetworkKind.CELLULAR
import com.noki.vpn.data.EndpointRankingPolicy.NetworkKind.OTHER
import com.noki.vpn.data.EndpointRankingPolicy.NetworkKind.WIFI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnderlyingNetworkPolicyTest {
    @Test
    fun validatedCellularBeatsCaptiveWifi() {
        val selected = UnderlyingNetworkPolicy.select(
            listOf(
                candidate("wifi", WIFI, active = false, validated = false),
                candidate("lte", CELLULAR, active = false, validated = true),
            ),
        )

        assertEquals("lte", selected?.value)
    }

    @Test
    fun activeUnvalidatedCandidateRemainsObservableWhileValidatedSelectionIsAbsent() {
        val candidates = listOf(
            candidate("lte", CELLULAR, active = true, validated = false),
        )

        val observation = UnderlyingNetworkPolicy.observe(candidates)

        assertEquals(UnderlyingNetworkAvailability.Unvalidated, observation.availability)
        assertEquals("lte", observation.candidate?.value)
        assertNull(UnderlyingNetworkPolicy.select(candidates))
    }

    @Test
    fun missingEligibleCandidateIsReportedExplicitly() {
        val observation = UnderlyingNetworkPolicy.observe(
            listOf(
                candidate(
                    value = "wifi",
                    kind = WIFI,
                    active = true,
                    validated = false,
                    notSuspended = false,
                ),
            ),
        )

        assertEquals(UnderlyingNetworkAvailability.None, observation.availability)
        assertNull(observation.candidate)
    }

    @Test
    fun activeValidatedOtherTransportIsSupported() {
        val selected = UnderlyingNetworkPolicy.select(
            listOf(
                candidate("wifi", WIFI, active = false, validated = true),
                candidate("ethernet", OTHER, active = true, validated = true),
            ),
        )

        assertEquals("ethernet", selected?.value)
    }

    @Test
    fun suspendedNetworkIsRejected() {
        assertNull(
            UnderlyingNetworkPolicy.select(
                listOf(
                    candidate(
                        value = "wifi",
                        kind = WIFI,
                        active = true,
                        validated = true,
                        notSuspended = false,
                    ),
                ),
            ),
        )
    }

    private fun candidate(
        value: String,
        kind: com.noki.vpn.data.EndpointRankingPolicy.NetworkKind,
        active: Boolean,
        validated: Boolean,
        notSuspended: Boolean = true,
    ): UnderlyingNetworkCandidate<String> {
        return UnderlyingNetworkCandidate(
            value = value,
            kind = kind,
            isActive = active,
            hasInternet = true,
            isValidated = validated,
            isNotSuspended = notSuspended,
            isNotMetered = true,
        )
    }
}
