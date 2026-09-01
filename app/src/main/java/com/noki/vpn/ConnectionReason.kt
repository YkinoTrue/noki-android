package com.noki.vpn

internal sealed interface ConnectionReason {
    val raw: String

    data object PermissionDenied : ConnectionReason { override val raw = "permission_denied" }
    data object EmptySelectedApps : ConnectionReason { override val raw = "empty_selected_apps" }
    data object TrafficLimit : ConnectionReason { override val raw = "traffic_limit" }
    data class Unknown(override val raw: String) : ConnectionReason

    companion object {
        fun parse(raw: String): ConnectionReason = when {
            raw == "permission_denied" -> PermissionDenied
            raw == "empty_selected_apps" -> EmptySelectedApps
            AppErrorMapper.isTrafficLimitReason(raw) -> TrafficLimit
            else -> Unknown(raw)
        }
    }
}
