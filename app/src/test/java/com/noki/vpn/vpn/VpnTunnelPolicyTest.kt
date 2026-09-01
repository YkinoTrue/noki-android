package com.noki.vpn.vpn

import java.net.InetAddress
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnTunnelPolicyTest {
    @Test
    fun vpnAdvertisesReservedDnsInsteadOfPublicProvider() {
        val address = InetAddress.getByName(VpnTunnelPolicy.DNS_SERVER).address

        assertTrue(
            "VPN DNS must stay inside RFC 2544 benchmark space",
            address.size == 4 &&
                address[0].toInt() and 0xff == 198 &&
                address[1].toInt() and 0xfe == 18,
        )
    }
}
