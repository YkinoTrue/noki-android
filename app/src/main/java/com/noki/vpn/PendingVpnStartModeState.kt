package com.noki.vpn

import androidx.lifecycle.SavedStateHandle
import com.noki.vpn.vpn.VpnRuntimeMode

internal class PendingVpnStartModeState(
    private val savedStateHandle: SavedStateHandle,
) {
    var mode: VpnRuntimeMode
        get() = runCatching {
            VpnRuntimeMode.valueOf(
                savedStateHandle.get<String>(KEY_PENDING_VPN_START_MODE).orEmpty(),
            )
        }.getOrDefault(VpnRuntimeMode.ACCOUNT)
        set(value) {
            savedStateHandle[KEY_PENDING_VPN_START_MODE] = value.name
        }

    private companion object {
        const val KEY_PENDING_VPN_START_MODE = "pending_vpn_start_mode"
    }
}
