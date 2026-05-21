package com.example.mybookslibrary.data.remote

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
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
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OkHttp", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
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
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheDir = context.cacheDir
        Timber.d(
            "Image OkHttp cache configured: dir=%s sizeBytes=%d",
            cacheDir,
            IMAGE_HTTP_CACHE_SIZE_BYTES
        )
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, IMAGE_HTTP_CACHE_SIZE_BYTES))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideMangaDexApi(retrofit: Retrofit): MangaDexApi =
        retrofit.create(MangaDexApi::class.java)
}
