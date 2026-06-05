package com.example.mybookslibrary.ui.util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import org.robolectric.RuntimeEnvironment

/**
 * ImageLoader giả lập cho test Robolectric: trả ngay ColorDrawable thay vì fetch network.
 * Dùng [install] trong @Before để ghi đè SingletonImageLoader của Coil.
 */
@coil3.annotation.ExperimentalCoilApi
object FakeImageLoader {
    fun install() {
        val context = RuntimeEnvironment.getApplication()
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(context)
                .components {
                    add(coil3.intercept.Interceptor { _ ->
                        SuccessResult(
                            image = ColorDrawable(Color.GRAY).asImage(),
                            request = ImageRequest.Builder(context).data("fake").build(),
                            dataSource = DataSource.MEMORY,
                        )
                    })
                }
                .build()
        }
    }

    /** Cài ImageLoader luôn thất bại → trigger `isError=true` trong MangaPageItem/WebtoonPageItem. */
    fun installFailing() {
        val context = RuntimeEnvironment.getApplication()
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(context)
                .components {
                    add(coil3.intercept.Interceptor { chain ->
                        ErrorResult(
                            image = null,
                            request = chain.request,
                            throwable = RuntimeException("fake load error"),
                        )
                    })
                }
                .build()
        }
    }

    @coil3.annotation.DelicateCoilApi
    fun reset() {
        SingletonImageLoader.reset()
    }
}
