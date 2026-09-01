package com.noki.vpn.vpn

import com.noki.vpn.data.AppFilterMode

internal data class AppVpnRoutingRules(
    val allowedPackages: Set<String> = emptySet(),
    val disallowedPackages: Set<String> = emptySet(),
    val removedPackages: Set<String> = emptySet(),
    val canEstablishTunnel: Boolean = true,
    val failureReason: String? = null,
)

internal object AppVpnRoutingPolicy {
    const val EMPTY_ONLY_SELECTED_REASON = "empty_selected_apps"

    fun rules(
        appPackageName: String,
        filterMode: AppFilterMode,
        selectedPackages: Set<String>,
        isInstalled: (String) -> Boolean = { true },
    ): AppVpnRoutingRules {
        val normalized = selectedPackages
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot { it == appPackageName }
            .toSet()
        val selected = normalized.filter(isInstalled).toSet()
        val removed = normalized - selected

        return when (filterMode) {
            AppFilterMode.ALL_APPS -> AppVpnRoutingRules(
                disallowedPackages = setOf(appPackageName),
                removedPackages = removed,
            )

            AppFilterMode.ONLY_SELECTED -> {
                if (selected.isEmpty()) {
                    AppVpnRoutingRules(
                        canEstablishTunnel = false,
                        failureReason = EMPTY_ONLY_SELECTED_REASON,
                        removedPackages = removed,
                    )
                } else {
                    AppVpnRoutingRules(
                        allowedPackages = selected,
                        removedPackages = removed,
                    )
                }
            }

            AppFilterMode.ALL_EXCEPT_SELECTED -> AppVpnRoutingRules(
                disallowedPackages = setOf(appPackageName) + selected,
                removedPackages = removed,
            )
        }
    }
}
