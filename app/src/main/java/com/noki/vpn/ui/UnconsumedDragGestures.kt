package com.noki.vpn.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull

internal suspend fun PointerInputScope.detectUnconsumedDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)
        val drag = initialDown

        onDragStart(down)
        onDrag(drag, initialUnconsumedDragAmount())
        val upEvent =
            drag(
                pointerId = drag.id,
                onDrag = { onDrag(it, it.positionChange()) },
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        when (
            unconsumedDragDecision(
                pointerPresent = true,
                consumed = change.isConsumed,
                changedToUp = change.changedToUpIgnoreConsumed(),
                otherPointerPressed = false,
                moved = true,
            )
        ) {
            UnconsumedDragDecision.Cancel -> return null
            UnconsumedDragDecision.Finish -> return change
            UnconsumedDragDecision.Emit -> onDrag(change)
            UnconsumedDragDecision.Handoff,
            UnconsumedDragDecision.Wait,
            -> Unit
        }
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }
        val otherDown = event.changes.fastFirstOrNull { it.pressed && it.id != pointer }
        when (
            unconsumedDragDecision(
                pointerPresent = dragEvent != null,
                consumed = false,
                changedToUp = dragEvent?.changedToUpIgnoreConsumed() == true,
                otherPointerPressed = otherDown != null,
                moved = dragEvent?.let { it.previousPosition != it.position } == true,
            )
        ) {
            UnconsumedDragDecision.Cancel -> return null
            UnconsumedDragDecision.Finish,
            UnconsumedDragDecision.Emit,
            -> return dragEvent
            UnconsumedDragDecision.Handoff -> pointer = checkNotNull(otherDown).id
            UnconsumedDragDecision.Wait -> Unit
        }
    }
}

internal enum class UnconsumedDragDecision {
    Cancel,
    Finish,
    Handoff,
    Emit,
    Wait,
}

internal fun initialUnconsumedDragAmount(): Offset = Offset.Zero

internal fun unconsumedDragDecision(
    pointerPresent: Boolean,
    consumed: Boolean,
    changedToUp: Boolean,
    otherPointerPressed: Boolean,
    moved: Boolean,
): UnconsumedDragDecision = when {
    !pointerPresent || consumed -> UnconsumedDragDecision.Cancel
    changedToUp && otherPointerPressed -> UnconsumedDragDecision.Handoff
    changedToUp -> UnconsumedDragDecision.Finish
    moved -> UnconsumedDragDecision.Emit
    else -> UnconsumedDragDecision.Wait
}
