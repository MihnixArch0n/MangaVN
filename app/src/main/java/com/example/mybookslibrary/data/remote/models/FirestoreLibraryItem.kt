package com.example.mybookslibrary.data.remote.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FirestoreLibraryItem(
    @DocumentId
    val mangaId: String = "",
    val title: String = "",
    val coverUrl: String? = null,
    val status: String = "READING",
    val addedAt: Long = 0,
    val lastReadAt: Long? = null,
    val lastChapterId: String? = null,
    val updatedAt: Long = 0,
    @ServerTimestamp
    val serverUpdatedAt: Date? = null
)
