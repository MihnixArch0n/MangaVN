@file:Suppress("ktlint:standard:filename")

package com.example.mybookslibrary.ui.theme

/**
 * Alpha tokens — bậc trong suốt dùng chung toàn UI (đối ứng với Dimens cho spacing).
 * Giá trị giữ NGUYÊN các literal đang dùng trong screens để không lệch golden screenshot.
 * Giá trị đặc thù một chỗ (vd scrim onboarding 0.78/0.75, reader bar 0.94) vẫn để
 * private const tại file dùng — chỉ alpha lặp lại nhiều nơi mới đưa vào đây.
 */
@Suppress("MayBeConst")
object Alphas {
    /** Scrim gradient đè lên ảnh cover để chữ trắng đạt contrast */
    val Scrim = 0.82f

    /** Container badge nổi trên ảnh / text chính trên scrim tối */
    val EmphasisVeryHigh = 0.85f

    /** Overlay/tint đậm nhưng không đặc — icon lỗi, lớp phủ surface */
    val EmphasisHigh = 0.7f

    /** Overlay/tint vừa — text phụ trên overlay */
    val EmphasisMedium = 0.6f

    /** Mờ — icon empty state, text hint */
    val EmphasisMuted = 0.5f

    /** Rất nhạt — track, divider mềm, lớp gradient nhẹ */
    val EmphasisFaint = 0.3f

    /** Nền container được chọn / control trên surface (M3 state-layer ~12%) */
    val ContainerSelected = 0.12f

    /** Nền chip tint nhẹ 10% */
    val ContainerTint = 0.1f

    /** Control container rất nhẹ trên nền sáng */
    val ContainerFaint = 0.08f
}
