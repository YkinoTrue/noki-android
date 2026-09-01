package com.noki.vpn.ui

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPresentationPolicyTest {
    @Test
    fun `Telegram account gets link and setup labels`() {
        val profile = UserProfile(
            hasRealEmail = false,
            hasPassword = false,
            telegramLinked = true,
        )

        assertEquals("Привязать e-mail", SecurityPresentationPolicy.emailLabel(profile, AppLanguage.RU))
        assertEquals("Задать пароль", SecurityPresentationPolicy.passwordLabel(profile, AppLanguage.RU))
        assertEquals("Telegram привязан", SecurityPresentationPolicy.telegramLabel(profile, AppLanguage.RU))
    }

    @Test
    fun `regular owner gets change labels while invited device cannot mutate`() {
        val profile = UserProfile(hasRealEmail = true, hasPassword = true, telegramLinked = false)

        assertEquals("Сменить e-mail", SecurityPresentationPolicy.emailLabel(profile, AppLanguage.RU))
        assertEquals("Сменить пароль", SecurityPresentationPolicy.passwordLabel(profile, AppLanguage.RU))
        assertEquals("Привязать Telegram", SecurityPresentationPolicy.telegramLabel(profile, AppLanguage.RU))
        assertTrue(SecurityPresentationPolicy.canManageAccount("owner"))
        assertFalse(SecurityPresentationPolicy.canManageAccount("invited"))
    }
}
