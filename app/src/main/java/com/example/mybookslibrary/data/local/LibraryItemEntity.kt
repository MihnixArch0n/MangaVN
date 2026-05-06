package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

enum class LibraryStatus {
    READING,
    COMPLETED,
    FAVORITE
}

class LibraryStatusConverters {
    @androidx.room.TypeConverter
    fun fromStatus(status: LibraryStatus): String = status.name

    @androidx.room.TypeConverter
    fun toStatus(value: String): LibraryStatus = try {
        LibraryStatus.valueOf(value)
    } catch (_: IllegalArgumentException) {
        LibraryStatus.READING
    }
}

@Entity(tableName = "library_items")
@TypeConverters(LibraryStatusConverters::class)
data class LibraryItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "manga_id") val manga_id: String,
    val title: String,
    @ColumnInfo(name = "cover_url") val cover_url: String,
    val status: LibraryStatus,
    @ColumnInfo(name = "last_read_chapter_id") val last_read_chapter_id: String?,
    @ColumnInfo(name = "last_read_page_index") val last_read_page_index: Int = 0,
    @ColumnInfo(name = "updated_at") val updated_at: Long
)

