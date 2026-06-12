@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Trạng thái rỗng — icon + tiêu đề + subtitle. Dùng khi list/grid không có dữ liệu.
 * Thay thế cả `DetailMessage` cũ (chỉ text) khi context cho phép.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconXl),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alphas.EmphasisMuted),
            )
            Spacer(Modifier.height(Dimens.SpacingLg))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Dimens.SpacingSm))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "EmptyState — dark + icon", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        EmptyState(
            title = "Chưa có truyện nào",
            subtitle = "Bắt đầu đọc để xây dựng bộ sưu tập",
            icon = Lucide.BookOpen,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "EmptyState — light no icon", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        EmptyState(title = "Không có kết quả với bộ lọc hiện tại")
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "EmptyState — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun EmptyState320Preview() {
    MyBooksLibraryTheme {
        EmptyState(
            title = "Chưa có truyện nào",
            subtitle = "Bắt đầu đọc để xây dựng bộ sưu tập của bạn ngay bây giờ",
            icon = Lucide.BookOpen,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "EmptyState — landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun EmptyStateLandscapePreview() {
    MyBooksLibraryTheme {
        EmptyState(
            title = "Chưa có truyện nào",
            subtitle = "Bắt đầu đọc để xây dựng bộ sưu tập",
            icon = Lucide.BookOpen,
        )
    }
}
