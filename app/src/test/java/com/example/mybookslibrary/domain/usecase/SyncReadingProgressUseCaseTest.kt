package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.repository.LibraryRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncReadingProgressUseCaseTest {
    private val libraryRepository = mockk<LibraryRepository>(relaxed = true)
    private val useCase = SyncReadingProgressUseCase(libraryRepository)

    @Test
    fun invoke_delegatesToLibraryRepository() =
        runTest {
            useCase(
                mangaId = "manga-1",
                chapterId = "chapter-1",
                pageIndex = 4,
                totalPages = 8,
            )

            coVerify {
                libraryRepository.updateReadingProgress(
                    mangaId = "manga-1",
                    chapterId = "chapter-1",
                    pageIndex = 4,
                    totalPages = 8,
                )
            }
        }
}
