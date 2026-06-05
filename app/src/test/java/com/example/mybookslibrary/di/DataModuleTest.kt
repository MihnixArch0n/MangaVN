package com.example.mybookslibrary.di

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.remote.MangaDexApi
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Kiểm tra DataModule cung cấp đúng các dependency.
 * Dùng Robolectric để có ApplicationContext cho Room + DataStore.
 */
@RunWith(RobolectricTestRunner::class)
class DataModuleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase =
        Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Test
    fun provideAppDatabase_returnsNonNull() {
        assertNotNull(DataModule.provideAppDatabase(context))
        DataModule.provideAppDatabase(context).close()
    }

    @Test
    fun provideLibraryDao_returnsNonNull() {
        assertNotNull(DataModule.provideLibraryDao(db))
    }

    @Test
    fun provideChapterDao_returnsNonNull() {
        assertNotNull(DataModule.provideChapterDao(db))
    }

    @Test
    fun provideDownloadQueueDao_returnsNonNull() {
        assertNotNull(DataModule.provideDownloadQueueDao(db))
    }

    @Test
    fun provideUserDao_returnsNonNull() {
        assertNotNull(DataModule.provideUserDao(db))
    }

    @Test
    fun provideLibraryRepository_returnsNonNull() {
        val repo =
            DataModule.provideLibraryRepository(
                libraryDao = DataModule.provideLibraryDao(db),
                chapterDao = DataModule.provideChapterDao(db),
                database = db,
            )
        assertNotNull(repo)
    }

    @Test
    fun provideUserPreferencesDataStore_returnsNonNull() {
        assertNotNull(DataModule.provideUserPreferencesDataStore(context))
    }

    @Test
    fun provideMangaRepository_returnsNonNull() {
        val prefs = DataModule.provideUserPreferencesDataStore(context)
        val repo =
            DataModule.provideMangaRepository(
                api = mockk<MangaDexApi>(relaxed = true),
                preferencesDataStore = prefs,
                ioDispatcher = Dispatchers.IO,
            )
        assertNotNull(repo)
    }

    @Test
    fun provideGoogleSignInClient_returnsNonNull() {
        assertNotNull(DataModule.provideGoogleSignInClient())
    }
}
