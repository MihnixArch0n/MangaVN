package com.example.mybookslibrary.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot

/**
 * Một bước trong guided tour — key dùng để map với vị trí element trên màn.
 */
data class CoachMarkStep(
    val key: String,
    val titleRes: Int,
    val bodyRes: Int,
)

/**
 * State quản lý vị trí highlight targets + step hiện tại.
 * Gắn `Modifier.onGloballyPositioned { state.registerTarget("key", it) }` lên component cần highlight.
 */
@Stable
class CoachMarkState {
    private val targets = mutableStateMapOf<String, Rect>()
    var currentStep by mutableIntStateOf(0)
        internal set

    fun registerTarget(key: String, coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            targets[key] = coordinates.boundsInRoot()
        }
    }

    fun getTargetRect(key: String): Rect? = targets[key]

    fun getTargetCenter(key: String): Offset? {
        val rect = targets[key] ?: return null
        return rect.center
    }
}

@Composable
fun rememberCoachMarkState(): CoachMarkState = remember { CoachMarkState() }
