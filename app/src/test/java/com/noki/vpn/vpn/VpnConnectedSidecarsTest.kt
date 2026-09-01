package com.noki.vpn.vpn

import com.noki.vpn.data.DefaultStoredSettingsFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class VpnConnectedSidecarsTest {
    @Test
    fun `same owner start is idempotent and replacement stops previous owner first`() {
        val events = mutableListOf<String>()
        val sidecars = OwnedVpnConnectedSidecars(
            onStart = { owner, _ -> events += "start:${owner.coreId}" },
            onStop = { owner -> events += "stop:${owner?.coreId}" },
        )
        val settings = DefaultStoredSettingsFactory.create()
        val first = RuntimeOwner(1L, 10L)
        val second = RuntimeOwner(2L, 20L)

        sidecars.start(first, settings)
        sidecars.start(first, settings)
        sidecars.start(second, settings)
        sidecars.stop(first)
        sidecars.stop(second)

        assertEquals(
            listOf("start:10", "stop:10", "start:20", "stop:20"),
            events,
        )
    }
}
