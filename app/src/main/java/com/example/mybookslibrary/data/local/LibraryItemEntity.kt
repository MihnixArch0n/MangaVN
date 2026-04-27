package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "manga_id") val manga_id: String, // PK - lấy từ MangaDex
    val title: String,
    @ColumnInfo(name = "cover_url") val cover_url: String,
    @ColumnInfo(name = "added_at") val added_at: Long
)

