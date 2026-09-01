package com.noki.vpn.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredSettingsPrivacyMigrationTest {
    private val codec = StoredSettingsCodec(DefaultStoredSettingsFactory::create)

    @Test
    fun firstInstallEnablesLoggingAndAutomaticUpload() {
        val settings = codec.decode(null).advancedSettings

        assertTrue(settings.connectionLogsEnabled)
        assertTrue(settings.errorLogsEnabled)
        assertTrue(settings.anonymousLogsEnabled)
    }

    @Test
    fun legacyAutomaticUploadIsResetUntilExplicitConsent() {
        val migrated = codec.decode("""{"anonymousLogsEnabled":true}""")

        assertFalse(migrated.advancedSettings.anonymousLogsEnabled)
    }

    @Test
    fun explicitConsentSurvivesCurrentCodecRoundTrip() {
        val settings = DefaultStoredSettingsFactory.create().copy(
            advancedSettings = DefaultStoredSettingsFactory.create().advancedSettings.copy(
                anonymousLogsEnabled = true,
            ),
        )

        val restored = codec.decode(codec.encode(settings))

        assertTrue(restored.advancedSettings.anonymousLogsEnabled)
    }
}
