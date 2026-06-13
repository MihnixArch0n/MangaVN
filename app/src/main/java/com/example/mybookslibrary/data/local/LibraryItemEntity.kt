package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mybookslibrary.domain.model.SyncStatus

// Yêu thích KHÔNG phải status: dùng cờ `is_favorite` riêng (một truyện yêu thích
// vẫn Đang đọc hoặc Hoàn thành). Giá trị FAVORITE cũ đã xóa — không code nào set.
enum class LibraryStatus {
    READING,
    COMPLETED,
}

class LibraryStatusConverters {
    @androidx.room.TypeConverter
    fun fromStatus(status: LibraryStatus): String = status.name

    @androidx.room.TypeConverter
    fun toStatus(value: String): LibraryStatus =
        // Fallback READING gồm cả "FAVORITE" legacy (DB rất cũ) — cờ is_favorite là cột riêng nên không mất
        LibraryStatus.entries.firstOrNull { it.name == value } ?: LibraryStatus.READING

    @androidx.room.TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @androidx.room.TypeConverter
    fun toSyncStatus(value: String): SyncStatus =
        try {
            SyncStatus.valueOf(value)
        } catch (_: IllegalArgumentException) {
            SyncStatus.PENDING_UPDATE
        }
}

@Suppress("ConstructorParameterNaming")
@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "manga_id") val manga_id: String,
    val title: String,
    @ColumnInfo(name = "cover_url") val cover_url: String,
    val status: LibraryStatus = LibraryStatus.READING,
    @ColumnInfo(name = "last_read_chapter_id") val last_read_chapter_id: String? = null,
    @ColumnInfo(name = "last_read_page_index") val last_read_page_index: Int = 0,
    @ColumnInfo(name = "added_at", defaultValue = "0") val added_at: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updated_at: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val is_favorite: Boolean = false,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE,
)
