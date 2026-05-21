package com.example.mybookslibrary.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton
import okio.Path.Companion.toPath

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    private const val IMAGE_DISK_CACHE_SIZE_BYTES = 200L * 1024 * 1024

    /**
     * ImageLoader with disk caching to speed up re-reading pages and reduce downloads.
     */
    @Provides
    @Singleton
    fun provideCoilImageLoader(
        @ApplicationContext context: Context,
        @Named("ImageOkHttpClient") imageOkHttpClient: OkHttpClient
    ): ImageLoader {
        val diskCacheDir = context.cacheDir.resolve("image_cache")
        Timber.d(
            "Coil disk cache configured: dir=%s sizeBytes=%d",
            diskCacheDir,
            IMAGE_DISK_CACHE_SIZE_BYTES
        )
        return ImageLoader.Builder(context)
            .diskCache(
                DiskCache.Builder()
                    .directory(diskCacheDir.path.toPath())
                    .maxSizeBytes(IMAGE_DISK_CACHE_SIZE_BYTES)
                    .build()
            )
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageOkHttpClient }))
            }
            .build()
    }
}
