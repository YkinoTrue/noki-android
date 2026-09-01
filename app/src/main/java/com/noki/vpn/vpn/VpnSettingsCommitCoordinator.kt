package com.noki.vpn.vpn

import com.noki.vpn.data.AtomicStoredSettingsStore
import com.noki.vpn.data.StoredSettings

internal class VpnSettingsCommitCoordinator(
    private val store: AtomicStoredSettingsStore,
) {
    fun commitRuntimeCandidate(
        previousRuntime: StoredSettings,
        preparationBaseline: StoredSettings,
        candidate: StoredSettings,
        result: VpnSettingsTransactionPolicy.Result,
    ): VpnSettingsTransactionPolicy.RuntimeCommitOutcome {
        var outcome: VpnSettingsTransactionPolicy.RuntimeCommitOutcome? = null
        val persisted = store.updateSettings { latest ->
            VpnSettingsTransactionPolicy.commitRuntimeCandidate(
                preparationBaseline = preparationBaseline,
                previousRuntime = previousRuntime,
                candidate = candidate,
                result = result,
                persisted = latest,
            ).also { outcome = it }.persisted
        }
        return checkNotNull(outcome).copy(persisted = persisted)
    }
}
