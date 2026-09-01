package com.noki.vpn

import com.noki.vpn.data.AdvancedSettings
import com.noki.vpn.data.DefaultStoredSettingsFactory
import com.noki.vpn.data.EndpointSelectionMode
import com.noki.vpn.data.UserProfile
import com.noki.vpn.data.VlessProfile
import com.noki.vpn.data.VpnEndpointOption
import com.noki.vpn.data.VpnProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VpnRuntimeSettingsSyncPolicyTest {
    @Test
    fun `runtime snapshot cannot overwrite user owned settings`() {
        val current = AppUiState(
            profile = VlessProfile(endpointCode = "current"),
            userProfile = UserProfile(
                username = "current-user",
                selectedCountryCode = "PL",
            ),
            advancedSettings = AdvancedSettings(
                protocol = VpnProtocol.TLS,
                endpointSelectionMode = EndpointSelectionMode.MANUAL,
                connectionLogsEnabled = false,
            ),
        )
        val stored = DefaultStoredSettingsFactory.create().copy(
            profile = VlessProfile(endpointCode = "runtime"),
            userProfile = UserProfile(
                username = "stale-user",
                selectedCountryCode = "LV",
                selectedServerCode = "lv-1",
            ),
            advancedSettings = AdvancedSettings(
                protocol = VpnProtocol.REALITY,
                endpointSelectionMode = EndpointSelectionMode.AUTO,
                manualEndpointCode = "runtime",
                connectionLogsEnabled = true,
            ),
            endpointOptions = listOf(VpnEndpointOption(code = "runtime")),
        )

        val merged = applyRuntimeOwnedSettingsSnapshot(current, stored)

        assertEquals("runtime", merged.profile.endpointCode)
        assertEquals("lv-1", merged.userProfile.selectedServerCode)
        assertEquals("current-user", merged.userProfile.username)
        assertEquals("PL", merged.userProfile.selectedCountryCode)
        assertEquals(VpnProtocol.TLS, merged.advancedSettings.protocol)
        assertEquals(EndpointSelectionMode.MANUAL, merged.advancedSettings.endpointSelectionMode)
        assertEquals(false, merged.advancedSettings.connectionLogsEnabled)
        assertEquals("runtime", merged.advancedSettings.manualEndpointCode)
    }

    @Test
    fun `sync key changes when user changes desired runtime selection`() {
        val before = AppUiState(
            profile = VlessProfile(endpointCode = "old"),
            advancedSettings = AdvancedSettings(protocol = VpnProtocol.AUTO),
        )
        val after = before.copy(
            profile = VlessProfile(),
            advancedSettings = before.advancedSettings.copy(protocol = VpnProtocol.TLS),
        )

        assertNotEquals(runtimeSettingsSyncKey(before), runtimeSettingsSyncKey(after))
    }
}
