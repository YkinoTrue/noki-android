package com.noki.vpn

import com.noki.vpn.data.AtomicStoredSettingsStore
import com.noki.vpn.data.RuntimeProfilePolicy
import com.noki.vpn.data.StoredSettings
import com.noki.vpn.data.VpnEndpointOption
import com.noki.vpn.data.VpnProtocol
import java.util.Locale

internal sealed interface SettingsEffect {
    data object None : SettingsEffect
    data object ApplyRuntimeSettings : SettingsEffect
    data object StartAutomaticLogUpload : SettingsEffect
}

internal data class SettingsMutationResult(
    val settings: StoredSettings,
    val effect: SettingsEffect,
)

internal class SettingsMutationCoordinator(
    private val store: AtomicStoredSettingsStore,
) {
    fun persistUiFields(state: AppUiState): StoredSettings {
        return store.updateSettings { latest ->
            latest.withUiFields(state)
        }
    }

    fun persistUiFields(
        state: AppUiState,
        effect: SettingsEffect,
    ): SettingsMutationResult = SettingsMutationResult(
        settings = persistUiFields(state),
        effect = effect,
    )

    fun persistServerSelection(countryCode: String): StoredSettings {
        val selectedCode = countryCode.trim().uppercase(Locale.ROOT)
        return store.updateSettings { latest ->
            latest.copy(
                profile = RuntimeProfilePolicy.profileAfterLocationSelection(
                    profile = latest.profile,
                    selectedLocationCode = selectedCode,
                    endpointOptions = latest.endpointOptions,
                ),
                endpointOptions = emptyList(),
                userProfile = latest.userProfile.copy(
                    selectedCountryCode = selectedCode,
                    selectedServerCode = "",
                ),
            )
        }
    }

    fun persistProtocolChange(
        state: AppUiState,
        protocol: VpnProtocol,
    ): StoredSettings {
        return store.updateSettings { latest ->
            latest.withUiFields(state).copy(
                profile = RuntimeProfilePolicy.profileAfterProtocolChange(latest.profile, protocol),
            )
        }
    }

    fun persistAutoEndpointSelection(state: AppUiState): StoredSettings {
        return store.updateSettings { latest ->
            latest.withUiFields(state).copy(profile = latest.profile.copy(uuid = ""))
        }
    }

    fun persistManualEndpointSelection(
        state: AppUiState,
        option: VpnEndpointOption,
    ): StoredSettings {
        return store.updateSettings { latest ->
            latest.withUiFields(state).copy(
                profile = RuntimeProfilePolicy.profileForManualEndpoint(latest.profile, option),
            )
        }
    }

    private fun StoredSettings.withUiFields(state: AppUiState): StoredSettings =
        copy(
            filterMode = state.filterMode,
            selectedPackages = state.selectedPackages,
            personalizationSettings = state.personalizationSettings,
            securitySettings = state.securitySettings,
            advancedSettings = state.advancedSettings,
        )
}
