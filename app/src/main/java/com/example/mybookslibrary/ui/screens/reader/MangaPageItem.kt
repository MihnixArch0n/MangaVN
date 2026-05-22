package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.decode.DataSource
import coil3.request.ImageRequest
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.model.TapZoneEvaluator
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import timber.log.Timber


/**
 * Renders one zoomable manga page with dynamic sizing, retry support, and long-press actions.
 *
 * The composable keeps the page aspect ratio in sync with the loaded image,
 * overlays a retry UI when Coil reports an error, and uses Telephoto to support
 * pinch-to-zoom and double-tap-to-zoom while retaining Coil load-state logging.
 *
 * @param imageUrl Page image URL loaded by Coil.
 * @param index Zero-based page index used for content description and logs.
 * @param readingMode Current reader mode used to map Telephoto click coordinates to tap-zone actions.
 * @param modifier Modifier applied to the outer container.
 * @param onTapAction Callback invoked with the evaluated reader action for a Telephoto single tap.
 * @param onLongPress Optional callback invoked with [imageUrl] and [index] when the user
 * long-presses the page; if `null`, the gesture is ignored.
 */
@Composable
fun MangaPageItem(
    imageUrl: String,
    index: Int,
    readingMode: ReadingMode,
    modifier: Modifier = Modifier,
    onTapAction: (ReaderTapAction) -> Unit = {},
    onLongPress: ((String, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var aspectRatio by remember(imageUrl) { mutableStateOf<Float?>(null) }
    // Increment to force Coil to re-fetch when the user taps "retry"
    var retryHash by remember(imageUrl) { mutableIntStateOf(0) }
    var isError by remember(imageUrl) { mutableStateOf(false) }
    var pageWidthPx by remember(imageUrl) { mutableIntStateOf(0) }
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 3f))
    val zoomableImageState = rememberZoomableImageState(zoomableState)
    val retryPageLoad = remember(imageUrl, index) {
        {
            Timber.d("Retry tapped for page=%d url=%s", index + 1, imageUrl)
            retryHash++
            isError = false
        }
    }
    val imageRequest = remember(context, imageUrl, retryHash) {
        ImageRequest.Builder(context)
            // Append retryHash so Coil treats it as a new request on retry
            .data("$imageUrl#retry=$retryHash")
            .listener(
                onStart = {
                    isError = false
                    Timber.d("Loading page=%d url=%s retry=%d", index + 1, imageUrl, retryHash)
                },
                onSuccess = { _, result ->
                    val image = result.image
                    val w = image.width
                    val h = image.height
                    if (w > 0 && h > 0) {
                        aspectRatio = w.toFloat() / h.toFloat()
                    }
                    isError = false
                    val dataSource = result.dataSource
                    val origin = if (dataSource == DataSource.NETWORK) "internet" else "cache"
                    Timber.d(
                        "Loaded page=%d url=%s origin=%s source=%s",
                        index + 1,
                        imageUrl,
                        origin,
                        dataSource
                    )
                },
                onError = { _, result ->
                    isError = true
                    Timber.e(result.throwable, "Failed to load page=%d url=%s", index + 1, imageUrl)
                }
            )
            .build()
    }

    LaunchedEffect(zoomableState, imageUrl, index) {
        snapshotFlow { zoomableState.zoomFraction }
            .distinctUntilChanged()
            .collect { zoomFraction ->
                Timber.d("Reader zoom changed: page=%d zoomFraction=%s", index + 1, zoomFraction)
            }
    }

    val imageModifier = remember(aspectRatio, modifier) {
        if (aspectRatio != null) {
            modifier.fillMaxWidth().aspectRatio(aspectRatio!!)
        } else {
            modifier.fillMaxWidth().height(400.dp)
        }
    }

    Box(
        modifier = imageModifier
            .onSizeChanged { pageWidthPx = it.width }
    ) {
        ZoomableAsyncImage(
            model = imageRequest,
            contentDescription = appString(R.string.reader_page_description, index + 1),
            state = zoomableImageState,
            contentScale = ContentScale.FillWidth,
            onClick = { offset ->
                val action = TapZoneEvaluator.evaluateTap(
                    x = offset.x,
                    totalWidth = pageWidthPx.toFloat(),
                    mode = readingMode
                )
                Timber.d(
                    "Reader page tap: page=%d x=%.1f width=%d mode=%s action=%s",
                    index + 1,
                    offset.x,
                    pageWidthPx,
                    readingMode,
                    action
                )
                onTapAction(action)
            },
            onLongClick = {
                Timber.d("Reader page long-click: page=%d url=%s", index + 1, imageUrl)
                onLongPress?.invoke(imageUrl, index)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Error overlay: icon + "Tap to retry"
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = retryPageLoad),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = appString(R.string.reader_loading_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = retryPageLoad
                    ) {
                        Text(
                        text = appString(R.string.reader_tap_to_retry),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private const val PreviewPageUrl = "https://example.com/preview-page.jpg"

@Preview(name = "Manga Page Item", showBackground = true)
@Composable
private fun MangaPageItemPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(Color.Black)
        ) {
            MangaPageItem(
                imageUrl = PreviewPageUrl,
                index = 0,
                readingMode = ReadingMode.VERTICAL,
                onTapAction = {},
                onLongPress = { _, _ -> },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
