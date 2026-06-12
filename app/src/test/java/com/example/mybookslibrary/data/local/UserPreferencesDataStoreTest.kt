@file:Suppress("ktlint")

package com.example.mybookslibrary.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phủ toàn bộ getter/setter/observer của [UserPreferencesDataStore] bằng DataStore
 * in-memory (không cần Robolectric): xác nhận default đúng, set rồi đọc lại khớp,
 * và nhánh xoá `LOGGED_IN_USER_ID` khi set null.
 *
 * Bổ sung [UserPreferencesDataStoreErrorTest] (chỉ phủ 1 hàm với IOException) bằng
 * cách phủ cả nhánh rethrow lỗi non-IOException của `safeData`.
 */
class UserPreferencesDataStoreTest {
    // DataStore in-memory: data emit state hiện tại, edit{} cập nhật state.
    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private fun store() = UserPreferencesDataStore(FakePreferencesDataStore())

    @Test
    fun readerQuality_macDinhRoiSetDocLai() =
        runTest {
            val store = store()
            assertEquals("data", store.getReaderQuality())

            store.setReaderQuality("data-saver")

            assertEquals("data-saver", store.getReaderQuality())
        }

    @Test
    fun language_macDinhSetVaObserve() =
        runTest {
            val store = store()
            assertEquals("vi", store.getLanguage())
            assertEquals("vi", store.observeLanguage().first())

            store.setLanguage("en")

            assertEquals("en", store.getLanguage())
            assertEquals("en", store.observeLanguage().first())
        }

    @Test
    fun themeMode_macDinhSetVaObserve() =
        runTest {
            val store = store()
            assertEquals("system", store.getThemeMode())
            assertEquals("system", store.observeThemeMode().first())

            store.setThemeMode("dark")

            assertEquals("dark", store.getThemeMode())
            assertEquals("dark", store.observeThemeMode().first())
        }

    @Test
    fun downloadOnlyOnWifi_macDinhTrueSetFalse() =
        runTest {
            val store = store()
            assertTrue(store.getDownloadOnlyOnWifi())
            assertTrue(store.observeDownloadOnlyOnWifi().first())

            store.setDownloadOnlyOnWifi(false)

            assertEquals(false, store.getDownloadOnlyOnWifi())
            assertEquals(false, store.observeDownloadOnlyOnWifi().first())
        }

    @Test
    fun authStatus_macDinhRoiSetDocLai() =
        runTest {
            val store = store()
            // Default AuthStatus string from empty is null -> AuthStatus.LOGGED_OUT
            assertEquals(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_OUT, store.getAuthStatus())
            assertEquals(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_OUT, store.observeAuthStatus().first())

            store.updateAuthStatus(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_IN)
            assertEquals(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_IN, store.getAuthStatus())
            assertEquals(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_IN, store.observeAuthStatus().first())
        }

    @Test
    fun firebaseUid_setRoiObserveRoiXoaBangNull() =
        runTest {
            val store = store()
            assertNull(store.getFirebaseUid())
            assertNull(store.observeFirebaseUid().first())

            store.updateFirebaseUid("user-1")
            assertEquals("user-1", store.getFirebaseUid())
            assertEquals("user-1", store.observeFirebaseUid().first())

            // Nhánh else: set null -> remove key -> đọc lại ra null
            store.updateFirebaseUid(null)
            assertNull(store.getFirebaseUid())
        }

    @Test
    fun clearAll_xoaHetVeMacDinh() =
        runTest {
            val store = store()
            store.setLanguage("vi")
            store.setReaderQuality("data-saver")
            store.updateFirebaseUid("user-1")
            store.updateAuthStatus(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_IN)

            store.clearAll()

            assertEquals("vi", store.getLanguage())
            assertEquals("data", store.getReaderQuality())
            assertNull(store.getFirebaseUid())
            assertEquals(com.example.mybookslibrary.domain.model.AuthStatus.LOGGED_OUT, store.getAuthStatus())
        }

    @Test
    fun safeData_khiNonIOExceptionThiReném() {
        // safeData chỉ nuốt IOException; lỗi khác phải bung ra (nhánh `else throw e`).
        val throwingStore =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> =
                    flow { throw IllegalStateException("boom") }

                override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                    throw UnsupportedOperationException()
            }
        val store = UserPreferencesDataStore(throwingStore)

        assertThrows(IllegalStateException::class.java) {
            runBlocking { store.getLanguage() }
        }
    }
}
