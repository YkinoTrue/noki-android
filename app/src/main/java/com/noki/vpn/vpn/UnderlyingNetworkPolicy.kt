package com.noki.vpn.vpn

import com.noki.vpn.data.EndpointRankingPolicy

data class UnderlyingNetworkCandidate<T>(
    val value: T,
    val kind: EndpointRankingPolicy.NetworkKind,
    val isActive: Boolean,
    val hasInternet: Boolean,
    val isValidated: Boolean,
    val isNotSuspended: Boolean,
    val isNotMetered: Boolean,
)

enum class UnderlyingNetworkAvailability {
    None,
    Unvalidated,
    Validated,
}

data class UnderlyingNetworkObservation<T>(
    val availability: UnderlyingNetworkAvailability,
    val candidate: T? = null,
)

object UnderlyingNetworkPolicy {
    fun <T> observe(
        candidates: List<UnderlyingNetworkCandidate<T>>,
    ): UnderlyingNetworkObservation<UnderlyingNetworkCandidate<T>> {
        val candidate = candidates
            .asSequence()
            .filter { it.hasInternet && it.isNotSuspended }
            .sortedWith(candidateComparator())
            .firstOrNull()
            ?: return UnderlyingNetworkObservation(UnderlyingNetworkAvailability.None)
        return UnderlyingNetworkObservation(
            availability = if (candidate.isValidated) {
                UnderlyingNetworkAvailability.Validated
            } else {
                UnderlyingNetworkAvailability.Unvalidated
            },
            candidate = candidate,
        )
    }

    fun <T> select(candidates: List<UnderlyingNetworkCandidate<T>>): UnderlyingNetworkCandidate<T>? {
        val observation = observe(candidates)
        return observation.candidate?.takeIf {
            observation.availability == UnderlyingNetworkAvailability.Validated
        }
    }

    private fun <T> candidateComparator(): Comparator<UnderlyingNetworkCandidate<T>> =
        compareByDescending<UnderlyingNetworkCandidate<T>> { it.isValidated }
            .thenByDescending { it.isActive }
            .thenBy { candidate ->
                when (candidate.kind) {
                    EndpointRankingPolicy.NetworkKind.WIFI -> 0
                    EndpointRankingPolicy.NetworkKind.OTHER -> 1
                    EndpointRankingPolicy.NetworkKind.CELLULAR -> 2
                }
            }
            .thenByDescending { it.isNotMetered }
}
