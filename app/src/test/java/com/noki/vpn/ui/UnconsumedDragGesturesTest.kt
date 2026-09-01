package com.noki.vpn.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UnconsumedDragGesturesTest {
    @Test
    fun `move emits and unchanged event waits`() {
        assertEquals(UnconsumedDragDecision.Emit, decision(moved = true))
        assertEquals(UnconsumedDragDecision.Wait, decision())
    }

    @Test
    fun `consumed change or missing pointer cancels`() {
        assertEquals(UnconsumedDragDecision.Cancel, decision(consumed = true))
        assertEquals(UnconsumedDragDecision.Cancel, decision(pointerPresent = false))
    }

    @Test
    fun `final up finishes while pointer handoff continues`() {
        assertEquals(UnconsumedDragDecision.Finish, decision(changedToUp = true))
        assertEquals(
            UnconsumedDragDecision.Handoff,
            decision(changedToUp = true, otherPointerPressed = true),
        )
    }

    private fun decision(
        pointerPresent: Boolean = true,
        consumed: Boolean = false,
        changedToUp: Boolean = false,
        otherPointerPressed: Boolean = false,
        moved: Boolean = false,
    ) = unconsumedDragDecision(
        pointerPresent = pointerPresent,
        consumed = consumed,
        changedToUp = changedToUp,
        otherPointerPressed = otherPointerPressed,
        moved = moved,
    )
}
