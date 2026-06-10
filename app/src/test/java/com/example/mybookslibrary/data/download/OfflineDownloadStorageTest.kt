package com.example.mybookslibrary.data.download

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDownloadStorageTest {
    @Test
    fun scanDownloadedChapters_requiresCompletionMarkerAndPage() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                assertFalse(CHAPTER_ID in storage.scanDownloadedChapters())

                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                assertTrue(CHAPTER_ID in storage.scanDownloadedChapters())
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun verifyDownloadedChapter_requiresCompletionMarkerAndPage() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                assertTrue(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun backfillCompletionMarkers_marksLegacyDirectoryOnce() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, LEGACY_CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())

                assertEquals(1, storage.backfillCompletionMarkers(setOf(LEGACY_CHAPTER_ID)))
                assertEquals(0, storage.backfillCompletionMarkers(setOf(LEGACY_CHAPTER_ID)))
                assertTrue(LEGACY_CHAPTER_ID in storage.scanDownloadedChapters())
            } finally {
                storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)
            }
        }

    @Test
    fun scanCorruptedChapters_findsChaptersWithMissingPages() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                // Not corrupted if nothing is downloaded
                assertTrue(storage.scanCorruptedChapters().isEmpty())

                // Download 2 pages, mark as complete (total = 2)
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                val page1 = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 1, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 2)
                
                // Not corrupted, perfectly valid
                assertTrue(storage.scanCorruptedChapters().isEmpty())

                // Simulate external deletion of page 1
                page1.delete()
                
                // Now it should be considered corrupted
                val corrupted = storage.scanCorruptedChapters()
                assertEquals(1, corrupted.size)
                assertEquals(Pair(MANGA_ID, CHAPTER_ID), corrupted[0])
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun getPageFileIfExists_returnsFileIfValid() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                val file = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                
                assertEquals(file.absolutePath, storage.getPageFileIfExists(MANGA_ID, CHAPTER_ID, 0)?.absolutePath)
                assertEquals(null, storage.getPageFileIfExists(MANGA_ID, CHAPTER_ID, 1))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    private fun storage(): OfflineDownloadStorage =
        OfflineDownloadStorage(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    private fun pageBytes() = ByteArrayInputStream(byteArrayOf(1, 2, 3))

    private companion object {
        const val MANGA_ID = "storage-test-manga"
        const val CHAPTER_ID = "storage-test-chapter"
        const val LEGACY_CHAPTER_ID = "storage-test-legacy-chapter"
    }
}
