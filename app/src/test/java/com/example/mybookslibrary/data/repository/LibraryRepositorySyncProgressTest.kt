package com.example.mybookslibrary.data.repository

import androidx.room.Room
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.remote.FirestoreDataSource
import com.example.mybookslibrary.data.remote.models.FirestoreChapterProgress
import com.example.mybookslibrary.data.remote.models.FirestoreLibraryItem
import com.example.mybookslibrary.domain.model.SyncStatus
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LibraryRepositorySyncProgressTest {
    private lateinit var database: AppDatabase
    private lateinit var firestoreDataSource: FirestoreDataSource
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        firestoreDataSource = mockk(relaxed = true)
        val user =
            mockk<FirebaseUser> {
                every { uid } returns USER_ID
            }
        val authRepository =
            mockk<AuthRepository>(relaxed = true) {
                every { getCurrentUser() } returns user
            }
        repository =
            LibraryRepository(
                libraryDao = database.libraryDao(),
                chapterDao = database.chapterDao(),
                database = database,
                firestoreDataSource = firestoreDataSource,
                authRepository = authRepository,
                externalScope = TestScope(),
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun performSync_remoteOnlyProgress_downloadsEveryChapterProgress() =
        runTest {
            insertSyncedManga()
            coEvery { firestoreDataSource.getAllProgress(USER_ID) } returns
                listOf(
                    remoteProgress(CHAPTER_1, lastReadPage = 4, updatedAt = 1000L),
                    remoteProgress(CHAPTER_2, lastReadPage = 8, updatedAt = 2000L),
                )

            repository.performSync()

            val first = database.chapterDao().getChapterProgressByChapter(CHAPTER_1)
            val second = database.chapterDao().getChapterProgressByChapter(CHAPTER_2)
            assertNotNull(first)
            assertNotNull(second)
            assertEquals(4, first!!.last_read_page)
            assertEquals(1000L, first.updated_at)
            assertEquals(8, second!!.last_read_page)
            assertEquals(2000L, second.updated_at)
        }

    @Test
    fun performSync_newerLocalProgress_uploadsEveryChapterProgress() =
        runTest {
            insertSyncedManga()
            database.chapterDao().upsertChapterProgress(localProgress(CHAPTER_1, lastReadPage = 4, updatedAt = 2000L))
            database.chapterDao().upsertChapterProgress(localProgress(CHAPTER_2, lastReadPage = 8, updatedAt = 3000L))
            coEvery { firestoreDataSource.getAllProgress(USER_ID) } returns
                listOf(remoteProgress(CHAPTER_1, lastReadPage = 1, updatedAt = 1000L))

            repository.performSync()

            coVerify(exactly = 1) {
                firestoreDataSource.saveProgressList(
                    USER_ID,
                    match { progress ->
                        progress.map { it.chapterId }.toSet() == setOf(CHAPTER_1, CHAPTER_2) &&
                            progress.associate { it.chapterId to it.lastReadPage } ==
                            mapOf(CHAPTER_1 to 4, CHAPTER_2 to 8)
                    },
                )
            }
        }

    @Test
    fun performSync_newerRemoteProgress_preservesLocalDownloadedFlag() =
        runTest {
            insertSyncedManga()
            database.chapterDao().upsertChapterProgress(
                localProgress(CHAPTER_1, lastReadPage = 3, updatedAt = 1000L).copy(is_downloaded = true),
            )
            coEvery { firestoreDataSource.getAllProgress(USER_ID) } returns
                listOf(
                    remoteProgress(
                        chapterId = CHAPTER_1,
                        lastReadPage = 20,
                        updatedAt = 2000L,
                        status = "COMPLETED",
                    ),
                )

            repository.performSync()

            val progress = database.chapterDao().getChapterProgressByChapter(CHAPTER_1)!!
            assertEquals(ChapterStatus.COMPLETED, progress.status)
            assertEquals(20, progress.last_read_page)
            assertEquals(2000L, progress.updated_at)
            assertEquals(true, progress.is_downloaded)
        }

    private suspend fun insertSyncedManga() {
        database.libraryDao().upsert(
            LibraryItemEntity(
                manga_id = MANGA_ID,
                title = "Title",
                cover_url = "",
                status = LibraryStatus.READING,
                updated_at = 1L,
                syncStatus = SyncStatus.SYNCED,
            ),
        )
        coEvery { firestoreDataSource.getAllItems(USER_ID) } returns
            listOf(
                FirestoreLibraryItem(
                    mangaId = MANGA_ID,
                    title = "Title",
                    coverUrl = "",
                    status = "READING",
                    addedAt = 1L,
                    lastReadAt = 1L,
                    updatedAt = 1L,
                ),
            )
    }

    private fun localProgress(
        chapterId: String,
        lastReadPage: Int,
        updatedAt: Long,
    ) = ChapterProgressEntity(
        chapter_id = chapterId,
        manga_id = MANGA_ID,
        status = ChapterStatus.READING,
        last_read_page = lastReadPage,
        total_pages = 20,
        updated_at = updatedAt,
    )

    private fun remoteProgress(
        chapterId: String,
        lastReadPage: Int,
        updatedAt: Long,
        status: String = "READING",
    ) = FirestoreChapterProgress(
        chapterId = chapterId,
        mangaId = MANGA_ID,
        status = status,
        lastReadPage = lastReadPage,
        totalPages = 20,
        updatedAt = updatedAt,
    )

    private companion object {
        const val USER_ID = "user-123"
        const val MANGA_ID = "manga-1"
        const val CHAPTER_1 = "chapter-1"
        const val CHAPTER_2 = "chapter-2"
    }
}
