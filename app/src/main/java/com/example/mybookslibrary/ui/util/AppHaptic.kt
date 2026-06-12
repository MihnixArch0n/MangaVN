package com.example.mybookslibrary.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Chuẩn hóa haptic: confirm nhẹ khi bookmark/chọn tab/swipe carousel; KHÔNG haptic khi cuộn.
 * Gọi `haptic.confirm()` hoặc `haptic.longPress()` — thay vì phải lấy LocalHapticFeedback rồi
 * gọi performHapticFeedback mỗi nơi.
 */
class AppHaptic(private val feedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberAppHaptic(): AppHaptic = AppHaptic(LocalHapticFeedback.current)
