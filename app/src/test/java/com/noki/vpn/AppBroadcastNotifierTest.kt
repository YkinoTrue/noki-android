package com.noki.vpn

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.noki.vpn.data.BackendAppNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppBroadcastNotifierTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test
    fun `plain notification does not erase an earlier security action`() {
        val security = post("security", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)
        post("plain")

        val intent = tap(security)

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE, action(intent))
        assertTrue(accepts(intent))
    }

    @Test
    fun `security notification does not add an action to an earlier plain notification`() {
        val plain = post("plain")
        post("security", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)

        val intent = tap(plain)

        assertNull(action(intent))
        assertNull(nonce(intent))
        assertFalse(accepts(intent))
    }

    @Test
    fun `consuming one notification action does not invalidate another notification`() {
        val first = post("first", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)
        val second = post("second", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)
        val firstIntent = tap(first)
        val secondIntent = tap(second)

        assertNotEquals(nonce(firstIntent), nonce(secondIntent))
        assertTrue(accepts(firstIntent))
        AppNotificationActionNonceStore.consume(application, requireNotNull(nonce(firstIntent)))

        assertFalse(accepts(firstIntent))
        assertTrue(accepts(secondIntent))
    }

    @Test
    fun `different notification identifiers with equal hashes remain independent`() {
        val first = post("Aa", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)
        val second = post("BB")

        assertEquals(2, manager.activeNotifications.size)
        assertTrue(accepts(tap(first)))
        assertNull(action(tap(second)))
    }

    @Test
    fun `reposting the same notification replaces its action without affecting another notification`() {
        val original = post("same", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)
        val other = post("other", MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE)
        val replacement = post("same", "unsupported_action")

        assertEquals(2, manager.activeNotifications.size)
        assertEquals(original.contentIntent, replacement.contentIntent)
        val replacementIntent = tap(replacement)
        assertEquals("unsupported_action", action(replacementIntent))
        assertFalse(accepts(replacementIntent))
        assertTrue(accepts(tap(other)))
    }

    private fun post(id: String, action: String? = null): Notification {
        assertTrue(AppBroadcastNotifier.show(application, BackendAppNotification(
            id = id,
            title = id,
            message = "Message for $id",
            createdAt = "2026-09-05T00:00:00Z",
            action = action,
        )))
        return manager.activeNotifications.single {
            it.notification.extras.getString(Notification.EXTRA_TITLE) == id
        }.notification
    }

    private fun tap(notification: Notification): Intent {
        notification.contentIntent.send()
        return requireNotNull(shadowOf(application).nextStartedActivity)
    }

    private fun action(intent: Intent) = intent.getStringExtra(MainActivity.EXTRA_APP_NOTIFICATION_ACTION)

    private fun nonce(intent: Intent) = intent.getStringExtra(MainActivity.EXTRA_APP_NOTIFICATION_ACTION_NONCE)

    private fun accepts(intent: Intent) = AppNotificationActionPolicy.shouldAccept(
        action = action(intent),
        nonce = nonce(intent),
        issuedNonces = AppNotificationActionNonceStore.issuedNonces(application),
        allowedActions = setOf(MainActivity.APP_NOTIFICATION_ACTION_OPEN_SECURITY_UPDATE),
    )
}
