package com.noki.vpn.ui

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.UserProfile

internal object SecurityPresentationPolicy {
    fun emailLabel(profile: UserProfile, language: AppLanguage): String =
        if (profile.hasRealEmail) {
            tr(language, "Сменить e-mail", "Change email")
        } else {
            tr(language, "Привязать e-mail", "Link email")
        }

    fun passwordLabel(profile: UserProfile, language: AppLanguage): String =
        if (profile.hasPassword) {
            tr(language, "Сменить пароль", "Change password")
        } else {
            tr(language, "Задать пароль", "Set password")
        }

    fun telegramLabel(profile: UserProfile, language: AppLanguage): String =
        if (profile.telegramLinked) {
            tr(language, "Telegram привязан", "Telegram linked")
        } else {
            tr(language, "Привязать Telegram", "Link Telegram")
        }

    fun canManageAccount(accessRole: String): Boolean =
        accessRole.equals("owner", ignoreCase = true)

    private fun tr(language: AppLanguage, russian: String, english: String): String =
        if (language == AppLanguage.RU) russian else english
}
