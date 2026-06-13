@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString

/**
 * Chỉ báo trang nhỏ (Phase 4 PR-4a) hiển thị ở giữa-dưới khi đã ẩn overlay/bar —
 * bù cho bộ đếm trang trong bottom bar (vốn ẩn cùng overlay).
 *
 * @param currentPage Số trang hiện tại (1-based, đã clamp ở caller).
 * @param totalPages Tổng số trang của chương.
 */
@Composable
internal fun ReaderPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    val description = appString(R.string.cd_reader_page_indicator, currentPage, totalPages)
    Text(
        text = "$currentPage / $totalPages",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        modifier =
            modifier
                .clip(RoundedCornerShape(Dimens.SpacingXl))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs)
                .semantics { contentDescription = description },
    )
}

@Suppress("UnusedPrivateMember")
@Preview(name = "Reader page indicator", showBackground = true)
@Composable
private fun ReaderPageIndicatorPreview() {
    MyBooksLibraryTheme {
        ReaderPageIndicator(currentPage = 3, totalPages = 18)
    }
}
