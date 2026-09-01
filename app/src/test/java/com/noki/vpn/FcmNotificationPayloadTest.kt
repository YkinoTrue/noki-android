package com.noki.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmNotificationPayloadTest {
    @Test
    fun `payload audience must match both current user and current device`() {
        val data = mapOf(
            "audience_user_id" to "user-1",
            "audience_device_id" to "device-1",
        )

        assertTrue(FcmNotificationPayload.matchesAudience(data, "user-1", "device-1"))
        assertFalse(FcmNotificationPayload.matchesAudience(data, "user-2", "device-1"))
        assertFalse(FcmNotificationPayload.matchesAudience(data, "user-1", "device-2"))
        assertFalse(FcmNotificationPayload.matchesAudience(emptyMap(), "user-1", "device-1"))
    }
}
