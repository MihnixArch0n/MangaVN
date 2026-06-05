package com.example.mybookslibrary.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Kiểm tra ImageModule cung cấp Coil ImageLoader đúng.
 * Dùng Robolectric để có ApplicationContext + cacheDir.
 */
@RunWith(RobolectricTestRunner::class)
class ImageModuleTest {
    @Test
    fun provideCoilImageLoader_returnsNonNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader =
            ImageModule.provideCoilImageLoader(
                context = context,
                imageOkHttpClient = mockk<OkHttpClient>(relaxed = true),
            )
        assertNotNull(imageLoader)
    }
}
