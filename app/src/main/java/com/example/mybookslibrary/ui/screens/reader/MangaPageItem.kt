@file:Suppress(
    "LongParameterList",
    "MaxLineLength",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.ZoomableState
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
 * @param modifier Modifier applied to the outer container.
 * @param onConfirmedTap Callback invoked with raw tap coordinates and page bounds after Telephoto
 * confirms a single tap.
 * @param onLongPress Optional callback invoked with [imageUrl] and [index] when the user
 * long-presses the page; if `null`, the gesture is ignored.
 */
@Composable
fun MangaPageItem(
    imageUrl: String,
    index: Int,
    modifier: Modifier = Modifier,
    onConfirmedTap: (x: Float, y: Float, width: Float, height: Float) -> Unit = { _, _, _, _ -> },
    onLongPress: ((String, Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    var retryHash by remember(imageUrl) { mutableIntStateOf(0) }
    var isError by remember(imageUrl) { mutableStateOf(false) }
    var pageSize by remember(imageUrl) { mutableStateOf(IntSize.Zero) }
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 3f))
    val zoomableImageState = rememberZoomableImageState(zoomableState)
    val retryPageLoad =
        remember(imageUrl, index) {
            {
                Timber.d("Retry tapped for page=%d url=%s", displayPage(index), imageUrl)
                retryHash++
                isError = false
            }
        }
    val imageRequest =
        remember(context, imageUrl, retryHash) {
            buildPageImageRequest(
                context = context,
                imageUrl = imageUrl,
                pageIndex = index,
                retryHash = retryHash,
                onLoadingChanged = { isError = it },
            )
        }

    LogPageZoomChanges(zoomableState = zoomableState, imageUrl = imageUrl, index = index)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { pageSize = it },
    ) {
        MangaPageImage(
            model = imageRequest,
            index = index,
            state = zoomableImageState,
            pageSize = pageSize,
            imageUrl = imageUrl,
            onConfirmedTap = onConfirmedTap,
            onLongPress = onLongPress,
            modifier = Modifier.fillMaxSize(),
        )

        if (isError) {
            MangaPageRetryOverlay(onRetry = retryPageLoad)
        }
    }
}

private fun buildPageImageRequest(
    context: android.content.Context,
    imageUrl: String,
    pageIndex: Int,
    retryHash: Int,
    onLoadingChanged: (Boolean) -> Unit,
): ImageRequest =
    ImageRequest
        .Builder(context)
        .data("$imageUrl#retry=$retryHash")
        .listener(
            onStart = {
                onLoadingChanged(false)
                Timber.d("Loading page=%d url=%s retry=%d", displayPage(pageIndex), imageUrl, retryHash)
            },
            onSuccess = { _, result ->
                onLoadingChanged(false)
                logPageLoadSuccess(pageIndex = pageIndex, imageUrl = imageUrl, result = result)
            },
            onError = { _, result ->
                onLoadingChanged(true)
                Timber.e(result.throwable, "Failed to load page=%d url=%s", displayPage(pageIndex), imageUrl)
            },
        ).build()

private fun logPageLoadSuccess(
    pageIndex: Int,
    imageUrl: String,
    result: SuccessResult,
) {
    val image = result.image
    if (image.width > 0 && image.height > 0) {
        Timber.d("Reader page decoded: page=%d width=%d height=%d", displayPage(pageIndex), image.width, image.height)
    }

    val dataSource = result.dataSource
    val origin = if (dataSource == DataSource.NETWORK) "internet" else "cache"
    Timber.d(
        "Loaded page=%d url=%s origin=%s source=%s",
        displayPage(pageIndex),
        imageUrl,
        origin,
        dataSource,
    )
}

@Composable
private fun LogPageZoomChanges(
    zoomableState: ZoomableState,
    imageUrl: String,
    index: Int,
) {
    LaunchedEffect(zoomableState, imageUrl, index) {
        snapshotFlow { zoomableState.zoomFraction }
            .distinctUntilChanged()
            .collect { zoomFraction ->
                Timber.d("Reader zoom changed: page=%d zoomFraction=%s", displayPage(index), zoomFraction)
            }
    }
}

@Composable
private fun MangaPageImage(
    model: ImageRequest,
    index: Int,
    state: ZoomableImageState,
    pageSize: IntSize,
    imageUrl: String,
    onConfirmedTap: (x: Float, y: Float, width: Float, height: Float) -> Unit,
    onLongPress: ((String, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ZoomableAsyncImage(
        model = model,
        contentDescription = appString(R.string.reader_page_description, displayPage(index)),
        state = state,
        contentScale = ContentScale.FillWidth,
        onClick = { offset ->
            Timber.d(
                "Reader page confirmed tap: page=%d x=%.1f y=%.1f width=%d height=%d",
                displayPage(index),
                offset.x,
                offset.y,
                pageSize.width,
                pageSize.height,
            )
            onConfirmedTap(offset.x, offset.y, pageSize.width.toFloat(), pageSize.height.toFloat())
        },
        onLongClick = {
            Timber.d("Reader page long-click: page=%d url=%s", displayPage(index), imageUrl)
            onLongPress?.invoke(imageUrl, index)
        },
        modifier = modifier,
    )
}

@Composable
private fun MangaPageRetryOverlay(onRetry: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ERROR_OVERLAY_ALPHA))
                .clickable(onClick = onRetry),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.BrokenImage,
                contentDescription = null,
                tint = Color.White.copy(alpha = ERROR_ICON_ALPHA),
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = appString(R.string.reader_loading_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = ERROR_TEXT_ALPHA),
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRetry) {
                Text(
                    text = appString(R.string.reader_tap_to_retry),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

private fun displayPage(index: Int): Int = index + 1

private const val ERROR_OVERLAY_ALPHA = 0.7f
private const val ERROR_ICON_ALPHA = 0.7f
private const val ERROR_TEXT_ALPHA = 0.6f
private const val PREVIEW_PAGE_URL = "https://example.com/preview-page.jpg"

@Preview(name = "Manga Page Item", showBackground = true)
@Composable
private fun MangaPageItemPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .background(Color.Black),
        ) {
            MangaPageItem(
                imageUrl = PREVIEW_PAGE_URL,
                index = 0,
                onConfirmedTap = { _, _, _, _ -> },
                onLongPress = { _, _ -> },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
