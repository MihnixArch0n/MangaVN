package com.example.mybookslibrary.data.remote

import com.example.mybookslibrary.data.remote.models.FirestoreLibraryItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source interacting with Cloud Firestore to sync and manage user library data.
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getLibraryCollection(userId: String) =
        firestore.collection("users").document(userId).collection("library")

    /**
     * Saves a single library item to the user's Firestore collection.
     *
     * @param userId The unique ID of the authenticated user.
     * @param item The library item details to be saved.
     */
    suspend fun saveItem(userId: String, item: FirestoreLibraryItem) {
        getLibraryCollection(userId)
            .document(item.mangaId)
            .set(item)
            .await()
    }

    /**
     * Retrieves a single library item from the user's Firestore collection by manga ID.
     *
     * @param userId The unique ID of the authenticated user.
     * @param mangaId The ID of the manga.
     * @return The library item if found, otherwise null.
     */
    suspend fun getItem(userId: String, mangaId: String): FirestoreLibraryItem? {
        val document = getLibraryCollection(userId).document(mangaId).get().await()
        return document.toObject(FirestoreLibraryItem::class.java)
    }

    /**
     * Deletes a single library item from the user's Firestore collection by manga ID.
     *
     * @param userId The unique ID of the authenticated user.
     * @param mangaId The ID of the manga.
     */
    suspend fun deleteItem(userId: String, mangaId: String) {
        getLibraryCollection(userId).document(mangaId).delete().await()
    }

    /**
     * Retrieves all library items from the user's Firestore collection.
     *
     * @param userId The unique ID of the authenticated user.
     * @return A list of library items.
     */
    suspend fun getAllItems(userId: String): List<FirestoreLibraryItem> {
        val snapshot = getLibraryCollection(userId).get().await()
        return snapshot.toObjects(FirestoreLibraryItem::class.java)
    }

    /**
     * Saves a list of library items to the user's Firestore collection in a batch.
     *
     * @param userId The unique ID of the authenticated user.
     * @param items The list of library items to be saved.
     */
    suspend fun saveAllItems(userId: String, items: List<FirestoreLibraryItem>) {
        if (items.isEmpty()) return
        val batch = firestore.batch()
        val collection = getLibraryCollection(userId)

        items.forEach { item ->
            batch.set(collection.document(item.mangaId), item)
        }

        batch.commit().await()
    }

    /**
     * Deletes all user data including sub-collections and the user document from Firestore.
     *
     * @param userId The unique ID of the authenticated user.
     */
    suspend fun deleteAllUserData(userId: String) {
        val collection = getLibraryCollection(userId)
        val snapshot = collection.get().await()
        if (!snapshot.isEmpty) {
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
        firestore.collection("users").document(userId).delete().await()
    }
}
