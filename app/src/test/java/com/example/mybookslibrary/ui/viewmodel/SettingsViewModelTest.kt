@file:Suppress("ktlint", "MaxLineLength")

package com.example.mybookslibrary.ui.viewmodel

import coil3.ImageLoader
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

    private fun stubDefaults(
        quality: String = "data",
        theme: String = "system",
        language: String = "en",
        keepScreenOn: Boolean = false,
        volumeKeyNav: Boolean = false,
    ) {
        coEvery { prefs.getReaderQuality() } returns quality
        coEvery { prefs.getThemeMode() } returns theme
        coEvery { prefs.getLanguage() } returns language
        coEvery { prefs.getReaderKeepScreenOn() } returns keepScreenOn
        coEvery { prefs.getReaderVolumeKeyNav() } returns volumeKeyNav
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
            val vm = viewModel()
            advanceUntilIdle()
            val output = ByteArrayOutputStream()

            vm.backupLibrary(output)
            advanceUntilIdle()

            assertEquals(BackupRestoreResult.Success(1), vm.uiState.value.backupResult)
            assertEquals(
                """[{"manga_id":"m1","title":"Title m1","cover_url":"https://x/m1.jpg","status":"READING","last_read_chapter_id":"","last_read_page_index":0,"added_at":1000,"updated_at":1000,"is_favorite":false}]""",
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
            coEvery { libraryRepository.restoreItems(capture(restoredItems)) } returns Unit
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
            coEvery { libraryRepository.restoreItems(capture(restoredItems)) } returns Unit
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
