package com.example.mybookslibrary.di

import android.content.Context
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.DownloadQueueDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.data.remote.MangaDexApi
import com.example.mybookslibrary.data.repository.CredentialManagerGoogleSignInClient
import com.example.mybookslibrary.data.repository.GoogleSignInClient
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

import com.example.mybookslibrary.data.remote.FirestoreDataSource
import com.example.mybookslibrary.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// Hilt module cung cấp Room DB, DAO, Repository và DataStore cho toàn app
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideLibraryDao(database: AppDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

    @Provides
    fun provideDownloadQueueDao(database: AppDatabase): DownloadQueueDao = database.downloadQueueDao()

    @Provides
    @Singleton
    fun provideLibraryRepository(
        libraryDao: LibraryDao,
        chapterDao: ChapterDao,
        database: AppDatabase,
        firestoreDataSource: FirestoreDataSource,
        authRepository: AuthRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LibraryRepository = LibraryRepository(
        libraryDao,
        chapterDao,
        database,
        firestoreDataSource,
        authRepository,
        CoroutineScope(SupervisorJob() + ioDispatcher)
    )

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
    ): UserPreferencesDataStore = UserPreferencesDataStore(context.userPreferencesDataStore)

    @Provides
    @Singleton
    fun provideMangaRepository(
        api: MangaDexApi,
        preferencesDataStore: UserPreferencesDataStore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): MangaRepository = MangaRepository(api, preferencesDataStore, ioDispatcher)

    @Provides
    @Singleton
    fun provideGoogleSignInClient(): GoogleSignInClient = CredentialManagerGoogleSignInClient()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
