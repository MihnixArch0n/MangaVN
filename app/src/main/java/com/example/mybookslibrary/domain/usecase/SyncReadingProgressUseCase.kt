package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.repository.LibraryRepository
import javax.inject.Inject

class SyncReadingProgressUseCase
@Inject
constructor(private val libraryRepository: LibraryRepository,) {
    /**
     * Persists the reader's current page and chapter page count.
     */
    suspend operator fun invoke(mangaId: String, chapterId: String, pageIndex: Int, totalPages: Int,) {
        libraryRepository.updateReadingProgress(
            mangaId = mangaId,
            chapterId = chapterId,
            pageIndex = pageIndex,
            totalPages = totalPages,
        )
    }
}
