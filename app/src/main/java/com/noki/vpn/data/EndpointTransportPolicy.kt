package com.noki.vpn.data

object EndpointTransportPolicy {
    fun isHysteria(candidate: BackendEndpointCandidate): Boolean {
        return candidate.proxyType.equals("hysteria", ignoreCase = true) ||
            candidate.normalizedTransport() == "hysteria"
    }

    fun requiresTcpPrecheck(candidate: BackendEndpointCandidate): Boolean {
        return !isHysteria(candidate)
    }
}
