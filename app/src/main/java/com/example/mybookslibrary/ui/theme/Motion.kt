package com.example.mybookslibrary.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Duration tokens (refactor-ui-ux.md §3.3 + §6).
 * Easing KHÔNG tự chế — dùng nguyên MotionScheme.expressive() của M3 qua MaterialTheme.
 */
object MotionTokens {
    const val DURATION_FAST_MS = 150
    const val DURATION_DEFAULT_MS = 250
    const val DURATION_EMPHASIZED_MS = 400
}

/**
 * true khi user tắt animation hệ thống (animator duration scale = 0) —
 * mọi animation trang trí (stagger, parallax, shimmer) phải render thẳng final state.
 * Provide tại MyBooksLibraryTheme; default false cho preview/test.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Đọc Settings.Global.ANIMATOR_DURATION_SCALE — nguồn chân lý cho LocalReducedMotion.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
