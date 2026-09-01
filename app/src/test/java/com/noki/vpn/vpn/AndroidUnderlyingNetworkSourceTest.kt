package com.noki.vpn.vpn

import com.noki.vpn.data.EndpointRankingPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidUnderlyingNetworkSourceTest {
    @Test
    fun `dual Wi-Fi and cellular capabilities use Wi-Fi for ranking and handover`() {
        assertEquals(
            EndpointRankingPolicy.NetworkKind.WIFI,
            AndroidUnderlyingNetworkSource.networkKind(
                hasWifiTransport = true,
                hasCellularTransport = true,
            ),
        )
    }
}
