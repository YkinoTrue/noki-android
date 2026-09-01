package com.noki.vpn.data

import java.util.Locale
import java.util.UUID

object VpnProfileValidator {
    fun isUsable(
        settings: StoredSettings,
    ): Boolean {
        return isUsable(
            profile = settings.profile,
            advancedSettings = settings.advancedSettings,
            selectedLocationCode = settings.userProfile.selectedServerCode,
            endpointOptions = settings.endpointOptions,
        )
    }

    fun isUsable(
        profile: VlessProfile,
        advancedSettings: AdvancedSettings,
        selectedLocationCode: String = "",
        endpointOptions: List<VpnEndpointOption> = emptyList(),
    ): Boolean {
        val expectedSecurity = advancedSettings.protocol.name.lowercase(Locale.ROOT)
        val normalizedSecurity = profile.security.lowercase(Locale.ROOT)
        if (expectedSecurity != "auto" && normalizedSecurity != expectedSecurity) return false

        if (advancedSettings.endpointSelectionMode == EndpointSelectionMode.MANUAL) {
            val manualGroupKey = EndpointGroupPolicy.resolveManualGroupKey(
                settings = advancedSettings,
                endpointOptions = endpointOptions,
                profile = profile,
            )
            if (manualGroupKey.isNotBlank()) {
                val profileGroupKey = endpointOptions.firstOrNull { it.code == profile.endpointCode }
                    ?.let(EndpointGroupPolicy::groupKey)
                if (profileGroupKey != null && profileGroupKey != manualGroupKey) return false
            } else {
                val manualCode = advancedSettings.manualEndpointCode.trim()
                if (manualCode.isNotBlank() && profile.endpointCode != manualCode) return false
            }
        }

        val endpointOption = endpointOptions.firstOrNull { it.code == profile.endpointCode }
        val selectedCode = selectedLocationCode.trim()
        if (endpointOption != null &&
            selectedCode.isNotBlank() &&
            endpointOption.locationCode.isNotBlank() &&
            !endpointOption.locationCode.equals(selectedCode, ignoreCase = true)
        ) {
            return false
        }

        val hasBaseProfile = profile.host.isNotBlank() &&
            profile.port.isNotBlank() &&
            runCatching { UUID.fromString(profile.uuid) }.isSuccess
        if (!hasBaseProfile) return false
        return EndpointSecurityPolicy.isAllowedProfile(profile)
    }
}
