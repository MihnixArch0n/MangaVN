@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Wrapper FilterChip — spec:
 * selected = primaryContainer bg + onPrimaryContainer text;
 * unselected = surface + onSurfaceVariant; shape medium 12dp; outline 1dp.
 * Dùng cho SearchFilterSheet + LanguageFilterRow.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border =
            if (selected) {
                null
            } else {
                BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outline)
            },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppFilterChip — dark", showBackground = true)
@Composable
private fun AppFilterChipDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppFilterChip("Hành động", selected = true, onClick = {})
            AppFilterChip("Lãng mạn", selected = false, onClick = {})
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppFilterChip — light", showBackground = true)
@Composable
private fun AppFilterChipLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppFilterChip("Đang cập nhật", selected = true, onClick = {})
            AppFilterChip("Đã hoàn thành", selected = false, onClick = {})
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppFilterChip — 320dp VI dài", widthDp = 320, showBackground = true)
@Composable
private fun AppFilterChip320Preview() {
    MyBooksLibraryTheme {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppFilterChip("Phiêu lưu / Hành động", selected = false, onClick = {})
            AppFilterChip("Đang cập nhật", selected = true, onClick = {})
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppFilterChip — landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun AppFilterChipLandscapePreview() {
    MyBooksLibraryTheme {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppFilterChip("Tiếng Việt", selected = true, onClick = {})
            AppFilterChip("English", selected = false, onClick = {})
        }
    }
}
