package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ViewConfiguration
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Observes edge taps without consuming pointer events, allowing Telephoto to keep handling
 * double-tap and pinch zoom gestures on the same image.
 */
internal fun Modifier.observeConfirmedEdgeTaps(
    viewConfiguration: ViewConfiguration,
    onConfirmedEdgeTap: (Offset) -> Unit,
    onManualDrag: () -> Unit
): Modifier = pointerInput(viewConfiguration, onConfirmedEdgeTap, onManualDrag) {
    coroutineScope {
        var confirmationJob: Job? = null
        val tapTracker = ConfirmedEdgeTapTracker(
            touchSlop = viewConfiguration.touchSlop,
            onConfirmedEdgeTap = onConfirmedEdgeTap
        )

        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            val downPosition = down.position
            var isTap = true
            var hasReportedDrag = false
            var pointerCount = 1
            var upPosition: Offset? = null

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                pointerCount = maxOf(pointerCount, event.changes.size)
                if (event.changes.any { change ->
                        (change.position - downPosition).getDistance() > viewConfiguration.touchSlop
                    }
                ) {
                    isTap = false
                    if (!hasReportedDrag) {
                        hasReportedDrag = true
                        confirmationJob?.cancel()
                        confirmationJob = null
                        tapTracker.cancelPendingTap()
                        Timber.d("Reader pager observer detected manual drag")
                        onManualDrag()
                    }
                }

                val trackedPointer = event.changes.firstOrNull { it.id == down.id }
                if (trackedPointer == null || !trackedPointer.pressed) {
                    upPosition = trackedPointer?.position ?: downPosition
                    break
                }
            }

            if (!isTap || pointerCount > 1) {
                Timber.d("Reader pager observer ignored gesture: isTap=%s pointerCount=%d", isTap, pointerCount)
                confirmationJob?.cancel()
                confirmationJob = null
                tapTracker.cancelPendingTap()
                return@awaitEachGesture
            }
            val tapPosition = upPosition
            confirmationJob?.cancel()
            confirmationJob = null
            when (tapTracker.onTap(tapPosition)) {
                EdgeTapConfirmation.Pending -> {
                    confirmationJob = launch {
                        delay(viewConfiguration.doubleTapTimeoutMillis)
                        if (tapTracker.confirmPendingTap(tapPosition)) {
                            Timber.d("Reader pager observer confirmed tap: x=%.1f y=%.1f", tapPosition.x, tapPosition.y)
                        }
                    }
                }
                EdgeTapConfirmation.ConfirmedPrevious -> {
                    confirmationJob = launch {
                        delay(viewConfiguration.doubleTapTimeoutMillis)
                        if (tapTracker.confirmPendingTap(tapPosition)) {
                            Timber.d("Reader pager observer confirmed tap: x=%.1f y=%.1f", tapPosition.x, tapPosition.y)
                        }
                    }
                }
                EdgeTapConfirmation.SuppressedDoubleTap -> {
                    Timber.d("Reader pager observer suppressed double tap: x=%.1f y=%.1f", tapPosition.x, tapPosition.y)
                }
            }
        }
    }
}

internal class ConfirmedEdgeTapTracker(
    private val touchSlop: Float,
    private val onConfirmedEdgeTap: (Offset) -> Unit
) {
    private var pendingTap: PendingTap? = null

    fun onTap(position: Offset): EdgeTapConfirmation {
        val previousTap = pendingTap
        val isDoubleTap = previousTap != null &&
            (previousTap.position - position).getDistance() <= touchSlop

        return if (isDoubleTap) {
            pendingTap = null
            EdgeTapConfirmation.SuppressedDoubleTap
        } else {
            previousTap?.let { onConfirmedEdgeTap(it.position) }
            pendingTap = PendingTap(position)
            if (previousTap == null) {
                EdgeTapConfirmation.Pending
            } else {
                EdgeTapConfirmation.ConfirmedPrevious
            }
        }
    }

    fun confirmPendingTap(position: Offset): Boolean {
        if (pendingTap?.position != position) return false
        pendingTap = null
        onConfirmedEdgeTap(position)
        return true
    }

    fun cancelPendingTap() {
        pendingTap = null
    }
}

internal enum class EdgeTapConfirmation {
    Pending,
    ConfirmedPrevious,
    SuppressedDoubleTap
}

private data class PendingTap(val position: Offset)
