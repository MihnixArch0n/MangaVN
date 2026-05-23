package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    @ColumnInfo(name = "avatar_path") val avatar_path: String? = null,
    @ColumnInfo(name = "created_at") val created_at: Long = System.currentTimeMillis(),
    val email: String? = null,
    @ColumnInfo(name = "google_id") val google_id: String? = null
)

