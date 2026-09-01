package com.noki.vpn

import com.noki.vpn.data.AppLanguage
import com.noki.vpn.data.BackendException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TelegramLoginStateReducerTest {
    @Test
    fun `link error is shown only by Security and login error only by Welcome`() {
        val linkError = TelegramLoginStateReducer.error("link failed", TelegramAuthPurpose.LINK)
        val loginError = TelegramLoginStateReducer.error("login failed", TelegramAuthPurpose.LOGIN)
        assertEquals("link failed", TelegramLoginStateReducer.errorMessage(linkError, TelegramAuthPurpose.LINK))
        assertNull(TelegramLoginStateReducer.errorMessage(linkError, TelegramAuthPurpose.LOGIN))
        assertEquals("login failed", TelegramLoginStateReducer.errorMessage(loginError, TelegramAuthPurpose.LOGIN))
        assertNull(TelegramLoginStateReducer.errorMessage(loginError, TelegramAuthPurpose.LINK))
        assertNull(TelegramLoginStateReducer.errorMessage(TelegramLoginState.Idle, TelegramAuthPurpose.LINK))
    }

    @Test
    fun `login moves from idle through SDK and exchange to authenticated`() {
        val launching = requireNotNull(
            TelegramLoginStateReducer.begin(
                current = TelegramLoginState.Idle,
                purpose = TelegramAuthPurpose.LOGIN,
            ),
        )
        val exchanging = requireNotNull(TelegramLoginStateReducer.beginExchange(launching))
        val authenticated = TelegramLoginStateReducer.authenticated(exchanging)

        assertEquals(TelegramLoginState.LaunchingSdk(TelegramAuthPurpose.LOGIN), launching)
        assertEquals(TelegramLoginState.Exchanging(TelegramAuthPurpose.LOGIN), exchanging)
        assertSame(TelegramLoginState.Authenticated, authenticated)
    }

    @Test
    fun `second tap cannot launch another Telegram flow`() {
        val launching = TelegramLoginState.LaunchingSdk(TelegramAuthPurpose.LOGIN)
        val exchanging = TelegramLoginState.Exchanging(TelegramAuthPurpose.LOGIN)

        assertNull(TelegramLoginStateReducer.begin(launching, TelegramAuthPurpose.LOGIN))
        assertNull(TelegramLoginStateReducer.begin(exchanging, TelegramAuthPurpose.LOGIN))
    }

    @Test
    fun `exchange cannot start without an active Telegram launch`() {
        assertNull(TelegramLoginStateReducer.beginExchange(TelegramLoginState.Idle))
        assertNull(TelegramLoginStateReducer.beginExchange(TelegramLoginState.Error("offline")))
    }

    @Test
    fun `cancel returns to idle without an error message`() {
        val cancelled = TelegramLoginStateReducer.cancel(
            TelegramLoginState.LaunchingSdk(TelegramAuthPurpose.LOGIN),
        )

        assertSame(TelegramLoginState.Idle, cancelled)
    }

    @Test
    fun `cancel dismisses Telegram error to idle`() {
        val dismissed = TelegramLoginStateReducer.cancel(
            TelegramLoginState.Error("offline"),
        )

        assertSame(TelegramLoginState.Idle, dismissed)
    }

    @Test
    fun `Telegram failures have stable Russian messages`() {
        assertEquals(
            "Не удалось подтвердить вход через Telegram",
            AppErrorMapper.readableTelegramAuthError(AppLanguage.RU, BackendException("invalid", 401)),
        )
        assertEquals(
            "Эта ссылка Telegram уже использована. Попробуйте войти ещё раз",
            AppErrorMapper.readableTelegramAuthError(AppLanguage.RU, BackendException("replayed", 409)),
        )
        assertEquals(
            "Слишком много попыток. Попробуйте позже",
            AppErrorMapper.readableTelegramAuthError(AppLanguage.RU, BackendException("limited", 429)),
        )
        assertEquals(
            "Вход через Telegram временно недоступен",
            AppErrorMapper.readableTelegramAuthError(AppLanguage.RU, BackendException("unavailable", 503)),
        )
        assertEquals(
            "Нет соединения с сервером",
            AppErrorMapper.readableTelegramAuthError(AppLanguage.RU, IOException("offline")),
        )
        assertEquals(
            "Не удалось получить данные от Telegram. Попробуйте ещё раз",
            AppErrorMapper.readableTelegramSdkError(AppLanguage.RU, "missing_id_token"),
        )
        assertEquals(
            "Telegram не завершил вход. Попробуйте ещё раз",
            AppErrorMapper.readableTelegramSdkError(AppLanguage.RU, "callback_not_received"),
        )
    }
}
