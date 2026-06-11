package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    ERROR,
}

class DownloadStatusConverters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(raw: String): DownloadStatus =
        DownloadStatus.entries.firstOrNull { it.name == raw } ?: DownloadStatus.PENDING
}

@Entity(
    tableName = "download_queue",
    indices = [Index(value = ["manga_id"])],
)
@TypeConverters(DownloadStatusConverters::class)
data class DownloadQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "chapter_id") val chapter_id: String,
    @ColumnInfo(name = "manga_id") val manga_id: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    @ColumnInfo(name = "progress_percent") val progress_percent: Int = 0,
    @ColumnInfo(name = "error_msg") val error_msg: String? = null,
)
