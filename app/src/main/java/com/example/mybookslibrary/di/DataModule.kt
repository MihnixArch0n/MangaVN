package com.example.mybookslibrary.di

import android.content.Context
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt module cung cấp Room DB, DAO, Repository và DataStore cho toàn app
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideLibraryDao(database: AppDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

    @Provides
    @Singleton
    fun provideLibraryRepository(
        libraryDao: LibraryDao,
        chapterDao: ChapterDao
    ): LibraryRepository {
        return LibraryRepository(libraryDao, chapterDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context
    ): UserPreferencesDataStore =
        UserPreferencesDataStore(context.userPreferencesDataStore)

    @Provides
    @Singleton
    fun provideMangaRepository(
        api: MangaDexApi,
        preferencesDataStore: UserPreferencesDataStore
    ): MangaRepository = MangaRepository(api, preferencesDataStore)

}



