@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString

/**
 * Tiêu đề section (title + "Xem tất cả") — dùng thống nhất trên Discover shelves, Library, Search.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPaddingCompact, vertical = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onToggle != null) {
            Text(
                if (expanded) appString(R.string.action_collapse) else appString(R.string.action_see_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onToggle),
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "SectionHeader — dark + toggle", showBackground = true)
@Composable
private fun SectionHeaderDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        SectionHeader(title = "Đang thịnh hành", onToggle = {})
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "SectionHeader — light no toggle", showBackground = true)
@Composable
private fun SectionHeaderLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        SectionHeader(title = "Mới cập nhật gần đây nhất")
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "SectionHeader — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun SectionHeader320Preview() {
    MyBooksLibraryTheme {
        SectionHeader(title = "Thể loại phổ biến nhất trong tuần này", onToggle = {})
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "SectionHeader — landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun SectionHeaderLandscapePreview() {
    MyBooksLibraryTheme {
        SectionHeader(title = "Mới cập nhật", expanded = true, onToggle = {})
    }
}
