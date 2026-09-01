package com.noki.vpn.data

interface DeviceActionApi {
    suspend fun clearOtherDevices(
        token: String,
        currentDeviceId: String?,
        currentDeviceKey: String?,
    ): List<BackendDevice>

    suspend fun deleteCurrentDevice(
        token: String,
        currentDeviceId: String?,
        currentDeviceKey: String?,
    )

    suspend fun deleteDevice(
        token: String,
        deviceId: String,
        currentDeviceId: String?,
        currentDeviceKey: String?,
    ): List<BackendDevice>

    suspend fun setDeviceFullAccess(
        token: String,
        deviceId: String,
        fullAccess: Boolean,
        currentDeviceId: String?,
        currentDeviceKey: String?,
    ): List<BackendDevice>
}
