package com.noki.vpn

import com.noki.vpn.data.VpnIncidentReport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLogUploadCoordinatorTest {
    @Test
    fun `incident upload includes current logs and incident metadata`() = runBlocking {
        var uploaded: AppLogUploadCoordinator.UploadRequest? = null
        val incident = VpnIncidentReport(
            id = "incident-id",
            reason = "readiness_failed",
            countryCode = "LV",
            locationCode = "lv-2",
            recoveryAttempts = 3,
            outcome = "recovered",
            occurredAt = "2026-07-27T10:00:00Z",
        )
        val coordinator = AppLogUploadCoordinator(
            exportLogs = { "safe logs" },
            shouldUploadAutomatically = { false },
            markAutomaticallyUploaded = {},
            upload = { uploaded = it },
        )

        coordinator.uploadIncident(
            AppLogUploadCoordinator.DeviceContext(
                token = "token",
                deviceId = "device-id",
                deviceKey = "device-key",
                deviceName = "Phone",
            ),
            incident,
        )

        assertEquals("safe logs", uploaded?.logsText)
        assertEquals(incident, uploaded?.incident)
    }

    @Test
    fun `automatic upload is marked only after network success`() = runBlocking {
        var marked = false
        var shouldFail = true
        val coordinator = AppLogUploadCoordinator(
            exportLogs = { "safe logs" },
            shouldUploadAutomatically = { true },
            markAutomaticallyUploaded = { marked = true },
            upload = {
                if (shouldFail) error("offline")
            },
        )
        val context = AppLogUploadCoordinator.DeviceContext(
            token = "token",
            deviceId = "device-id",
            deviceKey = "device-key",
            deviceName = "Phone",
        )

        assertTrue(coordinator.isAutomaticUploadDue(true))
        assertFalse(marked)
        runCatching { coordinator.uploadAutomatic(context) }
        assertFalse(marked)

        shouldFail = false
        coordinator.uploadAutomatic(context)
        assertTrue(marked)
    }
}
