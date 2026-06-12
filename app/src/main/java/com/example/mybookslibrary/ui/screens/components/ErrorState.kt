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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Trạng thái lỗi — icon + thông báo + nút thử lại. Dùng khi fetch thất bại.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Lucide.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconXl),
            tint = MaterialTheme.colorScheme.error.copy(alpha = Alphas.EmphasisHigh),
        )
        Spacer(Modifier.height(Dimens.SpacingLg))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(Dimens.SpacingLg))
            AppButton(
                text = "Thử lại",
                onClick = onRetry,
                style = AppButtonStyle.Secondary,
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorState — dark + retry", showBackground = true)
@Composable
private fun ErrorStateDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        ErrorState(
            message = "Không tải được thể loại — kiểm tra kết nối",
            onRetry = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorState — light no retry", showBackground = true)
@Composable
private fun ErrorStateLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        ErrorState(message = "Đã có lỗi xảy ra")
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorState — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun ErrorState320Preview() {
    MyBooksLibraryTheme {
        ErrorState(
            message = "Không tải được thể loại — kiểm tra kết nối mạng của bạn",
            onRetry = {},
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorState — landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun ErrorStateLandscapePreview() {
    MyBooksLibraryTheme {
        ErrorState(message = "Không tải được thể loại", onRetry = {})
    }
}
