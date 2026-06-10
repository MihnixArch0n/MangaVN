package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.TimeSource

/**
 * Downloads individual MangaDex@Home pages into offline storage.
 */
class PageDownloader
    @Inject
    constructor(
        private val mangaRepository: MangaRepository,
        private val offlineDownloadStorage: OfflineDownloadStorage,
        @param:Named("ImageOkHttpClient") private val imageOkHttpClient: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        /**
         * Downloads one page and retries it after coordinated MangaDex@Home failover.
         *
         * Concurrent callers share [AtHomeFailoverCoordinator], so one failed node refresh is
         * enough to update URL construction for retries and remaining pages.
         */
        @Suppress("TooGenericExceptionCaught")
        internal suspend fun downloadPageWithFailover(
            mangaId: String,
            chapterId: String,
            pageIndex: Int,
            failoverCoordinator: AtHomeFailoverCoordinator,
        ) {
            var attempt = 1
            while (true) {
                currentCoroutineContext().ensureActive()
                val pageAttempt = failoverCoordinator.pageAttempt(pageIndex)

                try {
                    downloadPage(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        pageIndex = pageIndex,
                        pageUrl = pageAttempt.url,
                    )
                    return
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (t: Throwable) {
                    currentCoroutineContext().ensureActive()
                    val failoverTriggered =
                        failoverCoordinator.onPageFailure(
                            chapterId = chapterId,
                            failedGeneration = pageAttempt.generation,
                        )
                    Timber.e(
                        t,
                        "downloadPage attempt failed: chapterId=%s pageIndex=%d attempt=%d failoverTriggered=%s",
                        chapterId,
                        pageIndex,
                        attempt,
                        failoverTriggered,
                    )

                    if (attempt >= MAX_PAGE_DOWNLOAD_ATTEMPTS) {
                        Timber.e(
                            t,
                            "downloadPage exhausted attempts: chapterId=%s pageIndex=%d attempts=%d",
                            chapterId,
                            pageIndex,
                            attempt,
                        )
                        throw t
                    }

                    attempt += 1
                    Timber.d(
                        "downloadPage retry scheduled: chapterId=%s pageIndex=%d nextAttempt=%d",
                        chapterId,
                        pageIndex,
                        attempt,
                    )
                }
            }
        }

        private suspend fun downloadPage(
            mangaId: String,
            chapterId: String,
            pageIndex: Int,
            pageUrl: String,
        ) = withContext(ioDispatcher) {
            val existingFile = offlineDownloadStorage.getPageFileIfExists(mangaId, chapterId, pageIndex)
            if (existingFile != null) {
                Timber.d("downloadPage skipped, already exists: chapterId=%s pageIndex=%d", chapterId, pageIndex)
                return@withContext
            }

            Timber.d("downloadPage start: chapterId=%s pageIndex=%d url=%s", chapterId, pageIndex, pageUrl)
            val startedAt = TimeSource.Monotonic.markNow()
            var bytes = 0L
            var cached = false
            var success = false

            try {
                val request =
                    Request
                        .Builder()
                        .url(pageUrl)
                        .build()
                imageOkHttpClient.newCall(request).execute().use { response ->
                    cached = response
                        .header(HEADER_X_CACHE)
                        ?.startsWith(CACHE_HIT_PREFIX, ignoreCase = true) == true
                    val body = response.body
                    if (!response.isSuccessful) {
                        bytes = responseBodySize(body)
                        error("Page download failed: HTTP ${response.code}")
                    }
                    val savedFile =
                        offlineDownloadStorage.savePage(
                            mangaId = mangaId,
                            chapterId = chapterId,
                            pageIndex = pageIndex,
                            byteStream = body.byteStream(),
                            extension = extensionFor(pageUrl, body.contentType()?.subtype),
                        )
                    bytes = savedFile.length()
                    success = true
                }
            } catch (ioException: IOException) {
                Timber.e(ioException, "downloadPage network error: chapterId=%s pageIndex=%d", chapterId, pageIndex)
                throw ioException
            }
            Timber.d("downloadPage end: chapterId=%s pageIndex=%d bytes=%d", chapterId, pageIndex, bytes)
        }

        @Suppress("TooGenericExceptionCaught")
        private fun responseBodySize(body: ResponseBody): Long =
            try {
                body.bytes().size.toLong()
            } catch (t: Throwable) {
                Timber.e(t, "responseBodySize failed")
                0L
            }

        private fun extensionFor(
            pageUrl: String,
            contentSubtype: String?,
        ): String {
            val subtype = contentSubtype?.lowercase()
            return when {
                subtype == "jpeg" || subtype == "jpg" -> "jpg"
                subtype == "png" -> "png"
                subtype == "webp" -> "webp"
                subtype == "gif" -> "gif"
                else ->
                    pageUrl
                        .substringBefore("?")
                        .substringAfterLast(".", missingDelimiterValue = "img")
                        .takeIf { it.length in MIN_EXTENSION_LENGTH..MAX_EXTENSION_LENGTH }
                        ?: "img"
            }
        }

        private companion object {
            const val MAX_PAGE_DOWNLOAD_ATTEMPTS = 3
            const val HEADER_X_CACHE = "X-Cache"
            const val CACHE_HIT_PREFIX = "HIT"
            const val MIN_EXTENSION_LENGTH = 2
            const val MAX_EXTENSION_LENGTH = 5
        }
    }
