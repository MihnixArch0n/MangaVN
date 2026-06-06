package com.example.mybookslibrary.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class Resource(
        @StringRes val id: Int,
    ) : UiText()

    data class Dynamic(
        val value: String,
    ) : UiText()

    @Composable
    fun asString(): String =
        when (this) {
            is Resource -> stringResource(id)
            is Dynamic -> value
        }
}
