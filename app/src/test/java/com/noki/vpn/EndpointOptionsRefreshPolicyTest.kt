package com.noki.vpn

import com.noki.vpn.data.VpnEndpointOption
import com.noki.vpn.data.EndpointGroupPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointOptionsRefreshPolicyTest {
    @Test
    fun `cached endpoint options are reused only for the same country`() {
        assertFalse(
            shouldRefreshEndpointOptions(
                force = false,
                optionsCount = 2,
                loadedCountryCode = "PL",
                selectedCountryCode = "PL",
            ),
        )
        assertTrue(
            shouldRefreshEndpointOptions(
                force = false,
                optionsCount = 2,
                loadedCountryCode = "PL",
                selectedCountryCode = "LV",
            ),
        )
        assertTrue(
            shouldRefreshEndpointOptions(
                force = true,
                optionsCount = 2,
                loadedCountryCode = "PL",
                selectedCountryCode = "PL",
            ),
        )
    }

    @Test
    fun `manual endpoint is selectable only from the list loaded for current country`() {
        val stalePolish = VpnEndpointOption(
            code = "pl-1",
            locationCode = "pl-1",
            host = "192.0.2.10",
        )
        val currentLatvian = VpnEndpointOption(
            code = "lv-1",
            locationCode = "lv-1",
            host = "192.0.2.20",
            security = "reality",
            transport = "tcp",
            port = 443,
        )
        val currentLatvianAlternatePort = currentLatvian.copy(code = "lv-2", port = 8443)
        val groupedLatvian = EndpointGroupPolicy.manualOptions(
            listOf(currentLatvian, currentLatvianAlternatePort),
        ).single()

        assertFalse(
            isManualEndpointSelectable(
                option = stalePolish,
                endpointOptions = listOf(stalePolish),
                loadedCountryCode = "PL",
                selectedCountryCode = "LV",
            ),
        )
        assertFalse(
            isManualEndpointSelectable(
                option = stalePolish,
                endpointOptions = listOf(currentLatvian),
                loadedCountryCode = "LV",
                selectedCountryCode = "LV",
            ),
        )
        assertTrue(
            isManualEndpointSelectable(
                option = groupedLatvian,
                endpointOptions = listOf(currentLatvian, currentLatvianAlternatePort),
                loadedCountryCode = "LV",
                selectedCountryCode = "LV",
            ),
        )
    }

    @Test
    fun `manual endpoint list is hidden until its country identity is proven`() {
        val option = VpnEndpointOption(
            code = "pl-1",
            locationCode = "pl-1",
            host = "192.0.2.10",
        )
        val cachedWithoutIdentity = AppUiState(
            userProfile = com.noki.vpn.data.UserProfile(selectedCountryCode = "PL"),
            endpointOptions = listOf(option),
            endpointOptionsCountryCode = null,
        )
        val proven = cachedWithoutIdentity.copy(endpointOptionsCountryCode = "PL")

        assertTrue(manualEndpointOptionsForCurrentCountry(cachedWithoutIdentity).isEmpty())
        assertTrue(manualEndpointOptionsForCurrentCountry(proven).isNotEmpty())
    }
}
