package com.noki.vpn

object FcmRegistrationStatePolicy {
    fun shouldSyncCurrentToken(
        accessToken: String?,
        deviceId: String?,
        fcmToken: String?,
    ): Boolean =
        !accessToken.isNullOrBlank() &&
            !deviceId.isNullOrBlank() &&
            !fcmToken.isNullOrBlank()

    fun isRegisteredForCurrentDevice(
        tokenHash: String,
        registeredDeviceId: String,
        currentDeviceId: String?,
    ): Boolean {
        val cleanHash = tokenHash.trim()
        val cleanRegisteredDeviceId = registeredDeviceId.trim()
        val cleanCurrentDeviceId = currentDeviceId?.trim().orEmpty()
        return cleanHash.isNotBlank() &&
            cleanRegisteredDeviceId.isNotBlank() &&
            cleanRegisteredDeviceId == cleanCurrentDeviceId
    }
}
