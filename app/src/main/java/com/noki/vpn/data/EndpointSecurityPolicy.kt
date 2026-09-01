package com.noki.vpn.data

import java.util.Locale

object EndpointSecurityPolicy {
    private val XHTTP_MODES = setOf("stream-up", "packet-up")

    fun isAllowedCandidate(candidate: BackendEndpointCandidate): Boolean {
        if (candidate.allowInsecure) return false
        if (candidate.entryHost.isBlank()) return false

        val proxyType = candidate.proxyType.lowercase(Locale.ROOT)
        val security = candidate.security.lowercase(Locale.ROOT)
        val transport = candidate.normalizedTransport()
        val mode = candidate.normalizedTransportMode()

        if (EndpointTransportPolicy.isHysteria(candidate)) {
            return proxyType == "hysteria" &&
                transport == "hysteria" &&
                security == "tls" &&
                candidate.serverName.isNotBlank()
        }

        if (proxyType != "vless") return false
        return when (security) {
            "reality" -> candidate.hasRealityMaterial() &&
                when (transport) {
                    "tcp" -> true
                    "xhttp" -> mode in XHTTP_MODES && candidate.path.orEmpty().isNotBlank()
                    else -> false
                }
            "tls" -> transport == "tcp" && candidate.serverName.isNotBlank()
            else -> false
        }
    }

    fun isAllowedProfile(profile: VlessProfile): Boolean {
        if (profile.allowInsecure) return false
        if (profile.host.isBlank() || profile.port.isBlank()) return false

        val proxyType = profile.proxyType.lowercase(Locale.ROOT)
        val security = profile.security.lowercase(Locale.ROOT)
        val transport = normalizedProfileTransport(profile.transport)
        val mode = profile.transportMode.lowercase(Locale.ROOT).trim().ifBlank {
            if (transport == "xhttp") "stream-up" else ""
        }

        if (proxyType == "hysteria") {
            return transport == "hysteria" &&
                security == "tls" &&
                profile.serverName.isNotBlank()
        }

        if (proxyType != "vless") return false
        return when (security) {
            "reality" -> profile.serverName.isNotBlank() &&
                profile.publicKey.isNotBlank() &&
                profile.shortId.isNotBlank() &&
                when (transport) {
                    "tcp" -> true
                    "xhttp" -> mode in XHTTP_MODES && profile.path.isNotBlank()
                    else -> false
                }
            "tls" -> transport == "tcp" && profile.serverName.isNotBlank()
            else -> false
        }
    }

    private fun BackendEndpointCandidate.hasRealityMaterial(): Boolean {
        return serverName.isNotBlank() &&
            !publicKey.isNullOrBlank() &&
            !shortId.isNullOrBlank()
    }

    private fun normalizedProfileTransport(value: String): String {
        return when (value.lowercase(Locale.ROOT).trim()) {
            "raw" -> "tcp"
            "hysteria2", "hy2" -> "hysteria"
            "" -> "tcp"
            else -> value.lowercase(Locale.ROOT).trim()
        }
    }
}
