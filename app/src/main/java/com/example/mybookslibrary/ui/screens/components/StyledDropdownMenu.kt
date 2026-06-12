@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * DropdownMenu thống nhất — surfaceContainerHigh + shape medium.
 * Thay 2 chỗ dùng DropdownMenu trực tiếp (ChapterComponents, DiscoverChromeComponents).
 */
@Composable
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = MenuDefaults.TonalElevation,
        content = content,
    )
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StyledDropdownMenu — dark", showBackground = true)
@Composable
private fun StyledDropdownMenuDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        StyledDropdownMenu(expanded = true, onDismissRequest = {}) {
            androidx.compose.material3.DropdownMenuItem(
                text = { androidx.compose.material3.Text("Mục 1") },
                onClick = {},
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { androidx.compose.material3.Text("Mục 2") },
                onClick = {},
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StyledDropdownMenu — light", showBackground = true)
@Composable
private fun StyledDropdownMenuLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        StyledDropdownMenu(expanded = true, onDismissRequest = {}) {
            androidx.compose.material3.DropdownMenuItem(
                text = { androidx.compose.material3.Text("Sắp xếp") },
                onClick = {},
            )
        }
    }
}
