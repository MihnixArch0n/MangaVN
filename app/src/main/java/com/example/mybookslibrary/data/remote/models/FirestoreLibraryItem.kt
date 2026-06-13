package com.example.mybookslibrary.data.remote.models

import com.example.mybookslibrary.data.local.LibraryStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FirestoreLibraryItem(
    @DocumentId
    val mangaId: String = "",
    val title: String = "",
    val coverUrl: String? = null,
    val status: String = LibraryStatus.READING.name,
    val isFavorite: Boolean = false,
    val addedAt: Long = 0,
    val lastReadAt: Long? = null,
    val lastChapterId: String? = null,
    val lastReadPageIndex: Int = 0,
    val updatedAt: Long = 0,
    @ServerTimestamp
    val serverUpdatedAt: Date? = null
)
