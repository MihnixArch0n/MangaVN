package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.util.appString
import timber.log.Timber


/**
 * Renders one manga page with dynamic sizing, retry support, and long-press actions.
 *
 * The composable keeps the page aspect ratio in sync with the loaded image,
 * overlays a retry UI when Coil reports an error, and forwards long-press
 * gestures to [onLongPress] so the caller can open page actions.
 *
 * @param imageUrl Page image URL loaded by Coil.
 * @param index Zero-based page index used for content description and logs.
 * @param modifier Modifier applied to the outer container.
 * @param onLongPress Optional callback invoked with [imageUrl] and [index] when the user
 * long-presses the page; if `null`, the gesture is ignored.
 */
@Composable
fun MangaPageItem(
    imageUrl: String,
    index: Int,
    modifier: Modifier = Modifier,
    onLongPress: ((String, Int) -> Unit)? = null
) {
    var aspectRatio by remember(imageUrl) { mutableStateOf<Float?>(null) }
    // Increment to force Coil to re-fetch when the user taps "retry"
    var retryHash by remember(imageUrl) { mutableIntStateOf(0) }
    var isError by remember(imageUrl) { mutableStateOf(false) }

    val imageModifier = remember(aspectRatio, modifier) {
        if (aspectRatio != null) {
            modifier.fillMaxWidth().aspectRatio(aspectRatio!!)
        } else {
            modifier.fillMaxWidth().defaultMinSize(minHeight = 400.dp)
        }
    }

    Box(
        modifier = imageModifier
            .pointerInput(imageUrl) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress != null) {
                        Timber.d("Long press detected for page=%d url=%s", index + 1, imageUrl)
                        onLongPress?.invoke(imageUrl, index)
                    }
                }
            }
    ) {
        AsyncImage(
            // Append retryHash so Coil treats it as a new request on retry
            model = "$imageUrl#retry=$retryHash",
            contentDescription = appString(R.string.reader_page_description, index + 1),
            contentScale = ContentScale.FillWidth,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Success -> {
                        val intrinsicSize = state.painter.intrinsicSize
                        val w = intrinsicSize.width
                        val h = intrinsicSize.height
                        if (w > 0f && h > 0f) {
                            aspectRatio = w / h
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        isError = true
                        Timber.e(state.result.throwable, "Failed to load page=%d url=%s", index + 1, imageUrl)
                    }
                    else -> { /* Loading / Empty — no-op */ }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Error overlay: icon + "Tap to retry"
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
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
                        onClick = {
                            Timber.d("Retry tapped for page=%d url=%s", index + 1, imageUrl)
                            retryHash++
                            isError = false
                        }
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
