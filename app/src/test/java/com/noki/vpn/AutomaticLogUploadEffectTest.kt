package com.noki.vpn

import com.noki.vpn.data.AdvancedSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticLogUploadEffectTest {
    @Test
    fun `upload starts only when complete privacy policy becomes enabled`() {
        val disabled = AdvancedSettings(
            connectionLogsEnabled = false,
            errorLogsEnabled = false,
            anonymousLogsEnabled = false,
        )
        val partiallyEnabled = disabled.copy(anonymousLogsEnabled = true)
        val enabled = partiallyEnabled.copy(connectionLogsEnabled = true, errorLogsEnabled = true)

        assertEquals(SettingsEffect.None, automaticLogUploadEffect(disabled, partiallyEnabled))
        assertEquals(SettingsEffect.StartAutomaticLogUpload, automaticLogUploadEffect(partiallyEnabled, enabled))
        assertEquals(SettingsEffect.None, automaticLogUploadEffect(enabled, enabled))
        assertEquals(SettingsEffect.None, automaticLogUploadEffect(enabled, disabled))
    }

    @Test
    fun `manual upload queues behind automatic lane instead of losing the tap`() {
        assertEquals(
            ManualLogUploadAction.Start,
            manualLogUploadAction(
                isManualUploadVisible = false,
                hasActiveLane = false,
                laneIsAutomatic = false,
            ),
        )
        assertEquals(
            ManualLogUploadAction.QueueAfterAutomatic,
            manualLogUploadAction(
                isManualUploadVisible = false,
                hasActiveLane = true,
                laneIsAutomatic = true,
            ),
        )
        assertEquals(
            ManualLogUploadAction.Ignore,
            manualLogUploadAction(
                isManualUploadVisible = true,
                hasActiveLane = true,
                laneIsAutomatic = false,
            ),
        )
    }
}
