package com.noki.vpn.vpn

import com.noki.vpn.data.VpnConnectionState

object NetworkCapabilityPolicy {
    fun isNotSuspended(
        sdkInt: Int,
        capabilityPresent: Boolean,
    ): Boolean = sdkInt < 28 || capabilityPresent

    fun hasCompetingVpn(
        activeNetworkUsesVpn: Boolean,
        nokiState: VpnConnectionState,
    ): Boolean = activeNetworkUsesVpn &&
        (nokiState == VpnConnectionState.DISCONNECTED || nokiState == VpnConnectionState.FAILED)
}
