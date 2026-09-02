package com.noki.vpn

import com.noki.vpn.data.BackendIncyDeviceCreate
import com.noki.vpn.data.IncyImportLink
import com.noki.vpn.data.V2raynSubscriptionUrl

interface IncyDeviceGateway {
    suspend fun create(name: String): BackendIncyDeviceCreate
}

data class IncyDeviceCreateResult(
    val device: com.noki.vpn.data.BackendIncyDevice,
    val importLink: IncyImportLink,
    val v2raynSubscriptionUrl: V2raynSubscriptionUrl? = null,
)

class IncyDeviceWorkflow(
    private val gateway: IncyDeviceGateway,
) {
    suspend fun create(rawName: String): IncyDeviceCreateResult {
        val name = rawName.trim()
        require(name.length in 1..80) { "invalid_incy_device_name" }
        val response = gateway.create(name)
        return IncyDeviceCreateResult(
            device = response.device,
            importLink = IncyImportLink.parse(response.importLink),
            v2raynSubscriptionUrl = response.v2raynSubscriptionUrl?.let(V2raynSubscriptionUrl::parse),
        )
    }
}
