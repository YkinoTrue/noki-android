package com.noki.vpn

import androidx.lifecycle.SavedStateHandle
import com.noki.vpn.vpn.VpnRuntimeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingVpnStartModeStateTest {
    @Test
    fun `auth temp mode survives activity state holder recreation`() {
        val handle = SavedStateHandle()
        PendingVpnStartModeState(handle).mode = VpnRuntimeMode.AUTH_TEMP

        val recreated = PendingVpnStartModeState(handle)

        assertEquals(VpnRuntimeMode.AUTH_TEMP, recreated.mode)
    }

    @Test
    fun `invalid saved mode fails closed to account`() {
        val handle = SavedStateHandle(mapOf("pending_vpn_start_mode" to "unknown"))

        assertEquals(VpnRuntimeMode.ACCOUNT, PendingVpnStartModeState(handle).mode)
    }
}
