@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Badge rating — tertiary (gold) star + số; dùng trên Detail hero + card.
 */
@Composable
fun RatingBadge(
    rating: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = Alphas.EmphasisVeryHigh),
                    MaterialTheme.shapes.small,
                )
                .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Lucide.Star,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconXs),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            rating,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(start = Dimens.SpacingXs),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "RatingBadge — dark", showBackground = true)
@Composable
private fun RatingBadgeDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            RatingBadge("8.5")
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "RatingBadge — light", showBackground = true)
@Composable
private fun RatingBadgeLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            RatingBadge("9.2")
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "RatingBadge — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun RatingBadge320Preview() {
    MyBooksLibraryTheme { RatingBadge("7.8") }
}
