package com.noki.vpn.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDiagnosticLogPolicyTest {
    private val disabledSettings = AdvancedSettings(
        connectionLogsEnabled = false,
        errorLogsEnabled = false,
    )

    @Test
    fun `diagnostic build stores logs when user toggles are disabled`() {
        assertTrue(AppDiagnosticLogPolicy.shouldStoreAppLog(disabledSettings, diagnosticBuild = true))
    }

    @Test
    fun `regular build respects disabled user toggles`() {
        assertFalse(AppDiagnosticLogPolicy.shouldStoreAppLog(disabledSettings, diagnosticBuild = false))
    }

    @Test
    fun `automatic sending requires both common logging and explicit consent`() {
        assertFalse(
            AppDiagnosticLogPolicy.shouldUploadAutomatically(
                disabledSettings.copy(anonymousLogsEnabled = true),
            ),
        )
        assertFalse(
            AppDiagnosticLogPolicy.shouldUploadAutomatically(
                disabledSettings.copy(connectionLogsEnabled = true, anonymousLogsEnabled = false),
            ),
        )
        assertTrue(
            AppDiagnosticLogPolicy.shouldUploadAutomatically(
                disabledSettings.copy(connectionLogsEnabled = true, anonymousLogsEnabled = true),
            ),
        )
    }
}
