package com.example.mybookslibrary.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryBackup(
    val version: Int = CURRENT_VERSION,
    val library: List<LibraryBackupItem> = emptyList(),
    @SerialName("chapter_progress") val chapterProgress: List<ChapterProgressBackupItem> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

@Serializable
data class ChapterProgressBackupItem(
    @SerialName("chapter_id") val chapterId: String? = null,
    @SerialName("manga_id") val mangaId: String? = null,
    val status: String = ChapterStatus.UNREAD.name,
    @SerialName("last_read_page") val lastReadPage: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("updated_at") val updatedAt: Long? = null,
) {
    /** Chuyển item backup thành entity Room; trả null nếu thiếu khóa bắt buộc (chapter_id/manga_id). */
    fun toEntity(): ChapterProgressEntity? =
        ChapterProgressEntity(
            chapter_id = chapterId ?: return null,
            manga_id = mangaId ?: return null,
            status = ChapterStatus.entries.firstOrNull { it.name == status } ?: ChapterStatus.UNREAD,
            last_read_page = lastReadPage.coerceAtLeast(0),
            total_pages = totalPages.coerceAtLeast(0),
            updated_at = updatedAt ?: System.currentTimeMillis(),
        )
}

/** Chuyển entity tiến độ chương thành item backup để serialize. */
fun ChapterProgressEntity.toBackupItem() =
    ChapterProgressBackupItem(
        chapterId = chapter_id,
        mangaId = manga_id,
        status = status.name,
        lastReadPage = last_read_page,
        totalPages = total_pages,
        updatedAt = updated_at,
    )
