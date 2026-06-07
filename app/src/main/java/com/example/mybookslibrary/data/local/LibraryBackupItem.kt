@file:Suppress("ktlint")

package com.example.mybookslibrary.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryBackupItem(
    @SerialName("manga_id") val mangaId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("cover_url") val coverUrl: String = "",
    @SerialName("status") val status: String = LibraryStatus.READING.name,
    @SerialName("last_read_chapter_id") val lastReadChapterId: String = "",
    @SerialName("last_read_page_index") val lastReadPageIndex: Int = 0,
    @SerialName("updated_at") val updatedAt: Long? = null,
) {
    /**
     * Converts a valid backup item to a Room entity, or returns null when required fields are absent.
     */
    fun toEntity(): LibraryItemEntity? =
        LibraryItemEntity(
            manga_id = mangaId ?: return null,
            title = title ?: return null,
            cover_url = coverUrl,
            status = LibraryStatus.entries.firstOrNull { it.name == status } ?: LibraryStatus.READING,
            last_read_chapter_id = lastReadChapterId.ifBlank { null },
            last_read_page_index = lastReadPageIndex,
            updated_at = updatedAt ?: System.currentTimeMillis(),
        )
}

/**
 * Converts a library entity to the stable JSON backup representation.
 */
fun LibraryItemEntity.toBackupItem() =
    LibraryBackupItem(
        mangaId = manga_id,
        title = title,
        coverUrl = cover_url,
        status = status.name,
        lastReadChapterId = last_read_chapter_id.orEmpty(),
        lastReadPageIndex = last_read_page_index,
        updatedAt = updated_at,
    )
