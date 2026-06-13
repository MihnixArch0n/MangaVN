@file:Suppress("ktlint", "MaxLineLength")

package com.example.mybookslibrary.ui.viewmodel

import coil3.ImageLoader
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.remote.NetworkModule
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(coil3.annotation.ExperimentalCoilApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefs = mockk<UserPreferencesDataStore>(relaxed = true)
    private val libraryRepository = mockk<LibraryRepository>(relaxed = true)
    private val authRepository = mockk<com.example.mybookslibrary.data.repository.AuthRepository>(relaxed = true)
    private val imageLoader = mockk<ImageLoader>(relaxed = true)

    @Suppress("LongParameterList") // helper test gom nhiều pref mặc định — dễ đọc hơn là tách nhỏ
    private fun stubDefaults(
        quality: String = "data",
        theme: String = "system",
        language: String = "en",
        keepScreenOn: Boolean = false,
        volumeKeyNav: Boolean = false,
        autoAdvance: Boolean = false,
        skipReadChapters: Boolean = false,
        autoDownloadNext: Boolean = false,
        newChapterNotifications: Boolean = false,
        deleteAfterRead: Boolean = false,
        deleteAfterReadKeep: Int = 2,
    ) {
        coEvery { prefs.getReaderQuality() } returns quality
        coEvery { prefs.getThemeMode() } returns theme
        coEvery { prefs.getLanguage() } returns language
        coEvery { prefs.getReaderKeepScreenOn() } returns keepScreenOn
        coEvery { prefs.getReaderVolumeKeyNav() } returns volumeKeyNav
        coEvery { prefs.getReaderAutoAdvance() } returns autoAdvance
        coEvery { prefs.getSkipReadChapters() } returns skipReadChapters
        coEvery { prefs.getAutoDownloadNext() } returns autoDownloadNext
        coEvery { prefs.getNewChapterNotifications() } returns newChapterNotifications
        coEvery { prefs.getDeleteAfterRead() } returns deleteAfterRead
        coEvery { prefs.getDeleteAfterReadKeep() } returns deleteAfterReadKeep
    }

    private fun viewModel() =
        SettingsViewModel(
            preferencesDataStore = prefs,
            libraryRepository = libraryRepository,
            authRepository = authRepository,
            imageLoader = imageLoader,
            ioDispatcher = mainDispatcherRule.dispatcher,
            json = NetworkModule.provideJson(),
        )

    @Test
    fun init_taiQualityThemeLanguage() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(quality = "data-saver", theme = "dark", language = "vi")

            val vm = viewModel()
            advanceUntilIdle()

            assertEquals("data-saver", vm.uiState.value.quality)
            assertEquals("dark", vm.uiState.value.themeMode)
            assertEquals("vi", vm.uiState.value.language)
        }

    @Test
    fun toggleQuality_doiQuaLai() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(quality = "data")
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleQuality()
            advanceUntilIdle()
            assertEquals("data-saver", vm.uiState.value.quality)
            coVerify { prefs.setReaderQuality("data-saver") }

            vm.toggleQuality()
            advanceUntilIdle()
            assertEquals("data", vm.uiState.value.quality)
        }

    @Test
    fun init_taiKeepScreenOnVaVolumeKeyNav() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(keepScreenOn = true, volumeKeyNav = true)

            val vm = viewModel()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.keepScreenOn)
            assertTrue(vm.uiState.value.volumeKeyNav)
        }

    @Test
    fun toggleKeepScreenOn_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(keepScreenOn = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleKeepScreenOn()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.keepScreenOn)
            coVerify { prefs.setReaderKeepScreenOn(true) }
        }

    @Test
    fun toggleVolumeKeyNav_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(volumeKeyNav = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleVolumeKeyNav()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.volumeKeyNav)
            coVerify { prefs.setReaderVolumeKeyNav(true) }
        }

    @Test
    fun toggleAutoAdvance_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(autoAdvance = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleAutoAdvance()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.autoAdvance)
            coVerify { prefs.setReaderAutoAdvance(true) }
        }

    @Test
    fun toggleAutoDownloadNext_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(autoDownloadNext = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleAutoDownloadNext()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.autoDownloadNext)
            coVerify { prefs.setAutoDownloadNext(true) }
        }

    @Test
    fun toggleNewChapterNotifications_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(newChapterNotifications = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleNewChapterNotifications()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.newChapterNotifications)
            coVerify { prefs.setNewChapterNotifications(true) }
        }

    @Test
    fun toggleSkipReadChapters_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(skipReadChapters = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleSkipReadChapters()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.skipReadChapters)
            coVerify { prefs.setSkipReadChapters(true) }
        }

    @Test
    fun toggleDeleteAfterRead_doiVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(deleteAfterRead = false)
            val vm = viewModel()
            advanceUntilIdle()

            vm.toggleDeleteAfterRead()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.deleteAfterRead)
            coVerify { prefs.setDeleteAfterRead(true) }
        }

    @Test
    fun setDeleteAfterReadKeep_luuGiaTri() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(deleteAfterReadKeep = 2)
            val vm = viewModel()
            advanceUntilIdle()

            vm.setDeleteAfterReadKeep(3)
            advanceUntilIdle()

            assertEquals(3, vm.uiState.value.deleteAfterReadKeep)
            coVerify { prefs.setDeleteAfterReadKeep(3) }
        }

    @Test
    fun cycleThemeMode_systemLightDarkSystem() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(theme = "system")
            val vm = viewModel()
            advanceUntilIdle()

            vm.cycleThemeMode()
            advanceUntilIdle()
            assertEquals("light", vm.uiState.value.themeMode)
            vm.cycleThemeMode()
            advanceUntilIdle()
            assertEquals("dark", vm.uiState.value.themeMode)
            vm.cycleThemeMode()
            advanceUntilIdle()
            assertEquals("system", vm.uiState.value.themeMode)
        }

    @Test
    fun setLanguage_capNhatVaLuu() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            vm.setLanguage("vi")
            advanceUntilIdle()

            assertEquals("vi", vm.uiState.value.language)
            coVerify { prefs.setLanguage("vi") }
        }

    @Test
    fun clearImageCache_xoaCacheVaDatCo() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            vm.clearImageCache()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.cacheCleared)
        }

    @Test
    fun signOut_thanhCong_resetQualityVaDanhDau() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults(quality = "data-saver")
            val vm = viewModel()
            advanceUntilIdle()

            vm.signOut()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.signedOut)
            assertEquals("data", vm.uiState.value.quality)
            coVerify { authRepository.signOut() }
            coVerify { libraryRepository.clearAll() }
        }

    @Test
    fun signOut_loi_khongCrashVaSignedOutFalse() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            coEvery { libraryRepository.clearAll() } throws RuntimeException("db lỗi")
            val vm = viewModel()
            advanceUntilIdle()

            vm.signOut()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.signedOut)
        }

    @Test
    fun forceSync_chayFullTwoWaySyncVaBaoThanhCong() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            vm.forceSync()
            advanceUntilIdle()

            coVerify(exactly = 1) { libraryRepository.performSync() }
            assertFalse(vm.uiState.value.isSyncing)
            assertEquals(true, vm.uiState.value.syncSuccess)
        }

    @Test
    fun forceSync_fullTwoWaySyncLoi_thongBaoThatBai() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            coEvery { libraryRepository.performSync() } throws RuntimeException("sync lỗi")
            val vm = viewModel()
            advanceUntilIdle()

            vm.forceSync()
            advanceUntilIdle()

            coVerify(exactly = 1) { libraryRepository.performSync() }
            assertFalse(vm.uiState.value.isSyncing)
            assertEquals(false, vm.uiState.value.syncSuccess)
        }

    @Test
    fun backupLibrary_thanhCong_ghiJsonVaSuccess() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            coEvery { libraryRepository.getAllItems() } returns listOf(sampleEntity("m1"))
            coEvery { libraryRepository.getAllChapterProgress() } returns
                listOf(
                    ChapterProgressEntity(
                        chapter_id = "c1",
                        manga_id = "m1",
                        status = ChapterStatus.READING,
                        last_read_page = 4,
                        total_pages = 10,
                        updated_at = 2000,
                        is_downloaded = true,
                    ),
                )
            val vm = viewModel()
            advanceUntilIdle()
            val output = ByteArrayOutputStream()

            vm.backupLibrary(output)
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.backupResult)
            assertEquals(
                """{"version":2,"library":[{"manga_id":"m1","title":"Title m1","cover_url":"https://x/m1.jpg","status":"READING","last_read_chapter_id":"","last_read_page_index":0,"added_at":1000,"updated_at":1000,"is_favorite":false}],"chapter_progress":[{"chapter_id":"c1","manga_id":"m1","status":"READING","last_read_page":4,"total_pages":10,"updated_at":2000}]}""",
                output.toString(),
            )
        }

    @Test
    fun backupLibrary_loi_traveFailure() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            coEvery { libraryRepository.getAllItems() } throws RuntimeException("đọc lỗi")
            val vm = viewModel()
            advanceUntilIdle()

            vm.backupLibrary(ByteArrayOutputStream())
            advanceUntilIdle()

            assertTrue(vm.uiState.value.backupResult is BackupRestoreResult.Failure)
        }

    @Test
    fun restoreLibrary_jsonHopLe_restoreItems() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val restoredItems = slot<List<LibraryItemEntity>>()
            val restoredProgress = slot<List<ChapterProgressEntity>>()
            coEvery { libraryRepository.restoreBackup(capture(restoredItems), capture(restoredProgress)) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()
            val item =
                """{"manga_id":"m1","title":"T","cover_url":"","status":"READING",""" +
                    """"last_read_page_index":0,"updated_at":123}"""
            val json = "[$item]"

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
            assertEquals(
                sampleEntity("m1").copy(title = "T", cover_url = "", added_at = 123, updated_at = 123),
                restoredItems.captured.single(),
            )
            assertTrue(restoredProgress.captured.isEmpty())
        }

    @Test
    fun restoreLibrary_schemaMoi_restoreChapterProgress() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val restoredProgress = slot<List<ChapterProgressEntity>>()
            coEvery { libraryRepository.restoreBackup(any(), capture(restoredProgress)) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()
            val json =
                """{"version":2,"library":[{"manga_id":"m1","title":"T"}],""" +
                    """"chapter_progress":[{"chapter_id":"c1","manga_id":"m1","status":"COMPLETED",""" +
                    """"last_read_page":9,"total_pages":10,"updated_at":123}]}"""

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
            assertEquals(
                ChapterProgressEntity(
                    chapter_id = "c1",
                    manga_id = "m1",
                    status = ChapterStatus.COMPLETED,
                    last_read_page = 9,
                    total_pages = 10,
                    updated_at = 123,
                ),
                restoredProgress.captured.single(),
            )
        }

    @Test
    fun restoreLibrary_boQuaItemThieuField_vaStatusLaFallbackReading() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            // item 1 thiếu manga_id -> skip; item 2 status lạ -> fallback READING
            val item2 =
                """{"manga_id":"m2","title":"T2","cover_url":"c","status":"BADVALUE",""" +
                    """"last_read_page_index":2,"updated_at":5}"""
            val json = """[{"title":"no id"},$item2]"""

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
        }

    @Test
    fun restoreLibrary_itemDayDuField_phuNhanhPresent() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            // Đủ field non-null: cover_url có, last_read_chapter_id non-blank, page_index + updated_at present
            val item =
                """{"manga_id":"m1","title":"T","cover_url":"c.jpg","status":"COMPLETED",""" +
                    """"last_read_chapter_id":"c9","last_read_page_index":7,"updated_at":999}"""
            val json = "[$item]"

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
        }

    @Test
    fun restoreLibrary_thieuTitle_skip_vaChapterIdBlank_thanhNull() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            // item 1 thiếu title -> skip; item 2 last_read_chapter_id rỗng -> ifBlank -> null
            val item2 =
                """{"manga_id":"m2","title":"T2","cover_url":"","status":"READING",""" +
                    """"last_read_chapter_id":"","last_read_page_index":0,"updated_at":1}"""
            val json = """[{"manga_id":"x","cover_url":"c"},$item2]"""

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
        }

    @Test
    fun restoreLibrary_coverUrlThieuVaPageIndexUpdatedAtNull_dùngDefault() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()
            // cover_url thiếu -> ""; last_read_page_index thiếu -> 0; updated_at thiếu -> now
            val json = """[{"manga_id":"m1","title":"T","status":"READING"}]"""

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
        }

    @Test
    fun restoreLibrary_jsonHong_traveFailure() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val vm = viewModel()
            advanceUntilIdle()

            vm.restoreLibrary(ByteArrayInputStream("not-json{".toByteArray()))
            advanceUntilIdle()

            assertTrue(vm.uiState.value.restoreResult is BackupRestoreResult.Failure)
        }

    @Test
    fun restoreLibrary_itemSaiKieu_boQuaNhungVanRestoreItemHopLe() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubDefaults()
            val restoredItems = slot<List<LibraryItemEntity>>()
            coEvery { libraryRepository.restoreBackup(capture(restoredItems), any()) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()
            val json =
                """[{"manga_id":"bad","title":"Bad","last_read_page_index":"not-number"},""" +
                    """{"manga_id":"m2","title":"Good","unknown":"ignored"}]"""

            vm.restoreLibrary(ByteArrayInputStream(json.toByteArray()))
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.restoreResult)
            assertEquals("m2", restoredItems.captured.single().manga_id)
        }

    private fun sampleEntity(id: String) =
        LibraryItemEntity(
            manga_id = id,
            title = "Title $id",
            cover_url = "https://x/$id.jpg",
            status = LibraryStatus.READING,
            last_read_chapter_id = null,
            last_read_page_index = 0,
            added_at = 1000L,
            updated_at = 1000L,
        )
}
