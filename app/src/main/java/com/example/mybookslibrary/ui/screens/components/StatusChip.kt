@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.theme.statusColors
import com.example.mybookslibrary.ui.util.appString

/**
 * Chip trạng thái (Đang đọc / Hoàn thành / Yêu thích) — pill shape, 10% alpha nền.
 * Dùng thống nhất ở Library grid + bất kỳ nơi nào hiện trạng thái reading.
 */
@Composable
fun StatusChip(
    status: LibraryStatus,
    modifier: Modifier = Modifier,
) {
    val label =
        when (status) {
            LibraryStatus.READING -> appString(R.string.status_reading)
            LibraryStatus.COMPLETED -> appString(R.string.status_completed)
            LibraryStatus.FAVORITE -> appString(R.string.status_favorite)
        }
    val color =
        when (status) {
            LibraryStatus.READING -> MaterialTheme.colorScheme.tertiary
            LibraryStatus.COMPLETED -> MaterialTheme.statusColors.success
            LibraryStatus.FAVORITE -> MaterialTheme.statusColors.favorite
        }
    Box(
        modifier =
            modifier
                .background(color.copy(alpha = Alphas.ContainerTint), CircleShape)
                .padding(horizontal = Dimens.SpacingSm + Dimens.SpacingXs, vertical = Dimens.SpacingXs),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StatusChip — dark", showBackground = true)
@Composable
private fun StatusChipDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column {
            LibraryStatus.entries.forEach { StatusChip(it, Modifier.padding(Dimens.SpacingXs)) }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StatusChip — light", showBackground = true)
@Composable
private fun StatusChipLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        Column {
            LibraryStatus.entries.forEach { StatusChip(it, Modifier.padding(Dimens.SpacingXs)) }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StatusChip — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun StatusChip320Preview() {
    MyBooksLibraryTheme {
        Column {
            LibraryStatus.entries.forEach { StatusChip(it, Modifier.padding(Dimens.SpacingXs)) }
        }
    }
}
