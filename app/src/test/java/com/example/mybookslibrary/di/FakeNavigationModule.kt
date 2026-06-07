@file:Suppress("ktlint")

package com.example.mybookslibrary.di

import android.content.Context
import androidx.room.Room
import coil3.ImageLoader
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.DownloadQueueDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.local.dao.UserDao
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.data.remote.AtHomeReportInterceptor
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.remote.NetworkModule
import com.example.mybookslibrary.data.repository.GoogleSignInClient
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

/**
 * Module thay thế DataModule + NetworkModule + ImageModule trong JVM/Robolectric test.
 * Dùng in-memory Room DB, real DataStore (Robolectric filesystem) và mockk cho network.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class, NetworkModule::class, ImageModule::class],
)
object FakeNavigationModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): UserPreferencesDataStore = UserPreferencesDataStore(context.userPreferencesDataStore)

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideLibraryDao(db: AppDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideDownloadQueueDao(db: AppDatabase): DownloadQueueDao = db.downloadQueueDao()

    @Provides
    @Singleton
    fun provideMangaDexApi(): MangaDexApi = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideJson(): Json = NetworkModule.provideJson()

    @Provides
    @Singleton
    fun provideMangaRepository(
        api: MangaDexApi,
        prefs: UserPreferencesDataStore,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): MangaRepository = MangaRepository(api, prefs, dispatcher)

    @Provides
    @Singleton
    fun provideLibraryRepository(
        libraryDao: LibraryDao,
        chapterDao: ChapterDao,
        db: AppDatabase,
    ): LibraryRepository = LibraryRepository(libraryDao, chapterDao, db)

    @Provides
    @Singleton
    fun provideGoogleSignInClient(): GoogleSignInClient = mockk(relaxed = true)

    @Provides
    @Singleton
    @Named("ImageOkHttpClient")
    fun provideImageOkHttpClient(): OkHttpClient = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideAtHomeReportInterceptor(): AtHomeReportInterceptor = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader = ImageLoader.Builder(context).build()
}
