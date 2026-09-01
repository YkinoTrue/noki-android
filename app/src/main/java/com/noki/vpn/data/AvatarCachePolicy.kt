package com.noki.vpn.data

import java.security.MessageDigest
import java.util.Locale

object AvatarCachePolicy {
    fun cacheFileName(avatarUrl: String): String {
        val normalized = avatarUrl.trim()
        val extension = when {
            normalized.substringBefore('?').endsWith(".png", ignoreCase = true) -> "png"
            normalized.substringBefore('?').endsWith(".webp", ignoreCase = true) -> "webp"
            normalized.substringBefore('?').endsWith(".jpg", ignoreCase = true) -> "jpg"
            normalized.substringBefore('?').endsWith(".jpeg", ignoreCase = true) -> "jpg"
            else -> "img"
        }
        return "avatar-${sha256Hex(normalized).take(16)}.$extension"
    }

    private fun sha256Hex(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
    }
}
