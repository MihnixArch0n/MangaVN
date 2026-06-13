package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineChapterScanResult
import com.example.mybookslibrary.data.local.ChapterMetadataEntity
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import com.example.mybookslibrary.domain.model.ChapterDownloadStatus
import com.example.mybookslibrary.domain.model.ChapterModel
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class GetChapterListWithProgressUseCaseTest {
    private val mangaRepository = mockk<MangaRepository>()
    private val chapterDao = mockk<ChapterDao>()
    private val offlineDownloadRepository = mockk<OfflineDownloadRepository>()
    private val downloadedChapterCache = mockk<DownloadedChapterCache>()
    private val userPreferencesDataStore = mockk<UserPreferencesDataStore>()
    private val useCase =
        GetChapterListWithProgressUseCase(
            mangaRepository,
            chapterDao,
            offlineDownloadRepository,
            downloadedChapterCache,
            userPreferencesDataStore,
        )

    private fun stubCommon(
        metadata: List<ChapterMetadataEntity>,
        progress: List<ChapterProgressEntity> = emptyList(),
        downloadedIds: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet()),
    ) {
        every { chapterDao.getChaptersByMangaIdFlow(MANGA_ID) } returns flowOf(metadata)
        every { chapterDao.getChapterProgressByManga(MANGA_ID) } returns flowOf(progress)
        every { offlineDownloadRepository.observeQueueByManga(MANGA_ID) } returns flowOf(emptyList())
        every { downloadedChapterCache.downloadedChapterIds } returns downloadedIds
        every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("")
        coEvery {
            downloadedChapterCache.scanFilesystem()
        } returns OfflineChapterScanResult(downloadedIds.value, emptyList())
        coEvery { chapterDao.syncChapterMetadata(any(), any(), any()) } returns Unit
    }

    @Test
    fun mapsProgressByChapterId_independently() =
        runTest {
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns
                Result.success(
                    listOf(
                        chapter(id = "chapter-1", pages = 12),
                        chapter(id = "chapter-2", pages = 10),
                        chapter(id = "chapter-3", pages = 8),
                    ),
                )
            stubCommon(
                metadata = listOf(metadata("chapter-1", 12), metadata("chapter-2", 10), metadata("chapter-3", 8)),
                progress =
                    listOf(
                        progress("chapter-1", ChapterStatus.READING, lastReadPage = 4, totalPages = 12),
                        progress("chapter-2", ChapterStatus.COMPLETED, lastReadPage = 9, totalPages = 10),
                    ),
                )

            val result = useCase(MANGA_ID).first().chapters

            assertEquals(3, result.size)

            assertEquals("chapter-1", result[0].chapterId)
            assertEquals(ChapterReadingStatus.READING, result[0].status)
            assertEquals(4, result[0].lastReadPage)
            assertEquals(12, result[0].totalPages)

            assertEquals("chapter-2", result[1].chapterId)
            assertEquals(ChapterReadingStatus.COMPLETED, result[1].status)
            assertEquals(9, result[1].lastReadPage)
            assertEquals(10, result[1].totalPages)

            assertEquals("chapter-3", result[2].chapterId)
            assertEquals(ChapterReadingStatus.UNREAD, result[2].status)
            assertEquals(0, result[2].lastReadPage)
            assertEquals(8, result[2].totalPages)
        }

    @Test
    fun usesProgressTotalPagesBeforeRemotePages() =
        runTest {
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns
                Result.success(
                    listOf(chapter(id = "chapter-1", pages = 99)),
                )
            stubCommon(
                metadata = listOf(metadata("chapter-1", 99)),
                progress = listOf(progress("chapter-1", ChapterStatus.READING, lastReadPage = 3, totalPages = 15)),
            )

            val result = useCase(MANGA_ID).first().chapters

            assertEquals(15, result.single().totalPages)
        }

    @Test
    fun mapsDownloadedCacheToChapterDownloadState() =
        runTest {
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns
                Result.success(
                    listOf(chapter(id = "chapter-1", pages = 12)),
                )
            stubCommon(
                metadata = listOf(metadata("chapter-1", 12)),
                downloadedIds = MutableStateFlow(setOf("chapter-1")),
            )

            val result = useCase(MANGA_ID).first().chapters

            assertEquals(ChapterDownloadStatus.DOWNLOADED, result.single().downloadState.status)
            assertEquals(100, result.single().downloadState.progressPercent)
        }

    @Test
    fun emitsDownloadedStateWhenFilesystemCacheChanges() =
        runTest {
            val downloadedIds = MutableStateFlow<Set<String>>(emptySet())
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns
                Result.success(
                    listOf(chapter(id = "chapter-1", pages = 12)),
                )
            stubCommon(metadata = listOf(metadata("chapter-1", 12)), downloadedIds = downloadedIds)

            val firstEmission = CompletableDeferred<Unit>()
            val emissions =
                async {
                    useCase(MANGA_ID)
                        .onEach { firstEmission.complete(Unit) }
                        .take(2)
                        .toList()
                }
            firstEmission.await()
            downloadedIds.value = setOf("chapter-1")

            val result = emissions.await()
            assertEquals(ChapterDownloadStatus.NOT_DOWNLOADED, result[0].chapters.single().downloadState.status)
            assertEquals(ChapterDownloadStatus.DOWNLOADED, result[1].chapters.single().downloadState.status)
        }

    @Test
    fun completedQueueWithoutFilesystemMarker_isNotDownloaded() =
        runTest {
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns
                Result.success(
                    listOf(chapter(id = "chapter-1", pages = 12)),
                )
            stubCommon(metadata = listOf(metadata("chapter-1", 12)))
            every { offlineDownloadRepository.observeQueueByManga(MANGA_ID) } returns
                flowOf(
                    listOf(
                        DownloadQueueEntity(
                            chapter_id = "chapter-1",
                            manga_id = MANGA_ID,
                            status = DownloadStatus.COMPLETED,
                            progress_percent = 100,
                        ),
                    ),
                )
            val result = useCase(MANGA_ID).first().chapters

            assertEquals(ChapterDownloadStatus.NOT_DOWNLOADED, result.single().downloadState.status)
            assertEquals(0, result.single().downloadState.progressPercent)
        }

    @Test
    fun feedFailure_cachedChaptersStillEmitWithoutFlowError() =
        runTest {
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.failure(IOException("offline"))
            stubCommon(metadata = listOf(metadata("chapter-1", 12)))

            val result = useCase(MANGA_ID).first().chapters

            assertEquals("chapter-1", result.single().chapterId)
        }

    @Test
    fun downloadedProgressWithoutMetadata_stillEmitsOfflineFallback() =
        runTest {
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.failure(IOException("offline"))
            stubCommon(
                metadata = emptyList(),
                progress = listOf(progress("chapter-1", ChapterStatus.READING, lastReadPage = 3, totalPages = 12)),
                downloadedIds = MutableStateFlow(setOf("chapter-1")),
            )

            val result = useCase(MANGA_ID).first().chapters.single()

            assertEquals("chapter-1", result.chapterId)
            assertEquals(ChapterDownloadStatus.DOWNLOADED, result.downloadState.status)
            assertEquals(3, result.lastReadPage)
        }

    @Test
    fun filesystemRescanRemovingGhost_emitsNotDownloaded() =
        runTest {
            val downloadedIds = MutableStateFlow(setOf("chapter-1"))
            coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.failure(IOException("offline"))
            coEvery { downloadedChapterCache.scanFilesystem() } answers {
                downloadedIds.value = emptySet()
                OfflineChapterScanResult(emptySet(), emptyList())
            }
            every { chapterDao.getChaptersByMangaIdFlow(MANGA_ID) } returns flowOf(listOf(metadata("chapter-1", 12)))
            every { chapterDao.getChapterProgressByManga(MANGA_ID) } returns flowOf(emptyList())
            every { offlineDownloadRepository.observeQueueByManga(MANGA_ID) } returns flowOf(emptyList())
            every { downloadedChapterCache.downloadedChapterIds } returns downloadedIds
            every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("")

            val result = useCase(MANGA_ID).first().chapters.single()

            assertEquals(ChapterDownloadStatus.NOT_DOWNLOADED, result.downloadState.status)
        }

    @Test
    fun fallbackToEnglishIfPreferredLanguageNotFound() = runTest {
        coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.success(listOf(
            chapter("c1", 10, "vi"),
            chapter("c2", 10, "en")
        ))
        stubCommon(
            metadata = listOf(metadata("c1", 10, "vi"), metadata("c2", 10, "en"))
        )
        every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("es")

        val result = useCase(MANGA_ID).first()
        assertEquals(listOf("en", "vi"), result.availableLanguages)
        assertEquals(1, result.chapters.size)
        assertEquals("c2", result.chapters[0].chapterId)
    }

    @Test
    fun fallbackToFirstAvailableLanguageIfEnglishNotFound() = runTest {
        coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.success(listOf(
            chapter("c1", 10, "vi"),
            chapter("c2", 10, "fr")
        ))
        stubCommon(
            metadata = listOf(metadata("c1", 10, "vi"), metadata("c2", 10, "fr"))
        )
        every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("es")

        val result = useCase(MANGA_ID).first()
        assertEquals(listOf("fr", "vi"), result.availableLanguages)
        assertEquals(1, result.chapters.size)
        assertEquals("c2", result.chapters[0].chapterId)
    }

    @Test
    fun noLanguageFilterSelected_returnsAllChapters() = runTest {
        coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.success(listOf(
            chapter("c1", 10, "vi"),
            chapter("c2", 10, "en")
        ))
        stubCommon(
            metadata = listOf(metadata("c1", 10, "vi"), metadata("c2", 10, "en"))
        )
        every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("")

        val result = useCase(MANGA_ID).first()
        assertEquals(listOf("en", "vi"), result.availableLanguages)
        assertEquals(2, result.chapters.size)
    }

    @Test
    fun selectedLanguageFoundInAvailableLanguages_returnsFilteredList() = runTest {
        coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns Result.success(listOf(
            chapter("c1", 10, "vi"),
            chapter("c2", 10, "en")
        ))
        stubCommon(
            metadata = listOf(metadata("c1", 10, "vi"), metadata("c2", 10, "en"))
        )
        every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("vi")

        val result = useCase(MANGA_ID).first()
        assertEquals(listOf("en", "vi"), result.availableLanguages)
        assertEquals("vi", result.selectedLanguage)
        assertEquals(1, result.chapters.size)
        assertEquals("c1", result.chapters[0].chapterId)
    }

    private fun chapter(
        id: String,
        pages: Int,
        translatedLanguage: String? = null,
    ): ChapterModel =
        ChapterModel(
            id = id,
            mangaId = MANGA_ID,
            volume = null,
            chapterNumber = id.removePrefix("chapter-"),
            title = null,
            pages = pages,
            isUnavailable = false,
            translatedLanguage = translatedLanguage,
        )

    private fun metadata(
        id: String,
        pages: Int,
        translatedLanguage: String? = null,
    ): ChapterMetadataEntity =
        ChapterMetadataEntity(
            chapterId = id,
            mangaId = MANGA_ID,
            volume = null,
            chapterNumber = id.removePrefix("chapter-"),
            title = null,
            pages = pages,
            isUnavailable = false,
            translatedLanguage = translatedLanguage,
            feedOrder = id.removePrefix("chapter-").toIntOrNull() ?: 0,
            updatedAt = 1_000L,
        )

    private fun progress(
        chapterId: String,
        status: ChapterStatus,
        lastReadPage: Int,
        totalPages: Int,
    ): ChapterProgressEntity =
        ChapterProgressEntity(
            chapter_id = chapterId,
            manga_id = MANGA_ID,
            status = status,
            last_read_page = lastReadPage,
            total_pages = totalPages,
            updated_at = 1_000L + lastReadPage,
        )

    private companion object {
        const val MANGA_ID = "manga-1"
    }
}
