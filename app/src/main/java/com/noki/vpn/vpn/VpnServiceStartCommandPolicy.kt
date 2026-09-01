package com.noki.vpn.vpn

internal class OwnedDelayedCallback(callback: () -> Unit) {
    val runnable = Runnable(callback)

    fun schedule(
        remove: (Runnable) -> Unit,
        post: (Runnable) -> Unit,
    ) {
        remove(runnable)
        post(runnable)
    }

    fun cancel(remove: (Runnable) -> Unit) {
        remove(runnable)
    }
}

enum class VpnRuntimeMode {
    ACCOUNT,
    AUTH_TEMP,
}

object VpnServiceStartCommandPolicy {
    enum class ActiveOperation { Stop, Restart, Connection }

    enum class ActiveStartDecision { QueueAfterCleanup, CoalesceWithRestart, Ignore }

    enum class TemporaryStopDecision { NotRequested, StopAndRevoke, RevokeOnly }

    fun activeStartDecision(operation: ActiveOperation): ActiveStartDecision = when (operation) {
        ActiveOperation.Stop -> ActiveStartDecision.QueueAfterCleanup
        ActiveOperation.Restart -> ActiveStartDecision.CoalesceWithRestart
        ActiveOperation.Connection -> ActiveStartDecision.Ignore
    }

    data class StartOptions(
        val forceRefreshSession: Boolean,
        val allowCachedFallback: Boolean,
        val runtimeMode: VpnRuntimeMode = VpnRuntimeMode.ACCOUNT,
    )

    fun startOptions(
        isNullIntent: Boolean,
        action: String?,
        refreshSessionExtra: Boolean,
    ): StartOptions {
        if (action == TEMPORARY_VPN_ACTION) {
            return StartOptions(
                forceRefreshSession = true,
                allowCachedFallback = false,
                runtimeMode = VpnRuntimeMode.AUTH_TEMP,
            )
        }
        val isSystemAlwaysOn = action == SYSTEM_VPN_SERVICE_ACTION
        val forceRefreshSession = isNullIntent || isSystemAlwaysOn || refreshSessionExtra
        return StartOptions(
            forceRefreshSession = forceRefreshSession,
            allowCachedFallback = true,
            runtimeMode = VpnRuntimeMode.ACCOUNT,
        )
    }

    fun temporaryStopDecision(
        action: String?,
        runtimeMode: VpnRuntimeMode,
    ): TemporaryStopDecision {
        if (action != STOP_AND_REVOKE_TEMPORARY_ACTION) return TemporaryStopDecision.NotRequested
        return if (runtimeMode == VpnRuntimeMode.AUTH_TEMP) {
            TemporaryStopDecision.StopAndRevoke
        } else {
            TemporaryStopDecision.RevokeOnly
        }
    }

    private const val SYSTEM_VPN_SERVICE_ACTION = "android.net.VpnService"
    private const val TEMPORARY_VPN_ACTION = "com.noki.vpn.START_TEMPORARY"
    private const val STOP_AND_REVOKE_TEMPORARY_ACTION =
        "com.noki.vpn.STOP_AND_REVOKE_AUTH_TEMP"
}
