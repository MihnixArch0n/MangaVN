package com.example.mybookslibrary.di

import android.content.Context
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.repository.LibraryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    fun provideLibraryDao(database: AppDatabase): LibraryDao = database.libraryDao()

    @Provides
    @Singleton
    fun provideLibraryRepository(libraryDao: LibraryDao): LibraryRepository {
        return LibraryRepository(libraryDao)
    }
}

