package com.example.mybookslibrary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val password: String,
    @ColumnInfo(name = "avatar_path") val avatar_path: String?,
    @ColumnInfo(name = "created_at") val created_at: Long
)

