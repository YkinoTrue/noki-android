package com.noki.vpn.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNotificationHistoryStoreTest {
    @Test
    fun `accept read and delete keep notification attention state consistent`() {
        val first = BackendAppNotification("first", "First", "one", "2026-08-28T10:00:00Z")
        val second = BackendAppNotification("second", "Second", "two", "2026-08-29T10:00:00Z")
        val initial = StoredAppNotificationHistory(notifications = listOf(first), hasUnread = false)

        val accepted = AppNotificationHistoryStore.mergeState(initial, second)
        val opened = AppNotificationHistoryStore.markRead(accepted)
        val deleted = AppNotificationHistoryStore.remove(opened, "second")

        assertTrue(accepted.hasUnread)
        assertEquals(listOf("second", "first"), accepted.notifications.map(BackendAppNotification::id))
        assertTrue(!opened.hasUnread)
        assertEquals(listOf(first), deleted.notifications)
        assertTrue(!deleted.hasUnread)
    }

    @Test
    fun `legacy history array migrates as already read`() {
        val notification = BackendAppNotification(
            id = "legacy",
            title = "Noki",
            message = "Existing notification",
            createdAt = "2026-08-28T10:00:00Z",
        )

        val migrated = AppNotificationHistoryStore.decodeState(
            """[{"id":"legacy","title":"Noki","message":"Existing notification","createdAt":"2026-08-28T10:00:00Z","action":""}]""",
        )

        assertEquals(listOf(notification), migrated.notifications)
        assertTrue(!migrated.hasUnread)
    }

    @Test
    fun `duplicate delivery does not make an opened notification unread again`() {
        val notification = BackendAppNotification(
            id = "same-id",
            title = "Noki",
            message = "Already opened",
            createdAt = "2026-08-29T10:00:00Z",
        )
        val opened = StoredAppNotificationHistory(
            notifications = listOf(notification),
            hasUnread = false,
        )

        val redelivered = AppNotificationHistoryStore.mergeState(opened, notification)

        assertTrue(!redelivered.hasUnread)
        assertEquals(listOf(notification), redelivered.notifications)
    }

    @Test
    fun `deleting the last notification clears attention state`() {
        val notification = BackendAppNotification(
            id = "only-id",
            title = "Noki",
            message = "Only notification",
            createdAt = "2026-08-29T10:00:00Z",
        )

        val deleted = AppNotificationHistoryStore.remove(
            StoredAppNotificationHistory(listOf(notification), hasUnread = true),
            notification.id,
        )

        assertTrue(deleted.notifications.isEmpty())
        assertTrue(!deleted.hasUnread)
    }

    @Test
    fun `history is accepted only for a complete authenticated device session`() {
        assertTrue(AppNotificationHistoryStore.canPersistForSession(true, "token", "user", "device"))
        assertTrue(!AppNotificationHistoryStore.canPersistForSession(false, "token", "user", "device"))
        assertTrue(!AppNotificationHistoryStore.canPersistForSession(true, null, "user", "device"))
        assertTrue(!AppNotificationHistoryStore.canPersistForSession(true, "token", "", "device"))
        assertTrue(!AppNotificationHistoryStore.canPersistForSession(true, "token", "user", ""))
    }

    @Test
    fun `history deduplicates by id and keeps newest value`() {
        val old = BackendAppNotification("same", "Old", "old", "2026-08-27T10:00:00Z")
        val updated = BackendAppNotification("same", "New", "new", "2026-08-28T10:00:00Z")

        val stored = AppNotificationHistoryStore.mergeState(
            StoredAppNotificationHistory(notifications = listOf(old)),
            updated,
        ).notifications

        assertEquals(listOf(updated), stored)
    }

    @Test
    fun `history is bounded and newest first`() {
        val notifications = (0..AppNotificationHistoryStore.MAX_ENTRIES).map { index ->
            BackendAppNotification(
                id = "id-$index",
                title = "Title $index",
                message = "Message $index",
                createdAt = Instant.ofEpochSecond(index.toLong()).toString(),
            )
        }

        val stored = notifications.fold(StoredAppNotificationHistory()) { current, item ->
            AppNotificationHistoryStore.mergeState(current, item)
        }.notifications

        assertEquals(AppNotificationHistoryStore.MAX_ENTRIES, stored.size)
        assertEquals("id-${AppNotificationHistoryStore.MAX_ENTRIES}", stored.first().id)
        assertTrue(stored.none { it.id == "id-0" })
    }

    @Test
    fun `codec rejects malformed rows and preserves valid entries`() {
        val notification = BackendAppNotification(
            id = "notice-1",
            title = "Noki",
            message = "Service message",
            createdAt = "2026-08-28T10:00:00Z",
            action = "open_security_update",
        )
        val encoded = AppNotificationHistoryStore.encodeState(
            StoredAppNotificationHistory(notifications = listOf(notification)),
        )

        assertEquals(listOf(notification), AppNotificationHistoryStore.decodeState(encoded).notifications)
        assertTrue(AppNotificationHistoryStore.decodeState("not-json").notifications.isEmpty())
    }

    @Test
    fun `malformed timestamps are normalized and sorted behind valid notifications`() {
        val malformed = BackendAppNotification("bad-time", "Old", "old", "not-a-time")
        val valid = BackendAppNotification("valid-time", "New", "new", "2026-08-28T10:00:00Z")

        val stored = AppNotificationHistoryStore.mergeState(
            StoredAppNotificationHistory(notifications = listOf(malformed)),
            valid,
        ).notifications

        assertEquals(listOf("valid-time", "bad-time"), stored.map(BackendAppNotification::id))
        assertEquals(Instant.EPOCH.toString(), stored.last().createdAt)
    }
}
