@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.ui.theme.CoverShape
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.LocalReducedMotion
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Loading placeholder shimmer — thay spinner trơ bằng hiệu ứng sáng chạy ngang.
 * Tự tắt animation khi reduce-motion (render màu tĩnh surfaceContainer).
 */
@Composable
fun SkeletonShimmer(
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val baseColor = MaterialTheme.colorScheme.surfaceContainer
    val highlightColor = MaterialTheme.colorScheme.surfaceContainerHigh

    if (reducedMotion) {
        Box(modifier = modifier.background(baseColor))
        return
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmerTranslate",
    )

    val brush =
        Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(translateX, 0f),
            end = Offset(translateX + 300f, 0f),
        )
    Box(modifier = modifier.background(brush))
}

/**
 * Placeholder cho 1 cover manga (ratio 2:3 + CoverShape).
 */
@Composable
fun CoverSkeleton(modifier: Modifier = Modifier) {
    SkeletonShimmer(
        modifier =
            modifier
                .width(120.dp)
                .aspectRatio(2f / 3f)
                .background(MaterialTheme.colorScheme.surfaceContainer, CoverShape),
    )
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Shimmer — dark", showBackground = true)
@Composable
private fun SkeletonShimmerDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            SkeletonShimmer(Modifier.fillMaxWidth().height(16.dp))
            Spacer(Modifier.height(Dimens.SpacingSm))
            SkeletonShimmer(Modifier.fillMaxWidth(0.6f).height(12.dp))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Shimmer — light + covers", showBackground = true)
@Composable
private fun SkeletonShimmerLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        Row(Modifier.padding(Dimens.SpacingLg)) {
            CoverSkeleton()
            Spacer(Modifier.width(Dimens.SpacingSm))
            CoverSkeleton()
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Shimmer — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun SkeletonShimmer320Preview() {
    MyBooksLibraryTheme {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            SkeletonShimmer(Modifier.fillMaxWidth().height(16.dp))
            Spacer(Modifier.height(Dimens.SpacingSm))
            SkeletonShimmer(Modifier.fillMaxWidth(0.4f).height(12.dp))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Shimmer — landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun SkeletonShimmerLandscapePreview() {
    MyBooksLibraryTheme {
        Row(Modifier.padding(Dimens.SpacingLg)) {
            CoverSkeleton()
            Spacer(Modifier.width(Dimens.SpacingSm))
            CoverSkeleton()
            Spacer(Modifier.width(Dimens.SpacingSm))
            CoverSkeleton()
        }
    }
}
