@file:Suppress(
    "LongMethod",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString
import timber.log.Timber

/**
 * Static webtoon page item. Zoom is handled by the parent vertical reader container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WebtoonPageItem(
    imageUrl: String,
    index: Int,
    modifier: Modifier = Modifier,
    onTap: (x: Float, y: Float, width: Float, height: Float) -> Unit = { _, _, _, _ -> },
    onLongPress: ((String, Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    var aspectRatio by remember(imageUrl) { mutableStateOf<Float?>(null) }
    var retryHash by remember(imageUrl) { mutableIntStateOf(0) }
    var isError by remember(imageUrl) { mutableStateOf(false) }
    var pageWidthPx by remember(imageUrl) { mutableIntStateOf(0) }
    var pageHeightPx by remember(imageUrl) { mutableIntStateOf(0) }

    val retryPageLoad =
        remember(imageUrl, index) {
            {
                Timber.v("Retry tapped for webtoon page=%d url=%s", index + 1, imageUrl)
                retryHash++
                isError = false
            }
        }
    val imageRequest =
        remember(context, imageUrl, retryHash) {
            ImageRequest
                .Builder(context)
                .data("$imageUrl#retry=$retryHash")
                .listener(
                    onStart = {
                        isError = false
                    },
                    onSuccess = { _, result ->
                        val image = result.image
                        val w = image.width
                        val h = image.height
                        if (w > 0 && h > 0) {
                            aspectRatio = w.toFloat() / h.toFloat()
                        }
                        isError = false
                    },
                    onError = { _, result ->
                        isError = true
                        Timber.w(result.throwable, "Failed to load webtoon page=%d url=%s", index + 1, imageUrl)
                    },
                ).build()
        }

    val imageModifier =
        if (aspectRatio != null) {
            modifier.fillMaxWidth().aspectRatio(aspectRatio!!)
        } else {
            modifier.fillMaxWidth().height(400.dp)
        }

    Box(
        modifier =
            imageModifier
                .onSizeChanged {
                    pageWidthPx = it.width
                    pageHeightPx = it.height
                }.combinedClickable(
                    onClick = {
                        Timber.v(
                            "Reader webtoon page tap: page=%d width=%d height=%d",
                            index + 1,
                            pageWidthPx,
                            pageHeightPx,
                        )
                        onTap(
                            pageWidthPx / 2f,
                            pageHeightPx / 2f,
                            pageWidthPx.toFloat(),
                            pageHeightPx.toFloat(),
                        )
                    },
                    onLongClick = {
                        Timber.v("Reader webtoon page long-click: page=%d url=%s", index + 1, imageUrl)
                        onLongPress?.invoke(imageUrl, index)
                    },
                ),
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = appString(R.string.reader_page_description, index + 1),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize(),
        )

        if (isError) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .combinedClickable(
                            onClick = retryPageLoad,
                            onLongClick = {
                                Timber.v("Reader webtoon error long-click: page=%d url=%s", index + 1, imageUrl)
                                onLongPress?.invoke(imageUrl, index)
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = appString(R.string.reader_loading_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = retryPageLoad) {
                        Text(
                            text = appString(R.string.reader_tap_to_retry),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Webtoon Page Item", showBackground = true)
@Composable
private fun WebtoonPageItemPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .background(Color.Black),
        ) {
            WebtoonPageItem(
                imageUrl = "https://example.com/preview-page.jpg",
                index = 0,
                onTap = { _, _, _, _ -> },
                onLongPress = { _, _ -> },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
