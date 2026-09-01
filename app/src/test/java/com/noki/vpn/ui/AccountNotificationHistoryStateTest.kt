package com.noki.vpn.ui

import com.noki.vpn.data.BackendAppNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountNotificationHistoryStateTest {
    private val first = BackendAppNotification(
        id = "first",
        title = "Noki",
        message = "A very long notification body that must remain available in full.",
        createdAt = "2026-08-29T15:52:00Z",
    )
    private val second = BackendAppNotification(
        id = "second",
        title = "Second",
        message = "Second body",
        createdAt = "2026-08-29T15:53:00Z",
    )

    @Test
    fun `opening a notification exposes its complete content`() {
        val state = AccountNotificationHistoryState().open(first.id)

        assertEquals(first, state.selectedNotification(listOf(first, second)))
        assertEquals(first.message, state.selectedNotification(listOf(first, second))?.message)
    }

    @Test
    fun `rapid selection keeps the most recently opened notification`() {
        val state = AccountNotificationHistoryState()
            .open(first.id)
            .open(second.id)

        assertEquals(second, state.selectedNotification(listOf(first, second)))
    }

    @Test
    fun `closing or deleting the selected notification returns to the list`() {
        val selected = AccountNotificationHistoryState().open(first.id)

        assertNull(selected.closeDetail().selectedNotification(listOf(first, second)))
        assertNull(selected.selectedNotification(listOf(second)))
    }
}
