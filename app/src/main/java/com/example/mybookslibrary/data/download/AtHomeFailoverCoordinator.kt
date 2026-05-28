package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.repository.ChapterDelivery
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
    private val errorThreshold: Int
) {
    private val currentDelivery = AtomicReference(initialDelivery)
    private val consecutiveErrors = AtomicInteger(0)
    private val failoverMutex = Mutex()

    val totalPages: Int
        get() = currentDelivery.get().filenames.size

    /**
     * Builds a page URL from the latest known delivery metadata.
     *
     * Holding the mutex here intentionally pauses new attempts while failover is
     * refreshing metadata, so no coroutine starts a retry using a stale base URL.
     */
    suspend fun pageUrl(pageIndex: Int): String = failoverMutex.withLock {
        currentDelivery.get().pageUrl(pageIndex)
    }

    fun onPageSuccess() {
        consecutiveErrors.set(0)
    }

    suspend fun onPageFailure(chapterId: String): Boolean {
        val errors = consecutiveErrors.incrementAndGet()
        if (errors < errorThreshold) return false

        return failoverMutex.withLock {
            if (consecutiveErrors.get() < errorThreshold) {
                return@withLock false
            }

            val oldDelivery = currentDelivery.get()
            Timber.d(
                "ChapterDownloadWorker failover triggered: chapterId=%s consecutiveErrors=%d oldBaseUrl=%s",
                chapterId,
                consecutiveErrors.get(),
                oldDelivery.baseUrl
            )
            val refreshedDelivery = refreshDelivery()
            currentDelivery.set(refreshedDelivery)
            consecutiveErrors.set(0)
            Timber.d(
                "ChapterDownloadWorker failover complete: chapterId=%s newBaseUrl=%s pages=%d",
                chapterId,
                refreshedDelivery.baseUrl,
                refreshedDelivery.filenames.size
            )
            true
        }
    }
}
