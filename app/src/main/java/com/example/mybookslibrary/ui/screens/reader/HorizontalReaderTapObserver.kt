package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ViewConfiguration
import timber.log.Timber

/**
 * Consumes gestures only while pager navigation is animating.
 *
 * Idle taps are intentionally left to Telephoto's confirmed click/double-click handling.
 * During animation, zoom is blocked and edge taps are converted into queued navigation.
 */
internal fun Modifier.consumeNavigationDuringAnimation(
    viewConfiguration: ViewConfiguration,
    isNavigationActive: () -> Boolean,
    isNavigationTap: (Offset) -> Boolean,
    onNavigationTap: (Offset) -> Unit,
): Modifier =
    pointerInput(
        viewConfiguration,
        isNavigationActive,
        isNavigationTap,
        onNavigationTap,
    ) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            if (!isNavigationActive()) {
                return@awaitEachGesture
            }

            val downPosition = down.position
            down.consume()
            Timber.v(
                "Reader pager animation shield down consumed: x=%.1f y=%.1f time=%d",
                downPosition.x,
                downPosition.y,
                down.uptimeMillis,
            )

            var isTap = true
            var pointerCount = 1
            var upPosition: Offset? = null

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                pointerCount = maxOf(pointerCount, event.changes.size)

                if (event.changes.any { change ->
                        (change.position - downPosition).getDistance() > viewConfiguration.touchSlop
                    }
                ) {
                    isTap = false
                }

                val trackedPointer = event.changes.firstOrNull { it.id == down.id }
                if (trackedPointer == null || !trackedPointer.pressed) {
                    upPosition = trackedPointer?.position ?: downPosition
                    break
                }
            }

            if (!isTap || pointerCount > 1) {
                Timber.v(
                    "Reader pager animation shield ignored gesture: isTap=%s pointerCount=%d",
                    isTap,
                    pointerCount,
                )
                return@awaitEachGesture
            }

            val tapPosition = upPosition
            val isEdgeTap = isNavigationTap(tapPosition)
            Timber.v(
                "Reader pager animation shield tap: x=%.1f y=%.1f edge=%s",
                tapPosition.x,
                tapPosition.y,
                isEdgeTap,
            )
            if (isEdgeTap) {
                onNavigationTap(tapPosition)
            }
        }
    }
