package com.noki.vpn.vpn

import com.noki.vpn.data.AtomicStoredSettingsStore
import com.noki.vpn.data.DefaultStoredSettingsFactory
import com.noki.vpn.data.StoredSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class VpnSettingsCommitCoordinatorTest {
    @Test
    fun `commit decides and writes against latest stored settings atomically`() {
        val baseline = DefaultStoredSettingsFactory.create()
        val previousRuntime = baseline.copy(profile = baseline.profile.copy(host = "working.example"))
        val candidate = baseline.copy(profile = baseline.profile.copy(host = "candidate.example"))
        val latest = baseline.copy(selectedPackages = setOf("latest.user.edit"))
        val store = RecordingStore(latest)
        val coordinator = VpnSettingsCommitCoordinator(store)

        val outcome = coordinator.commitRuntimeCandidate(
            previousRuntime = previousRuntime,
            preparationBaseline = baseline,
            candidate = candidate,
            result = VpnSettingsTransactionPolicy.Result.Accepted,
        )

        assertEquals("candidate.example", outcome.runtime.profile.host)
        assertEquals(setOf("latest.user.edit"), outcome.persisted.selectedPackages)
        assertEquals(outcome.persisted, store.current)
        assertEquals(1, store.updateCount)
    }
}

private class RecordingStore(
    var current: StoredSettings,
) : AtomicStoredSettingsStore {
    var updateCount: Int = 0

    override fun load(): StoredSettings = current

    override fun updateSettings(transform: (StoredSettings) -> StoredSettings): StoredSettings {
        updateCount += 1
        current = transform(current)
        return current
    }
}
