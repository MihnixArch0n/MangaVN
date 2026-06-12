@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * LoadingIndicator wrapper — size token S/M/L (§3.3 + §1B).
 * Khi material3 1.5+ stable → đổi sang M3 Expressive `LoadingIndicator` morphing.
 */
object LoadingSize {
    val Small: Dp = 24.dp
    val Medium: Dp = 36.dp
    val Large: Dp = 48.dp
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = LoadingSize.Medium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = if (size <= LoadingSize.Small) 2.dp else 3.dp,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Loading — dark S/M/L", showBackground = true)
@Composable
private fun LoadingIndicatorDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            LoadingIndicator(size = LoadingSize.Small)
            LoadingIndicator(size = LoadingSize.Medium, modifier = Modifier.padding(top = Dimens.SpacingSm))
            LoadingIndicator(size = LoadingSize.Large, modifier = Modifier.padding(top = Dimens.SpacingSm))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Loading — light", showBackground = true)
@Composable
private fun LoadingIndicatorLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        LoadingIndicator(size = LoadingSize.Medium)
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Loading — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun LoadingIndicator320Preview() {
    MyBooksLibraryTheme { LoadingIndicator(size = LoadingSize.Large) }
}
