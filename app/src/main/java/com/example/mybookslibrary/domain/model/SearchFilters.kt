package com.example.mybookslibrary.domain.model

/**
 * Bộ tiêu chí lọc cho màn Search. Mọi list rỗng nghĩa là không áp dụng tiêu chí đó.
 *
 * @property includedTagIds UUID các tag (genre/theme) phải có.
 * @property languages Mã ngôn ngữ bản dịch khả dụng (en, vi, ja…).
 * @property contentRatings Mức độ nội dung (safe/suggestive/erotica).
 * @property statuses Trạng thái phát hành (ongoing/completed/hiatus/cancelled).
 */
data class SearchFilters(
    val includedTagIds: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val contentRatings: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean =
        includedTagIds.isEmpty() &&
            languages.isEmpty() &&
            contentRatings.isEmpty() &&
            statuses.isEmpty()
}
