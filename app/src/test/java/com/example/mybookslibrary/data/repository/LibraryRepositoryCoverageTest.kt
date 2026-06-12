@file:Suppress("ktlint")

package com.example.mybookslibrary.data.repository

import androidx.room.Room
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterMetadataEntity
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.remote.FirestoreDataSource
import com.google.firebase.auth.FirebaseUser
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Phủ các thao tác thư viện của [LibraryRepository] với Room in-memory: thêm/xóa/kiểm tra
 * tồn tại, xóa toàn bộ, observe, và đổi trạng thái đọc của chapter.
 */
@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryCoverageTest {
    private lateinit var db: AppDatabase
    private lateinit var firestoreDataSource: FirestoreDataSource
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        firestoreDataSource = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        repository =
            LibraryRepository(
                db.libraryDao(),
                db.chapterDao(),
                db,
                firestoreDataSource,
                authRepository,
                kotlinx.coroutines.test.TestScope(),
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addToLibrary_makesMangaPresentAndListed() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c", status = LibraryStatus.READING)

            assertTrue(repository.isInLibrary(MANGA_ID))
            assertEquals(listOf(MANGA_ID), repository.getAllItems().map { it.manga_id })
        }

    @Test
    fun removeFromLibrary_makesMangaAbsent() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")

            repository.removeFromLibrary(MANGA_ID)

            assertFalse(repository.isInLibrary(MANGA_ID))
        }

    @Test
    fun clearAll_emptiesLibrary() =
        runTest {
            repository.addToLibrary("m1", title = "A", coverUrl = "c")
            repository.addToLibrary("m2", title = "B", coverUrl = "c")

            repository.clearAll()

            assertTrue(repository.getAllItems().isEmpty())
        }

    @Test
    fun getLibraryItem_returnsMatchingItemOrNull() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")

            assertEquals(MANGA_ID, repository.getLibraryItem(MANGA_ID)?.manga_id)
            assertNull(repository.getLibraryItem("missing"))
        }

    @Test
    fun clearAllRemote_withSignedInUser_deletesFirestoreData() =
        runTest {
            val user = mockk<FirebaseUser>()
            every { user.uid } returns "user-1"
            every { authRepository.getCurrentUser() } returns user

            repository.clearAllRemote()

            coVerify { firestoreDataSource.deleteAllUserData("user-1") }
        }

    @Test
    fun clearAllRemote_withoutSignedInUser_doesNothing() =
        runTest {
            every { authRepository.getCurrentUser() } returns null

            repository.clearAllRemote()

            coVerify(exactly = 0) { firestoreDataSource.deleteAllUserData(any()) }
        }

    @Test
    fun observeLibraryItems_emitsAddedItems() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")

            val items = repository.observeLibraryItems().first()

            assertEquals(listOf(MANGA_ID), items.map { it.manga_id })
        }

    @Test
    fun markChapterUnread_setsUnreadStatusAndResetsPage() =
        runTest {
            db.libraryDao().upsert(LibraryItemEntity(manga_id = MANGA_ID, title = "T", cover_url = ""))
            db.chapterDao().upsertChapterProgress(
                ChapterProgressEntity(
                    chapter_id = CHAPTER_ID,
                    manga_id = MANGA_ID,
                    status = ChapterStatus.COMPLETED,
                    last_read_page = 9,
                    total_pages = 10,
                    updated_at = 1L,
                ),
            )

            repository.markChapterUnread(MANGA_ID, CHAPTER_ID, totalPages = 10)

            val progress = db.chapterDao().getChapterProgressByChapter(CHAPTER_ID)!!
            assertEquals(ChapterStatus.UNREAD, progress.status)
            assertEquals(0, progress.last_read_page)
        }

    @Test
    fun removeBookmark_deletesLibraryItemAndProgress() =
        runTest {
            db.libraryDao().upsert(LibraryItemEntity(manga_id = MANGA_ID, title = "T", cover_url = ""))
            db.chapterDao().upsertChapterProgress(
                ChapterProgressEntity(
                    chapter_id = CHAPTER_ID,
                    manga_id = MANGA_ID,
                    updated_at = 1L,
                ),
            )

            repository.removeBookmark(MANGA_ID)

            assertFalse(repository.isInLibrary(MANGA_ID))
            org.junit.Assert.assertNotNull(db.chapterDao().getChapterProgressByChapter(CHAPTER_ID))
        }

    @Test
    fun setFavorite_mangaDaCoTrongThuVien_batCoYeuThich() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")

            repository.setFavorite(MANGA_ID, title = "Manga", coverUrl = "c", isFavorite = true)

            assertTrue(repository.getLibraryItem(MANGA_ID)!!.is_favorite)
        }

    @Test
    fun setFavorite_mangaChuaCoTrongThuVien_tuThemVoiCoYeuThich() =
        runTest {
            repository.setFavorite(MANGA_ID, title = "Manga", coverUrl = "c", isFavorite = true)

            val item = repository.getLibraryItem(MANGA_ID)!!
            assertTrue(item.is_favorite)
            assertEquals("Manga", item.title)
        }

    @Test
    fun setFavorite_tatCoYeuThich_giuNguyenItemTrongThuVien() =
        runTest {
            repository.setFavorite(MANGA_ID, title = "Manga", coverUrl = "c", isFavorite = true)

            repository.setFavorite(MANGA_ID, title = "Manga", coverUrl = "c", isFavorite = false)

            val item = repository.getLibraryItem(MANGA_ID)!!
            assertFalse(item.is_favorite)
        }

    @Test
    fun updateReadingProgress_docHetTatCaChuong_statusThanhCompleted() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")
            insertMetadata(metadata("c1", chapterNumber = "1"), metadata("c2", chapterNumber = "2"))

            // Đọc hết c1 (trang cuối) — còn c2 nên vẫn READING
            repository.updateReadingProgress(MANGA_ID, "c1", pageIndex = 9, totalPages = 10)
            assertEquals(LibraryStatus.READING, repository.getLibraryItem(MANGA_ID)!!.status)

            // Đọc hết c2 — toàn bộ chương xong -> COMPLETED
            repository.updateReadingProgress(MANGA_ID, "c2", pageIndex = 9, totalPages = 10)
            assertEquals(LibraryStatus.COMPLETED, repository.getLibraryItem(MANGA_ID)!!.status)
        }

    @Test
    fun markChapterUnread_sauKhiHoanThanh_statusQuayVeReading() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")
            insertMetadata(metadata("c1", chapterNumber = "1"))
            repository.updateReadingProgress(MANGA_ID, "c1", pageIndex = 9, totalPages = 10)
            assertEquals(LibraryStatus.COMPLETED, repository.getLibraryItem(MANGA_ID)!!.status)

            repository.markChapterUnread(MANGA_ID, "c1", totalPages = 10)

            assertEquals(LibraryStatus.READING, repository.getLibraryItem(MANGA_ID)!!.status)
        }

    @Test
    fun markChapterCompleted_motBanDichXong_chuongDoTinhLaDaDoc() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")
            // Cùng chương "1" có 2 bản dịch — chỉ cần đọc 1 bản là tính xong chương
            insertMetadata(
                metadata("c1-en", chapterNumber = "1", language = "en"),
                metadata("c1-vi", chapterNumber = "1", language = "vi"),
            )

            repository.markChapterCompleted(MANGA_ID, "c1-en", totalPages = 10)

            assertEquals(LibraryStatus.COMPLETED, repository.getLibraryItem(MANGA_ID)!!.status)
        }

    @Test
    fun chuongUnavailable_khongTinhVaoDieuKienHoanThanh() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")
            insertMetadata(
                metadata("c1", chapterNumber = "1"),
                metadata("c2", chapterNumber = "2", unavailable = true),
            )

            repository.markChapterCompleted(MANGA_ID, "c1", totalPages = 10)

            assertEquals(LibraryStatus.COMPLETED, repository.getLibraryItem(MANGA_ID)!!.status)
        }

    private suspend fun insertMetadata(vararg chapters: ChapterMetadataEntity) {
        db.chapterDao().syncChapterMetadata(MANGA_ID, chapters.toList(), emptySet())
    }

    private fun metadata(
        chapterId: String,
        chapterNumber: String?,
        language: String = "en",
        unavailable: Boolean = false,
    ) = ChapterMetadataEntity(
        chapterId = chapterId,
        mangaId = MANGA_ID,
        volume = null,
        chapterNumber = chapterNumber,
        title = null,
        pages = 10,
        isUnavailable = unavailable,
        translatedLanguage = language,
        feedOrder = 0,
        updatedAt = 1L,
    )

    @Test
    fun restoreItems_insertsAllItems() =
        runTest {
            val items =
                listOf(
                    LibraryItemEntity(manga_id = "m1", title = "T1", cover_url = ""),
                    LibraryItemEntity(manga_id = "m2", title = "T2", cover_url = ""),
                )

            repository.restoreItems(items)

            assertTrue(repository.isInLibrary("m1"))
            assertTrue(repository.isInLibrary("m2"))
        }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
