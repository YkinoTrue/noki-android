package com.noki.vpn

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthPurposeStateTest {
    @Test
    fun `new attempt and invalidation reject stale async results`() {
        val handle = SavedStateHandle()
        val state = TelegramAuthPurposeState(handle)

        val first = state.begin(TelegramAuthPurpose.LOGIN)
        val second = state.begin(TelegramAuthPurpose.LINK)

        assertFalse(state.isCurrent(first))
        assertTrue(state.isCurrent(second))
        assertEquals(TelegramAuthPurpose.LINK, TelegramAuthPurposeState(handle).purpose)

        state.invalidate()

        assertFalse(state.isCurrent(second))
        assertNull(state.currentAttemptId)
        assertNull(state.purpose)
    }

    @Test
    fun `new attempt with the same purpose invalidates earlier work`() {
        val state = TelegramAuthPurposeState(SavedStateHandle())

        val first = state.begin(TelegramAuthPurpose.LOGIN)
        val second = state.begin(TelegramAuthPurpose.LOGIN)

        assertFalse(state.isCurrent(first))
        assertTrue(state.isCurrent(second))
    }
}
