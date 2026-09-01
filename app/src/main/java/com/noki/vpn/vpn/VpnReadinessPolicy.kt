package com.noki.vpn.vpn

object VpnReadinessPolicy {
    fun accept(delayMs: Long?): Boolean = delayMs != null && delayMs > 0L

    fun failureReason(started: Boolean, delayMs: Long?): String? = when {
        !started -> "core_start_error"
        !accept(delayMs) -> "runtime_readiness_error"
        else -> null
    }
}
