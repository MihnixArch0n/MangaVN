package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class ChapterStatus {
    UNREAD,
    READING,
    COMPLETED
}

class ChapterStatusConverters {
    @TypeConverter
    fun fromStatus(status: ChapterStatus): String = status.name

    @TypeConverter
    fun toStatus(raw: String): ChapterStatus =
        ChapterStatus.entries.firstOrNull { it.name == raw } ?: ChapterStatus.UNREAD
}

@Entity(
    tableName = "chapter_progress",
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["manga_id"],
            childColumns = ["manga_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["manga_id"])]
)
@TypeConverters(ChapterStatusConverters::class)
data class ChapterProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "chapter_id") val chapter_id: String,
    @ColumnInfo(name = "manga_id") val manga_id: String,
    val status: ChapterStatus = ChapterStatus.UNREAD,
    @ColumnInfo(name = "last_read_page") val last_read_page: Int = 0,
    @ColumnInfo(name = "total_pages") val total_pages: Int = 0,
    @ColumnInfo(name = "updated_at") val updated_at: Long
)

