package com.noki.vpn

import com.noki.vpn.data.BackendAppNotification
import java.time.Instant

object FcmNotificationPayload {
    fun matchesAudience(
        data: Map<String, String>,
        userId: String,
        deviceId: String,
    ): Boolean = userId.isNotBlank() &&
        deviceId.isNotBlank() &&
        data["audience_user_id"] == userId &&
        data["audience_device_id"] == deviceId

    fun toBackendNotification(
        data: Map<String, String>,
        notificationTitle: String?,
        notificationBody: String?,
        messageId: String?,
        sentTimeMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): BackendAppNotification? {
        val message = firstNotBlank(
            data["message"],
            data["body"],
            notificationBody,
        ) ?: return null
        val timestampMillis = sentTimeMillis.takeIf { it > 0L } ?: nowMillis
        return BackendAppNotification(
            id = firstNotBlank(data["notification_id"], messageId) ?: "fcm-$timestampMillis",
            title = firstNotBlank(data["title"], notificationTitle) ?: "Noki",
            message = message,
            createdAt = firstNotBlank(data["created_at"]) ?: Instant.ofEpochMilli(timestampMillis).toString(),
            action = firstNotBlank(data["action"]),
        )
    }

    private fun firstNotBlank(vararg values: String?): String? =
        values.firstNotNullOfOrNull { value -> value?.trim()?.takeIf { it.isNotBlank() } }
}
