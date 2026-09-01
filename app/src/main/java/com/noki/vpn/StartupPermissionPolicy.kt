package com.noki.vpn

object StartupPermissionPolicy {
    private const val ANDROID_13_API = 33

    fun shouldRequestNotificationPermissionOnFirstLaunch(
        sdkInt: Int,
        notificationGranted: Boolean,
        alreadyRequested: Boolean,
    ): Boolean =
        sdkInt >= ANDROID_13_API &&
            !notificationGranted &&
            !alreadyRequested
}
