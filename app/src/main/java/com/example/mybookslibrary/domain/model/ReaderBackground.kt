package com.example.mybookslibrary.domain.model

/**
 * Màu nền hiển thị phía sau trang truyện trong reader.
 *
 * Lưu dưới dạng tên enum trong DataStore; UI map sang [androidx.compose.ui.graphics.Color] tương ứng.
 *
 * - [BLACK]: nền đen (mặc định) — phù hợp đọc ban đêm, OLED.
 * - [WHITE]: nền trắng — giống đọc sách giấy.
 * - [GRAY]: nền xám trung tính — giảm tương phản gắt.
 */
enum class ReaderBackground {
    BLACK,
    WHITE,
    GRAY,
    ;

    companion object {
        /** Parse tên đã lưu, fallback [BLACK] khi rỗng/không hợp lệ. */
        fun fromString(value: String?): ReaderBackground =
            entries.firstOrNull { it.name == value } ?: BLACK
    }
}
