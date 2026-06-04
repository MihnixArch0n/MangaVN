package com.example.mybookslibrary.domain.model

/**
 * Tag của MangaDex dùng cho bộ lọc Search.
 *
 * @property id UUID tag — giá trị gửi lên query `includedTags[]` (không phải tên).
 * @property name Tên hiển thị đã chọn theo ngôn ngữ ưu tiên.
 * @property group Nhóm tag: genre / theme / format / content.
 */
data class MangaTag(
    val id: String,
    val name: String,
    val group: String,
)
