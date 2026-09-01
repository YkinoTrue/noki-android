package com.noki.vpn

import com.noki.vpn.data.VpnIncidentReport

class AppLogUploadCoordinator(
    private val exportLogs: () -> String,
    private val shouldUploadAutomatically: () -> Boolean,
    private val markAutomaticallyUploaded: () -> Unit,
    private val upload: suspend (UploadRequest) -> Unit,
) {
    data class DeviceContext(
        val token: String,
        val deviceId: String?,
        val deviceKey: String?,
        val deviceName: String,
    )

    data class UploadRequest(
        val token: String,
        val deviceId: String?,
        val deviceKey: String?,
        val deviceName: String,
        val logsText: String,
        val incident: VpnIncidentReport? = null,
    )

    fun captureLogs(): String = exportLogs()

    suspend fun uploadManual(context: DeviceContext, logsText: String = captureLogs()) {
        uploadCurrentLogs(context, logsText = logsText)
        markAutomaticallyUploaded()
    }

    suspend fun uploadAutomatic(context: DeviceContext, logsText: String = captureLogs()) {
        uploadCurrentLogs(context, logsText = logsText)
        markAutomaticallyUploaded()
    }

    suspend fun uploadIncident(context: DeviceContext, incident: VpnIncidentReport) {
        uploadCurrentLogs(context, incident)
    }

    fun isAutomaticUploadDue(anonymousLogsEnabled: Boolean): Boolean {
        if (!anonymousLogsEnabled) return false
        if (!shouldUploadAutomatically()) return false
        return true
    }

    private suspend fun uploadCurrentLogs(
        context: DeviceContext,
        incident: VpnIncidentReport? = null,
        logsText: String = captureLogs(),
    ) {
        upload(
            UploadRequest(
                token = context.token,
                deviceId = context.deviceId,
                deviceKey = context.deviceKey,
                deviceName = context.deviceName,
                logsText = logsText,
                incident = incident,
            ),
        )
    }
}
