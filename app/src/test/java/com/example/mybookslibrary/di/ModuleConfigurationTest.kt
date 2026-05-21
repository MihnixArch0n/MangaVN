package com.example.mybookslibrary.di

import android.content.Context
import com.example.mybookslibrary.data.remote.NetworkModule
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class ModuleConfigurationTest {

    @Test
    fun provideImageOkHttpClient_hasCache() {
        val context = createContextWithTempCache()
        val client = NetworkModule.provideImageOkHttpClient(context)

        assertNotNull(client.cache)
    }

    @Test
    fun provideCoilImageLoader_hasDiskCache() {
        val context = createContextWithTempCache()
        val client = NetworkModule.provideImageOkHttpClient(context)
        val imageLoader = ImageModule.provideCoilImageLoader(context, client)

        assertNotNull(imageLoader.diskCache)
    }

    private fun createContextWithTempCache(): Context {
        val baseContext = RuntimeEnvironment.getApplication()
        val tempDir = Files.createTempDirectory("image-cache-test").toFile()
        val context = spyk(baseContext)
        every { context.cacheDir } returns tempDir
        return context
    }
}
