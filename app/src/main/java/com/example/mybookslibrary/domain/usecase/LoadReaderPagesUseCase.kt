package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import timber.log.Timber
import javax.inject.Inject

class LoadReaderPagesUseCase
@Inject
constructor(
    private val mangaRepository: MangaRepository,
    private val downloadedChapterCache: DownloadedChapterCache,
    private val offlineDownloadStorage: OfflineDownloadStorage,
    private val offlineDownloadRepository: OfflineDownloadRepository,
) {
    /**
     * Routes verified physical downloads to local files before considering MangaDex@Home.
     */
    suspend operator fun invoke(mangaId: String, chapterId: String,): Result<List<String>> = runCatching {
        val wasCachedAsDownloaded = chapterId in downloadedChapterCache.downloadedChapterIds.value
        val isPhysicallyDownloaded = offlineDownloadStorage.verifyDownloadedChapter(mangaId, chapterId)

        if (isPhysicallyDownloaded) {
            if (!wasCachedAsDownloaded) {
                downloadedChapterCache.addChapter(chapterId)
            }
            val localPages = offlineDownloadStorage.getChapterPages(mangaId, chapterId)
            if (localPages.isNotEmpty()) {
                return@runCatching localPages.map { it.toURI().toString() }
            }
        }

        if (wasCachedAsDownloaded || isPhysicallyDownloaded) {
            offlineDownloadRepository.markChapterCorrupted(mangaId, chapterId)
            Timber.w(
                "LoadReaderPagesUseCase physical download invalid, fallback network: mangaId=%s chapterId=%s",
                mangaId,
                chapterId,
            )
        }

        mangaRepository.getChapterPages(chapterId).getOrThrow()
    }
}
