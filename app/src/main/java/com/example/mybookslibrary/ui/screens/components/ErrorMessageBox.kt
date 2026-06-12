@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.background
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
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

/**
 * Hộp thông báo lỗi inline — container + icon + text.
 * Dùng cho lỗi auth (Login/Register) thay Text trơ.
 */
@Composable
fun ErrorMessageBox(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.shapes.small,
                )
                .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Lucide.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconSm),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(start = Dimens.SpacingSm),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorMessageBox — dark", showBackground = true)
@Composable
private fun ErrorMessageBoxDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        ErrorMessageBox("Tên đăng nhập và mật khẩu không được để trống")
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorMessageBox — light", showBackground = true)
@Composable
private fun ErrorMessageBoxLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        ErrorMessageBox("Tên đăng nhập hoặc mật khẩu không đúng")
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorMessageBox — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun ErrorMessageBox320Preview() {
    MyBooksLibraryTheme {
        ErrorMessageBox("Không tải được thể loại — kiểm tra kết nối")
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "ErrorMessageBox — landscape", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun ErrorMessageBoxLandscapePreview() {
    MyBooksLibraryTheme {
        ErrorMessageBox("Tên đăng nhập và mật khẩu không được để trống")
    }
}
