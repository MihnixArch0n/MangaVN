@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.screens.components.AppButton
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString

/**
 * Trang chuyển tiếp cuối chương (Phase 4 PR-2a) — hiển thị như trang ảo cuối reader.
 *
 * Nền dùng `surface` phủ kín để chữ đọc được bất kể màu nền reader (đen/trắng/xám). Khi còn
 * chương sau: hiện tên chương kế + nút đọc tiếp; khi hết: báo đã là chương mới nhất.
 *
 * @param nextChapterTitle Tên chương kế, null nếu đây là chương mới nhất.
 * @param onNextClick Gọi khi bấm "Đọc chương sau".
 */
@Composable
internal fun ReaderTransitionView(
    nextChapterTitle: String?,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd),
            modifier = Modifier.padding(Dimens.SpacingXl),
        ) {
            Text(
                text = appString(R.string.reader_transition_finished),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (nextChapterTitle != null) {
                Text(
                    text = appString(R.string.reader_transition_next, nextChapterTitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                AppButton(
                    text = appString(R.string.reader_transition_action_next),
                    onClick = onNextClick,
                    modifier = Modifier.padding(top = Dimens.SpacingMd),
                )
            } else {
                Text(
                    text = appString(R.string.reader_transition_no_next),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Reader transition — có chương sau", showBackground = true)
@Composable
private fun ReaderTransitionViewPreview() {
    MyBooksLibraryTheme {
        ReaderTransitionView(
            nextChapterTitle = "Vol.2 Ch.13",
            onNextClick = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Reader transition — hết chương", showBackground = true)
@Composable
private fun ReaderTransitionViewLatestPreview() {
    MyBooksLibraryTheme {
        ReaderTransitionView(
            nextChapterTitle = null,
            onNextClick = {},
        )
    }
}
