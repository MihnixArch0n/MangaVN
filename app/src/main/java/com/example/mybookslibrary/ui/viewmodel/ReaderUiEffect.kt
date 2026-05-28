package com.example.mybookslibrary.ui.viewmodel

sealed interface ReaderUiEffect {
    data class NavigateToPage(val pageIndex: Int) : ReaderUiEffect
    data class QuickSavePage(
        val target: ReaderPageActionTarget,
        val fileName: String
    ) : ReaderUiEffect
    data class SavePageAs(
        val target: ReaderPageActionTarget,
        val fileName: String,
        val extension: String
    ) : ReaderUiEffect
    data class SharePage(
        val target: ReaderPageActionTarget,
        val fileName: String
    ) : ReaderUiEffect
    data class ShowPageActionResult(
        val action: ReaderPageAction,
        val errorMessage: String? = null
    ) : ReaderUiEffect
}
