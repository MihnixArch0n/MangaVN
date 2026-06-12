@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Badge nhỏ (primary bg, pill, min 18dp) — hiển thị số count (filter, notification).
 * Thay Badge M3 trực tiếp (SearchScreen filter count).
 */
@Composable
fun StyledBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Box(
        modifier =
            modifier
                .sizeIn(minWidth = Dimens.IconSm, minHeight = Dimens.IconSm)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(horizontal = Dimens.SpacingXs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StyledBadge — dark", showBackground = true)
@Composable
private fun StyledBadgeDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            StyledBadge(3)
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StyledBadge — light 99+", showBackground = true)
@Composable
private fun StyledBadgeLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            StyledBadge(150)
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StyledBadge — single digit", showBackground = true)
@Composable
private fun StyledBadgeSinglePreview() {
    MyBooksLibraryTheme { StyledBadge(1) }
}
