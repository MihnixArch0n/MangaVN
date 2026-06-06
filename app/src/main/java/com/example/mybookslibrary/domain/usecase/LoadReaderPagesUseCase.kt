package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.MangaRepository
import timber.log.Timber
import javax.inject.Inject

class LoadReaderPagesUseCase
@Inject
constructor(
    private val mangaRepository: MangaRepository,
    private val downloadedChapterCache: DownloadedChapterCache,
    private val offlineDownloadStorage: OfflineDownloadStorage,
) {
    /**
     * Loads local chapter pages when available, otherwise falls back to the network source.
     */
    suspend operator fun invoke(mangaId: String, chapterId: String,): Result<List<String>> = runCatching {
        val isDownloaded = downloadedChapterCache.isChapterDownloaded(chapterId)
        val localPages =
            if (isDownloaded) {
                offlineDownloadStorage.getChapterPages(mangaId, chapterId)
            } else {
                emptyList()
            }

        if (localPages.isNotEmpty()) {
            return@runCatching localPages.map { it.toURI().toString() }
        }

        if (isDownloaded) {
            Timber.w(
                "LoadReaderPagesUseCase local missing, fallback network: mangaId=%s chapterId=%s",
                mangaId,
                chapterId,
            )
        }

        mangaRepository.getChapterPages(chapterId).getOrThrow()
    }
}
