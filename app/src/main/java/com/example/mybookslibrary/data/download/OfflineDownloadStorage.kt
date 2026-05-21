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
class OfflineDownloadStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
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
        extension: String = DEFAULT_EXTENSION
    ): File = withContext(ioDispatcher) {
        val chapterDir = chapterDirectory(mangaId, chapterId)
        if (!chapterDir.exists() && !chapterDir.mkdirs()) {
            throw IOException("Cannot create offline chapter directory: ${chapterDir.absolutePath}")
        }

        val pageFile = File(chapterDir, pageFileName(pageIndex, extension))
        val tempFile = File(chapterDir, "${pageFile.name}.tmp")

        Timber.d(
            "savePage start: mangaId=%s chapterId=%s pageIndex=%d file=%s",
            mangaId,
            chapterId,
            pageIndex,
            pageFile.absolutePath
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
            Timber.d("savePage end: chapterId=%s pageIndex=%d bytes=%d", chapterId, pageIndex, pageFile.length())
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
    suspend fun getChapterPages(mangaId: String, chapterId: String): List<File> = withContext(ioDispatcher) {
        val chapterDir = chapterDirectory(mangaId, chapterId)
        val pages = chapterDir
            .listFiles { file -> file.isFile && file.name.startsWith(PAGE_PREFIX) && !file.name.endsWith(TEMP_SUFFIX) }
            ?.sortedBy { file -> pageIndexFromName(file.name) }
            .orEmpty()

        Timber.d("getChapterPages: mangaId=%s chapterId=%s pages=%d", mangaId, chapterId, pages.size)
        pages
    }

    /**
     * Deletes all downloaded pages for [chapterId].
     */
    suspend fun deleteChapter(mangaId: String, chapterId: String) = withContext(ioDispatcher) {
        val chapterDir = chapterDirectory(mangaId, chapterId)
        if (!chapterDir.exists()) {
            Timber.d("deleteChapter skipped: mangaId=%s chapterId=%s missing=true", mangaId, chapterId)
            return@withContext
        }

        Timber.d("deleteChapter start: mangaId=%s chapterId=%s dir=%s", mangaId, chapterId, chapterDir.absolutePath)
        if (!chapterDir.deleteRecursively()) {
            throw IOException("Cannot delete offline chapter directory: ${chapterDir.absolutePath}")
        }
        Timber.d("deleteChapter end: mangaId=%s chapterId=%s", mangaId, chapterId)
    }

    private fun chapterDirectory(mangaId: String, chapterId: String): File {
        return File(File(File(context.filesDir, ROOT_DIRECTORY), safeSegment(mangaId)), safeSegment(chapterId))
    }

    private fun pageFileName(pageIndex: Int, extension: String): String {
        val safeExtension = extension
            .trim()
            .trimStart('.')
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .ifBlank { DEFAULT_EXTENSION }
        return "$PAGE_PREFIX${pageIndex.coerceAtLeast(0).toString().padStart(PAGE_INDEX_WIDTH, '0')}.$safeExtension"
    }

    private fun pageIndexFromName(name: String): Int {
        return name
            .substringAfter(PAGE_PREFIX, missingDelimiterValue = "")
            .substringBefore(".")
            .toIntOrNull()
            ?: Int.MAX_VALUE
    }

    private fun safeSegment(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { UNKNOWN_SEGMENT }
    }

    private companion object {
        const val ROOT_DIRECTORY = "offline_manga"
        const val PAGE_PREFIX = "page_"
        const val PAGE_INDEX_WIDTH = 5
        const val DEFAULT_EXTENSION = "img"
        const val TEMP_SUFFIX = ".tmp"
        const val UNKNOWN_SEGMENT = "unknown"
    }
}
