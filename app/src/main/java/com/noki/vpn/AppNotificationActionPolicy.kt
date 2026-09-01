package com.noki.vpn

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object AppNotificationActionPolicy {
    fun shouldAccept(
        action: String?,
        nonce: String?,
        issuedNonces: Set<String>,
        allowedActions: Set<String>,
    ): Boolean {
        val safeAction = action?.trim().orEmpty()
        val safeNonce = nonce?.trim().orEmpty()
        return safeAction.isNotBlank() &&
            safeNonce.isNotBlank() &&
            safeAction in allowedActions &&
            safeNonce in issuedNonces
    }
}

object AppNotificationActionNonceStore {
    fun issue(context: Context): String {
        val nonce = UUID.randomUUID().toString()
        val updated = issuedNonces(context) + nonce
        preferences(context).edit { putStringSet(KEY_ISSUED_NONCES, updated) }
        return nonce
    }

    fun issuedNonces(context: Context): Set<String> =
        preferences(context).getStringSet(KEY_ISSUED_NONCES, emptySet()).orEmpty().toSet()

    fun consume(context: Context, nonce: String) {
        val updated = issuedNonces(context) - nonce
        preferences(context).edit { putStringSet(KEY_ISSUED_NONCES, updated) }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "noki_app_notification_action_nonces"
    private const val KEY_ISSUED_NONCES = "issued_nonces"
}
