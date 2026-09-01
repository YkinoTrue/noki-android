package com.noki.vpn

import com.noki.vpn.data.VpnConnectionState
import com.noki.vpn.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionUiCoordinatorTest {
    @Test
    fun `known and unknown service reasons are parsed once`() {
        assertEquals(ConnectionReason.PermissionDenied, ConnectionReason.parse("permission_denied"))
        assertEquals(ConnectionReason.EmptySelectedApps, ConnectionReason.parse("empty_selected_apps"))
        assertEquals(ConnectionReason.TrafficLimit, ConnectionReason.parse("traffic_limit"))
        assertEquals(ConnectionReason.Unknown("future_reason"), ConnectionReason.parse("future_reason"))
    }

    @Test
    fun `runtime start and readiness failures have distinct messages`() {
        assertEquals(
            "Failed to start VPN runtime",
            AppErrorMapper.readableVpnError(AppLanguage.EN, "core_start_error"),
        )
        assertEquals(
            "VPN runtime started, but traffic through it could not be confirmed",
            AppErrorMapper.readableVpnError(AppLanguage.EN, "runtime_readiness_error"),
        )
        assertEquals(
            "VPN traffic check failed",
            AppErrorMapper.localizeConnectionReason(AppLanguage.EN, "runtime_readiness_error"),
        )
    }

    @Test
    fun `empty selected apps produces typed dialog transition`() {
        val transition = ConnectionUiCoordinator().reduce(
            current = AppUiState(),
            state = VpnConnectionState.FAILED,
            reason = ConnectionReason.EmptySelectedApps,
            connectedAtMillis = null,
            nowMillis = 1L,
            dailyStats = emptyList(),
            endpointRating = "",
        )

        assertTrue(transition is ConnectionStateReducer.Transition.EmptySelectedApps)
    }

    @Test
    fun `traffic limit notice is emitted once per reached session`() {
        val coordinator = ConnectionUiCoordinator()

        assertEquals(
            ConnectionUiCoordinator.LimitNoticeDecision(true, true),
            coordinator.limitNotice(limitReached = true, forceDialog = false, hasActiveDialog = false),
        )
        assertEquals(
            ConnectionUiCoordinator.LimitNoticeDecision(false, false),
            coordinator.limitNotice(limitReached = true, forceDialog = false, hasActiveDialog = false),
        )
        coordinator.limitNotice(limitReached = false, forceDialog = false, hasActiveDialog = false)
        assertEquals(
            ConnectionUiCoordinator.LimitNoticeDecision(true, true),
            coordinator.limitNotice(limitReached = true, forceDialog = false, hasActiveDialog = false),
        )
    }
}
