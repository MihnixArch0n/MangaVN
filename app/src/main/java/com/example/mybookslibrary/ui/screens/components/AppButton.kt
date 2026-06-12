@file:OptIn(ExperimentalTextApi::class)

package com.example.mybookslibrary.ui.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

enum class AppButtonStyle { Primary, Secondary, Text }

/**
 * Nút chuẩn — 3 biến thể (Primary filled / Secondary outlined / Text).
 * Dùng thay Button/OutlinedButton/TextButton trực tiếp để giữ nhất quán padding, shape, typography.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val haptic = com.example.mybookslibrary.ui.util.rememberAppHaptic()
    val hapticClick = {
        haptic.confirm()
        onClick()
    }
    val contentPadding = PaddingValues(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
    when (style) {
        AppButtonStyle.Primary ->
            Button(
                onClick = hapticClick,
                modifier = modifier,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                contentPadding = contentPadding,
            ) {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        AppButtonStyle.Secondary ->
            OutlinedButton(
                onClick = hapticClick,
                modifier = modifier,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                contentPadding = contentPadding,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        AppButtonStyle.Text ->
            TextButton(
                onClick = hapticClick,
                modifier = modifier,
                enabled = enabled,
                contentPadding = contentPadding,
            ) {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppButton — dark", showBackground = true)
@Composable
private fun AppButtonDarkPreview() {
    MyBooksLibraryTheme(darkTheme = true) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppButton("Đọc ngay", onClick = {}, style = AppButtonStyle.Primary)
            Spacer(Modifier.height(Dimens.SpacingSm))
            AppButton("Thử lại", onClick = {}, style = AppButtonStyle.Secondary)
            Spacer(Modifier.height(Dimens.SpacingSm))
            AppButton("Bỏ qua", onClick = {}, style = AppButtonStyle.Text)
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppButton — light", showBackground = true)
@Composable
private fun AppButtonLightPreview() {
    MyBooksLibraryTheme(darkTheme = false) {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppButton("Đọc ngay", onClick = {}, style = AppButtonStyle.Primary)
            Spacer(Modifier.height(Dimens.SpacingSm))
            AppButton("Thử lại", onClick = {}, style = AppButtonStyle.Secondary)
            Spacer(Modifier.height(Dimens.SpacingSm))
            AppButton("Bỏ qua", onClick = {}, style = AppButtonStyle.Text)
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(name = "AppButton — 320dp", widthDp = 320, showBackground = true)
@Composable
private fun AppButton320Preview() {
    MyBooksLibraryTheme {
        Column(Modifier.padding(Dimens.SpacingLg)) {
            AppButton("Đã có tài khoản? Đăng nhập", onClick = {})
        }
    }
}
