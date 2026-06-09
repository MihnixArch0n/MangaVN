package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.repository.ChapterDelivery
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

internal data class AtHomePageAttempt(
    val url: String,
    val generation: Long,
)

/**
 * Coordinates concurrent MangaDex@Home failover for a single chapter download.
 *
 * Page downloads run in parallel, so several failures can arrive together. This
 * coordinator uses a [Mutex] to pause URL construction while one coroutine
 * refreshes `/at-home/server/{chapterId}` and swaps in the replacement delivery
 * metadata for retries and remaining pages.
 */
internal class AtHomeFailoverCoordinator(
    initialDelivery: ChapterDelivery,
    private val refreshDelivery: suspend () -> ChapterDelivery,
) {
    private data class DeliveryGeneration(
        val delivery: ChapterDelivery,
        val generation: Long,
    )

    private val currentDelivery = AtomicReference(DeliveryGeneration(initialDelivery, generation = 0L))
    private val failoverMutex = Mutex()

    val totalPages: Int
        get() = currentDelivery.get().delivery.filenames.size

    /**
     * Builds a page attempt from the latest known delivery metadata.
     *
     * Holding the mutex here intentionally pauses new attempts while failover is
     * refreshing metadata, so no coroutine starts a retry using a stale base URL.
     */
    suspend fun pageAttempt(pageIndex: Int): AtHomePageAttempt =
        failoverMutex.withLock {
            val current = currentDelivery.get()
            AtHomePageAttempt(
                url = current.delivery.pageUrl(pageIndex),
                generation = current.generation,
            )
        }

    suspend fun onPageFailure(
        chapterId: String,
        failedGeneration: Long,
    ): Boolean =
        failoverMutex.withLock {
            val current = currentDelivery.get()
            if (current.generation != failedGeneration) {
                return@withLock false
            }

            Timber.d(
                "AtHome failover triggered: chapterId=%s generation=%d oldBaseUrl=%s",
                chapterId,
                failedGeneration,
                current.delivery.baseUrl,
            )
            val refreshedDelivery = refreshDelivery()
            currentDelivery.set(
                DeliveryGeneration(
                    delivery = refreshedDelivery,
                    generation = current.generation + 1,
                ),
            )
            Timber.d(
                "AtHome failover complete: chapterId=%s generation=%d newBaseUrl=%s pages=%d",
                chapterId,
                current.generation + 1,
                refreshedDelivery.baseUrl,
                refreshedDelivery.filenames.size,
            )
            true
        }
}
