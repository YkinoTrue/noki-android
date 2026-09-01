package com.noki.vpn.vpn

import com.noki.vpn.data.AppFilterMode.ALL_EXCEPT_SELECTED
import com.noki.vpn.data.AppFilterMode.ONLY_SELECTED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVpnRoutingPolicyTest {
    @Test
    fun staleOnlySelectedPackageProducesExplicitEmptyError() {
        val rules = AppVpnRoutingPolicy.rules(
            appPackageName = "com.noki.vpn",
            filterMode = ONLY_SELECTED,
            selectedPackages = setOf("gone.app"),
            isInstalled = { false },
        )

        assertFalse(rules.canEstablishTunnel)
        assertEquals(AppVpnRoutingPolicy.EMPTY_ONLY_SELECTED_REASON, rules.failureReason)
        assertEquals(setOf("gone.app"), rules.removedPackages)
    }

    @Test
    fun staleExcludedPackageDoesNotAbortAllAppsMode() {
        val rules = AppVpnRoutingPolicy.rules(
            appPackageName = "com.noki.vpn",
            filterMode = ALL_EXCEPT_SELECTED,
            selectedPackages = setOf("gone.app"),
            isInstalled = { false },
        )

        assertTrue(rules.canEstablishTunnel)
        assertEquals(setOf("com.noki.vpn"), rules.disallowedPackages)
    }
}
