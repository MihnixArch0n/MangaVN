package com.example.mybookslibrary.data.download

import android.content.Context
import com.example.mybookslibrary.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles app-private chapter download files.
 *
 * Downloaded pages are stored under
 * `filesDir/offline_manga/{mangaId}/{chapterId}/` so they remain private to the
 * app and require no storage permission.
 */
@Singleton
class OfflineDownloadStorage
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        /**
         * Persists one chapter page using a stable page-index based file name.
         *
         * @return the final page [File].
         * @throws IOException when the chapter directory or output file cannot be written.
         */
        suspend fun savePage(
            mangaId: String,
            chapterId: String,
            pageIndex: Int,
            byteStream: InputStream,
            extension: String = DEFAULT_EXTENSION,
        ): File =
            withContext(ioDispatcher) {
                val chapterDir = chapterDirectory(mangaId, chapterId)
                // mkdirs() trả false khi thread khác vừa tạo dir cùng lúc (TOCTOU race, issue #92)
                // → chỉ throw khi dir thật sự không tồn tại sau khi mkdirs thất bại.
                if (!chapterDir.mkdirs() && !chapterDir.isDirectory) {
                    throw IOException("Cannot create offline chapter directory: ${chapterDir.absolutePath}")
                }

                val pageFile = File(chapterDir, pageFileName(pageIndex, extension))
                val tempFile = File(chapterDir, "${pageFile.name}.tmp")

                Timber.d(
                    "savePage start: mangaId=%s chapterId=%s pageIndex=%d file=%s",
                    mangaId,
                    chapterId,
                    pageIndex,
                    pageFile.absolutePath,
                )

                try {
                    byteStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (pageFile.exists() && !pageFile.delete()) {
                        throw IOException("Cannot replace offline page: ${pageFile.absolutePath}")
                    }
                    if (!tempFile.renameTo(pageFile)) {
                        throw IOException("Cannot move temp page into place: ${pageFile.absolutePath}")
                    }
                    Timber.d(
                        "savePage end: chapterId=%s pageIndex=%d bytes=%d",
                        chapterId,
                        pageIndex,
                        pageFile.length(),
                    )
                    pageFile
                } catch (e: Exception) {
                    tempFile.delete()
                    Timber.e(e, "savePage failed: mangaId=%s chapterId=%s pageIndex=%d", mangaId, chapterId, pageIndex)
                    throw e
                }
            }

        /**
         * Returns downloaded page files for [chapterId], sorted by page index.
         */
        suspend fun getChapterPages(
            mangaId: String,
            chapterId: String,
        ): List<File> =
            withContext(ioDispatcher) {
                val chapterDir = chapterDirectory(mangaId, chapterId)
                val pages = getValidPageFiles(chapterDir)

                Timber.d("getChapterPages: mangaId=%s chapterId=%s pages=%d", mangaId, chapterId, pages.size)
                pages
            }

        /**
         * Verifies that [chapterId] is a completed physical download.
         *
         * A directory with partial pages but no completion marker is intentionally invalid.
         */
        suspend fun verifyDownloadedChapter(
            mangaId: String,
            chapterId: String,
        ): Boolean =
            withContext(ioDispatcher) {
                val chapterDir = chapterDirectory(mangaId, chapterId)
                val pages = getValidPageFiles(chapterDir)
                val marker = File(chapterDir, COMPLETION_MARKER)

                var isValid = false
                if (marker.isFile && pages.isNotEmpty()) {
                    val content = runCatching { marker.readText().trim() }.getOrNull()
                    val expectedPages = content?.toIntOrNull()

                    isValid = if (expectedPages != null) {
                        pages.size == expectedPages && pageIndexFromName(pages.last().name) == pages.size - 1
                    } else {
                        pageIndexFromName(pages.last().name) == pages.size - 1
                    }
                }

                Timber.d(
                    "verifyDownloadedChapter: mangaId=%s chapterId=%s downloaded=%s",
                    mangaId,
                    chapterId,
                    isValid,
                )
                isValid
            }

        /**
         * Writes the marker used to distinguish a completed download from leftover partial pages.
         */
        suspend fun markChapterComplete(
            mangaId: String,
            chapterId: String,
            totalPages: Int,
        ) = withContext(ioDispatcher) {
            val chapterDir = chapterDirectory(mangaId, chapterId)
            if (getValidPageFiles(chapterDir).isEmpty()) {
                throw IOException("Cannot complete offline chapter without pages: ${chapterDir.absolutePath}")
            }
            writeCompletionMarker(chapterDir, totalPages)
            Timber.d("markChapterComplete: mangaId=%s chapterId=%s totalPages=%d", mangaId, chapterId, totalPages)
        }

        /**
         * Returns chapter ids whose directories contain both valid pages and a completion marker.
         */
        suspend fun scanDownloadedChapters(): Set<String> =
            withContext(ioDispatcher) {
                val downloadedIds =
                    rootDirectory()
                        .listFiles { file -> file.isDirectory }
                        .orEmpty()
                        .flatMap { mangaDir -> mangaDir.listFiles { file -> file.isDirectory }.orEmpty().asList() }
                        .filter { chapterDir -> isCompletedChapterDir(chapterDir) }
                        .mapTo(linkedSetOf()) { chapterDir -> chapterDir.name }
                Timber.d("scanDownloadedChapters: count=%d", downloadedIds.size)
                downloadedIds
            }

        /**
         * Liệt kê chapter đã tải hoàn chỉnh kèm mangaId (từ dir cha) và dung lượng —
         * cho màn Downloads. Filesystem là nguồn sự thật về "đã tải".
         */
        suspend fun listDownloadedChapters(): List<DownloadedChapterDirInfo> =
            withContext(ioDispatcher) {
                val result =
                    rootDirectory()
                        .listFiles { file -> file.isDirectory }
                        .orEmpty()
                        .flatMap { mangaDir ->
                            mangaDir
                                .listFiles { file -> file.isDirectory }
                                .orEmpty()
                                .filter { chapterDir -> isCompletedChapterDir(chapterDir) }
                                .map { chapterDir ->
                                    DownloadedChapterDirInfo(
                                        mangaId = mangaDir.name,
                                        chapterId = chapterDir.name,
                                        sizeBytes = chapterDir.listFiles().orEmpty().sumOf { it.length() },
                                    )
                                }
                        }
                Timber.d("listDownloadedChapters: count=%d", result.size)
                result
            }

        private fun isCompletedChapterDir(chapterDir: File): Boolean {
            val pages = getValidPageFiles(chapterDir)
            val marker = File(chapterDir, COMPLETION_MARKER)
            if (!marker.isFile || pages.isEmpty()) return false
            val expected = runCatching { marker.readText().trim().toInt() }.getOrNull()
            return if (expected != null) {
                pages.size == expected && pageIndexFromName(pages.last().name) == pages.size - 1
            } else {
                pageIndexFromName(pages.last().name) == pages.size - 1
            }
        }

        /**
         * Scans for chapters that have a completion marker but fail validity checks (e.g. missing pages).
         */
        suspend fun scanCorruptedChapters(): List<Pair<String, String>> =
            withContext(ioDispatcher) {
                val corrupted = mutableListOf<Pair<String, String>>()
                rootDirectory()
                    .listFiles { file -> file.isDirectory }
                    .orEmpty()
                    .forEach { mangaDir ->
                        val mangaId = mangaDir.name
                        mangaDir.listFiles { file -> file.isDirectory }.orEmpty().forEach { chapterDir ->
                            val marker = File(chapterDir, COMPLETION_MARKER)
                            if (marker.isFile && !isCompletedChapterDir(chapterDir)) {
                                corrupted.add(Pair(mangaId, chapterDir.name))
                            }
                        }
                    }
                Timber.d("scanCorruptedChapters: count=%d", corrupted.size)
                corrupted
            }

        /**
         * Returns a valid page file if it already exists, or null otherwise.
         */
        suspend fun getPageFileIfExists(mangaId: String, chapterId: String, pageIndex: Int): File? =
            withContext(ioDispatcher) {
                val chapterDir = chapterDirectory(mangaId, chapterId)
                if (!chapterDir.exists()) return@withContext null

                val pages = getValidPageFiles(chapterDir)
                pages.firstOrNull { pageIndexFromName(it.name) == pageIndex && it.length() > 0 }
            }

        /**
         * Adds completion markers for downloads created before filesystem tracking existed.
         */
        suspend fun backfillCompletionMarkers(legacyDownloadedIds: Set<String>): Int =
            withContext(ioDispatcher) {
                if (legacyDownloadedIds.isEmpty()) return@withContext 0

                var createdMarkers = 0
                rootDirectory()
                    .listFiles { file -> file.isDirectory }
                    .orEmpty()
                    .flatMap { mangaDir -> mangaDir.listFiles { file -> file.isDirectory }.orEmpty().asList() }
                    .filter { chapterDir ->
                        val pages = getValidPageFiles(chapterDir)
                        chapterDir.name in legacyDownloadedIds &&
                            pages.isNotEmpty() &&
                            pageIndexFromName(pages.last().name) == pages.size - 1 &&
                            !File(chapterDir, COMPLETION_MARKER).exists()
                    }.forEach { chapterDir ->
                        val pages = getValidPageFiles(chapterDir)
                        writeCompletionMarker(chapterDir, pages.size)
                        createdMarkers += 1
                    }
                Timber.d("backfillCompletionMarkers: legacy=%d created=%d", legacyDownloadedIds.size, createdMarkers)
                createdMarkers
            }

        /**
         * Deletes all downloaded pages for [chapterId].
         */
        suspend fun deleteChapter(
            mangaId: String,
            chapterId: String,
        ) = withContext(ioDispatcher) {
            val chapterDir = chapterDirectory(mangaId, chapterId)
            if (!chapterDir.exists()) {
                Timber.d("deleteChapter skipped: mangaId=%s chapterId=%s missing=true", mangaId, chapterId)
                return@withContext
            }

            Timber.d("deleteChapter start: mangaId=%s chapterId=%s dir=%s", mangaId, chapterId, chapterDir.absolutePath)
            if (!chapterDir.deleteRecursively()) {
                throw IOException("Cannot delete offline chapter directory: ${chapterDir.absolutePath}")
            }
            // Dọn dir manga cha khi không còn chapter nào — tránh tích rác dir rỗng.
            val mangaDir = chapterDir.parentFile
            if (mangaDir != null && mangaDir.listFiles().isNullOrEmpty()) {
                mangaDir.delete()
            }
            Timber.d("deleteChapter end: mangaId=%s chapterId=%s", mangaId, chapterId)
        }

        private fun chapterDirectory(
            mangaId: String,
            chapterId: String,
        ): File = File(File(rootDirectory(), safeSegment(mangaId)), safeSegment(chapterId))

        private fun rootDirectory(): File = File(context.filesDir, ROOT_DIRECTORY)

        private fun getValidPageFiles(chapterDir: File): List<File> =
            chapterDir
                .listFiles { file ->
                    file.isFile &&
                        file.name.startsWith(PAGE_PREFIX) &&
                        !file.name.endsWith(TEMP_SUFFIX)
                }
                ?.sortedBy { file -> pageIndexFromName(file.name) }
                .orEmpty()

        private fun writeCompletionMarker(chapterDir: File, totalPages: Int) {
            val marker = File(chapterDir, COMPLETION_MARKER)
            if (!marker.exists()) {
                marker.createNewFile()
            }
            marker.writeText(totalPages.toString())
        }

        private fun pageFileName(
            pageIndex: Int,
            extension: String,
        ): String {
            val safeExtension =
                extension
                    .trim()
                    .trimStart('.')
                    .lowercase()
                    .replace(Regex("[^a-z0-9]"), "")
                    .ifBlank { DEFAULT_EXTENSION }
            return "$PAGE_PREFIX${pageIndex.coerceAtLeast(0).toString().padStart(PAGE_INDEX_WIDTH, '0')}.$safeExtension"
        }

        private fun pageIndexFromName(name: String): Int =
            name
                .substringAfter(PAGE_PREFIX, missingDelimiterValue = "")
                .substringBefore(".")
                .toIntOrNull()
                ?: Int.MAX_VALUE

        private fun safeSegment(raw: String): String =
            raw
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .ifBlank { UNKNOWN_SEGMENT }

        private companion object {
            const val ROOT_DIRECTORY = "offline_manga"
            const val PAGE_PREFIX = "page_"
            const val PAGE_INDEX_WIDTH = 5
            const val DEFAULT_EXTENSION = "img"
            const val TEMP_SUFFIX = ".tmp"
            const val COMPLETION_MARKER = ".complete"
            const val UNKNOWN_SEGMENT = "unknown"
        }
    }

/**
 * Một chapter đã tải hoàn chỉnh trên filesystem — kết quả của
 * [OfflineDownloadStorage.listDownloadedChapters].
 */
data class DownloadedChapterDirInfo(
    val mangaId: String,
    val chapterId: String,
    val sizeBytes: Long,
)
