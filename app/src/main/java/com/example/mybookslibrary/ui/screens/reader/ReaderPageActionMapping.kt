package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.ui.screens.reader.components.PageAction
import com.example.mybookslibrary.ui.viewmodel.ReaderPageAction

internal fun PageAction.toReaderPageAction(): ReaderPageAction =
    when (this) {
        PageAction.QuickSave -> ReaderPageAction.QuickSave
        PageAction.SaveAs -> ReaderPageAction.SaveAs
        PageAction.Share -> ReaderPageAction.Share
    }
