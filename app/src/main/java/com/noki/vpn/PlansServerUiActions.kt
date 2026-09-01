package com.noki.vpn

import android.os.SystemClock
import com.noki.vpn.data.BillingCycle
import com.noki.vpn.data.VpnConnectionState

private const val SERVER_STATS_REFRESH_MIN_INTERVAL_MS = 60_000L

internal fun shouldRefreshServerStats(
    lastSuccessElapsedMs: Long,
    nowElapsedMs: Long,
    minIntervalMs: Long,
): Boolean = lastSuccessElapsedMs <= 0L ||
    nowElapsedMs < lastSuccessElapsedMs ||
    nowElapsedMs - lastSuccessElapsedMs >= minIntervalMs

internal fun AppUiRuntime.setBillingCycle(cycle: BillingCycle) {
    uiState = uiState.copy(billingCycle = cycle)
}

internal fun AppUiRuntime.selectServer(code: String) {
    val selectedCode = code.trim()
    if (selectedCode.isBlank() || selectedCode == uiState.userProfile.selectedCountryCode.trim()) return

    val preChangeConnectionState = uiState.connectionState
    val staleEndpointOptionsRefresh = endpointOptionsRefreshJob
    endpointOptionsRefreshJob = null
    endpointOptionsRefreshCountryCode = null
    staleEndpointOptionsRefresh?.cancel()
    val persisted = settingsMutationCoordinator.persistServerSelection(selectedCode)
    val next = uiState.copy(
        userProfile = persisted.userProfile,
        profile = persisted.profile,
        endpointOptions = persisted.endpointOptions,
        endpointOptionsCountryCode = null,
        inlineMessage = tr(
            uiState.personalizationSettings.language,
            "Сервер выбран",
            "Server selected",
        ),
    )
    uiState = next
    when (preChangeConnectionState) {
        VpnConnectionState.CONNECTED,
        VpnConnectionState.CONNECTING,
        VpnConnectionState.FAILED,
        -> vpnCommands.restart()
        VpnConnectionState.DISCONNECTED -> Unit
    }
}

internal fun AppUiRuntime.refreshServers() {
    refreshAllData(showNetworkFailureInline = true)
}

internal fun AppUiRuntime.refreshServerStats() {
    if (authSessionCoordinator.attempt() == null) return
    val now = SystemClock.elapsedRealtime()
    if (!shouldRefreshServerStats(
            lastSuccessElapsedMs = lastServerStatsRefreshElapsedMs,
            nowElapsedMs = now,
            minIntervalMs = SERVER_STATS_REFRESH_MIN_INTERVAL_MS,
        )
    ) {
        return
    }
    launchBackendRefresh(BackendRefreshTrigger.Stats)
}

internal fun AppUiRuntime.refreshOfflineStats() {
    uiState = uiState.copy(dailyStats = repository.loadDailyStats())
}

internal fun AppUiRuntime.refreshAllData(
    showNetworkFailureInline: Boolean = true,
    refreshClientLatency: Boolean = false,
) {
    launchBackendRefresh(
        trigger = BackendRefreshTrigger.UserRefresh,
        showNetworkFailureInline = showNetworkFailureInline,
        refreshClientLatency = refreshClientLatency,
    )
}
