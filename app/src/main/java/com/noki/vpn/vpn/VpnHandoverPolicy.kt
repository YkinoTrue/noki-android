package com.noki.vpn.vpn

object VpnHandoverPolicy {
    enum class Action {
        NoAction,
        FreshStart,
        RestartTunnel,
    }

    data class Plan(
        val action: Action,
        val allowCachedFallback: Boolean,
        val forceRefreshSession: Boolean,
    )

    fun plan(
        hasTunnel: Boolean,
        activeSignature: String?,
        nextSignature: String?,
    ): Plan {
        return when {
            !hasTunnel -> Plan(
                Action.FreshStart,
                allowCachedFallback = true,
                forceRefreshSession = false,
            )
            activeSignature == nextSignature -> Plan(
                Action.NoAction,
                allowCachedFallback = false,
                forceRefreshSession = false,
            )
            else -> Plan(
                Action.RestartTunnel,
                allowCachedFallback = true,
                forceRefreshSession = false,
            )
        }
    }
}
