package com.noki.vpn

internal object TelegramLoginStateReducer {
    fun begin(
        current: TelegramLoginState,
        purpose: TelegramAuthPurpose,
    ): TelegramLoginState.LaunchingSdk? = when (current) {
        TelegramLoginState.Idle,
        TelegramLoginState.Authenticated,
        is TelegramLoginState.Error -> TelegramLoginState.LaunchingSdk(purpose)

        is TelegramLoginState.LaunchingSdk,
        is TelegramLoginState.Exchanging -> null
    }

    fun beginExchange(current: TelegramLoginState): TelegramLoginState.Exchanging? = when (current) {
        is TelegramLoginState.LaunchingSdk -> TelegramLoginState.Exchanging(current.purpose)
        TelegramLoginState.Idle,
        TelegramLoginState.Authenticated,
        is TelegramLoginState.Error,
        is TelegramLoginState.Exchanging -> null
    }

    fun cancel(current: TelegramLoginState): TelegramLoginState = when (current) {
        is TelegramLoginState.LaunchingSdk,
        is TelegramLoginState.Error -> TelegramLoginState.Idle
        else -> current
    }

    fun authenticated(current: TelegramLoginState): TelegramLoginState = when (current) {
        is TelegramLoginState.Exchanging -> TelegramLoginState.Authenticated
        else -> current
    }

    fun error(message: String, purpose: TelegramAuthPurpose): TelegramLoginState.Error =
        TelegramLoginState.Error(message.trim(), purpose)

    fun errorMessage(current: TelegramLoginState, purpose: TelegramAuthPurpose): String? =
        (current as? TelegramLoginState.Error)?.takeIf { it.purpose == purpose }?.message

    fun isActive(current: TelegramLoginState): Boolean =
        current is TelegramLoginState.LaunchingSdk || current is TelegramLoginState.Exchanging
}
