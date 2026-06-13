package com.example.mybookslibrary.ui.screens.reader

/**
 * Cầu nối phím âm lượng giữa [com.example.mybookslibrary.MainActivity] và reader.
 *
 * App là single-activity nên `Modifier.onKeyEvent` của Compose KHÔNG bắt được phím âm lượng
 * (volume key đi qua [android.app.Activity.onKeyDown]). ReaderScreen đăng ký [onVolumeKey] khi
 * tính năng "lật trang bằng phím âm lượng" đang bật và huỷ đăng ký khi rời màn hình; Activity
 * gọi handler này trong onKeyDown.
 */
object ReaderVolumeKeyHandler {
    /**
     * Handler nhận `volumeUp` (true = phím tăng, false = phím giảm) và trả về true nếu reader đã
     * tiêu thụ sự kiện (chặn đổi âm lượng hệ thống). Null = không có reader nào đang lắng nghe.
     */
    @Volatile
    var onVolumeKey: ((volumeUp: Boolean) -> Boolean)? = null
}
