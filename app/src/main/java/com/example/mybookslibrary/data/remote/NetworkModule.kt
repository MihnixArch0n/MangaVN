package com.example.mybookslibrary.data.remote

import android.content.Context
import android.util.Log
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

// Hilt module cung cấp OkHttpClient, Retrofit và MangaDexApi singleton
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://api.mangadex.org/"
    private const val IMAGE_HTTP_CACHE_SIZE_BYTES = 50L * 1024 * 1024

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()

    /**
     * Unauthenticated OkHttpClient strictly for image loading (Coil).
     * No Authorization headers to avoid MangaDex@Home rejection.
     * Uses a small disk cache to speed up re-reads and reduce network usage.
     */
    @Provides
    @Singleton
    @Named("ImageOkHttpClient")
    fun provideImageOkHttpClient(
        @ApplicationContext context: Context,
        atHomeReportInterceptor: AtHomeReportInterceptor,
    ): OkHttpClient {
        val cacheDir = context.cacheDir
        Timber.d(
            "Image OkHttp cache configured: dir=%s sizeBytes=%d",
            cacheDir,
            IMAGE_HTTP_CACHE_SIZE_BYTES,
        )
        return OkHttpClient
            .Builder()
            .cache(Cache(cacheDir, IMAGE_HTTP_CACHE_SIZE_BYTES))
            .addInterceptor(atHomeReportInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAtHomeReportInterceptor(
        mangaRepository: MangaRepository,
        @ApplicationScope applicationScope: CoroutineScope,
    ): AtHomeReportInterceptor =
        AtHomeReportInterceptor(
            mangaRepository = mangaRepository,
            applicationScope = applicationScope,
        )

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideMangaDexApi(retrofit: Retrofit): MangaDexApi = retrofit.create(MangaDexApi::class.java)
}
