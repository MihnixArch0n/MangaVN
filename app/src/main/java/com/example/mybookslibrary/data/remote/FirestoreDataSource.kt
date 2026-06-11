package com.example.mybookslibrary.data.remote

import com.example.mybookslibrary.data.remote.models.FirestoreLibraryItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getLibraryCollection(userId: String) =
        firestore.collection("users").document(userId).collection("library")

    suspend fun saveItem(userId: String, item: FirestoreLibraryItem) {
        getLibraryCollection(userId)
            .document(item.mangaId)
            .set(item)
            .await()
    }

    suspend fun getItem(userId: String, mangaId: String): FirestoreLibraryItem? {
        val document = getLibraryCollection(userId).document(mangaId).get().await()
        return document.toObject(FirestoreLibraryItem::class.java)
    }

    suspend fun deleteItem(userId: String, mangaId: String) {
        getLibraryCollection(userId).document(mangaId).delete().await()
    }

    suspend fun getAllItems(userId: String): List<FirestoreLibraryItem> {
        val snapshot = getLibraryCollection(userId).get().await()
        return snapshot.toObjects(FirestoreLibraryItem::class.java)
    }

    suspend fun saveAllItems(userId: String, items: List<FirestoreLibraryItem>) {
        if (items.isEmpty()) return
        val batch = firestore.batch()
        val collection = getLibraryCollection(userId)
        
        items.forEach { item ->
            batch.set(collection.document(item.mangaId), item)
        }
        
        batch.commit().await()
    }
}
