package com.noki.vpn.data

import java.util.Locale

object AppFilterPolicy {
    fun visibleApps(
        apps: List<AppInfo>,
        query: String,
        hideSystemApps: Boolean,
    ): List<AppInfo> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return apps.filter { app ->
            (!hideSystemApps || !app.isSystemApp) &&
                (
                    normalizedQuery.isBlank() ||
                        app.appName.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                        app.packageName.lowercase(Locale.ROOT).contains(normalizedQuery)
                    )
        }
    }
}

