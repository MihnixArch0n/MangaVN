package com.example.mybookslibrary.ui.util

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalIsLandscape = compositionLocalOf { false }

@Composable
fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/** Tablet thật (smallest width ≥ 600dp) — không đổi khi xoay, dùng để chọn rail vs bottom bar. */
@Composable
fun isTablet(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP

@Composable
fun adaptiveMaxWidth(): Dp = if (isLandscape()) MAX_CONTENT_WIDTH_LANDSCAPE else Dp.Unspecified

@Composable
fun adaptiveFormMaxWidth(): Dp = if (isLandscape()) MAX_FORM_WIDTH else Dp.Unspecified

@Composable
fun adaptiveGridColumns(): Int = if (isLandscape()) GRID_COLUMNS_LANDSCAPE else GRID_COLUMNS_PORTRAIT

@Composable
fun CenteredContent(
    maxWidth: Dp = MAX_CONTENT_WIDTH_LANDSCAPE,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth)) {
            content()
        }
    }
}

private const val TABLET_MIN_WIDTH_DP = 600

private val MAX_CONTENT_WIDTH_LANDSCAPE = 640.dp
private val MAX_FORM_WIDTH = 420.dp
private const val GRID_COLUMNS_PORTRAIT = 3
private const val GRID_COLUMNS_LANDSCAPE = 5
