@file:Suppress(
    "CyclomaticComplexMethod",
    "LongMethod",
    "MaxLineLength",
    "ktlint:standard:function-naming",
    "ktlint:standard:max-line-length",
)

package com.example.mybookslibrary.ui.screens.reader

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.viewmodel.ReaderPageAction
import com.example.mybookslibrary.ui.viewmodel.ReaderPageActionTarget
import com.example.mybookslibrary.ui.viewmodel.ReaderUiEffect
import com.example.mybookslibrary.util.storage.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private enum class ReaderToastType {
    SaveFailed,
    ShareFailed,
}

@Composable
internal fun ReaderEffectHandler(
    effects: SharedFlow<ReaderUiEffect>,
    listState: LazyListState,
    pagerState: PagerState,
    currentReadingMode: ReadingMode,
    onEvent: (ReaderEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageSaver = remember { ImageSaver(context) }
    val savedToFileText = appString(R.string.reader_saved_to_file)
    val savedToPicturesText = appString(R.string.reader_saved_to_pictures)
    val sharingText = appString(R.string.reader_sharing)
    val fallbackError = appString(R.string.error_unexpected)

    var errorMessageEvent by remember { mutableStateOf<String?>(null) }
    var errorToastType by remember { mutableStateOf(ReaderToastType.SaveFailed) }

    val saveFailedText = appString(R.string.reader_save_failed, errorMessageEvent ?: fallbackError)
    val shareFailedText = appString(R.string.reader_share_failed, errorMessageEvent ?: fallbackError)

    LaunchedEffect(errorMessageEvent, errorToastType) {
        if (errorMessageEvent != null) {
            val toastText =
                when (errorToastType) {
                    ReaderToastType.SaveFailed -> saveFailedText
                    ReaderToastType.ShareFailed -> shareFailedText
                }
            snackbarHostState.showSnackbar(toastText)
            errorMessageEvent = null
        }
    }

    var pendingSaveAsTarget by remember { mutableStateOf<ReaderPageActionTarget?>(null) }
    val saveAsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("image/*"),
        ) { uri ->
            val target = pendingSaveAsTarget ?: return@rememberLauncherForActivityResult
            pendingSaveAsTarget = null
            if (uri == null) {
                Timber.w("Reader save-as cancelled: page=%d url=%s", target.pageIndex + 1, target.pageUrl)
                return@rememberLauncherForActivityResult
            }

            scope.launch(Dispatchers.IO) {
                try {
                    Timber.d("Reader save-as start: page=%d url=%s uri=%s", target.pageIndex + 1, target.pageUrl, uri)
                    imageSaver.saveToUri(target.pageUrl, uri)
                    withContext(Dispatchers.Main) {
                        onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.SaveAs))
                    }
                    Timber.d("Reader save-as end: page=%d url=%s uri=%s", target.pageIndex + 1, target.pageUrl, uri)
                } catch (e: Exception) {
                    Timber.e(e, "Reader save-as failed: page=%d url=%s uri=%s", target.pageIndex + 1, target.pageUrl, uri)
                    withContext(Dispatchers.Main) {
                        onEvent(
                            ReaderEvent.PageActionFailed(
                                action = ReaderPageAction.SaveAs,
                                message = e.message ?: fallbackError,
                            ),
                        )
                    }
                }
            }
        }

    LaunchedEffect(effects, listState, pagerState, currentReadingMode) {
        var navigationJob: Job? = null
        effects.collect { effect ->
            when (effect) {
                is ReaderUiEffect.NavigateToPage -> {
                    navigationJob?.cancel()
                    navigationJob =
                        launch {
                            Timber.v(
                                "Reader page navigation start: targetPage=%d mode=%s",
                                effect.pageIndex,
                                currentReadingMode,
                            )
                            when (currentReadingMode) {
                                ReadingMode.VERTICAL -> listState.animateScrollToItem(effect.pageIndex)
                                ReadingMode.LTR,
                                ReadingMode.RTL,
                                -> pagerState.animateScrollToPage(effect.pageIndex)
                            }
                            Timber.v("Reader page navigation end: targetPage=%d", effect.pageIndex)
                        }
                }
                is ReaderUiEffect.QuickSavePage -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            Timber.d("Reader quick-save start: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                            imageSaver.quickSave(effect.target.pageUrl, effect.fileName)
                            withContext(Dispatchers.Main) {
                                onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.QuickSave))
                            }
                            Timber.d("Reader quick-save end: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                        } catch (e: Exception) {
                            Timber.e(e, "Reader quick-save failed: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                            withContext(Dispatchers.Main) {
                                onEvent(
                                    ReaderEvent.PageActionFailed(
                                        action = ReaderPageAction.QuickSave,
                                        message = e.message ?: fallbackError,
                                    ),
                                )
                            }
                        }
                    }
                }
                is ReaderUiEffect.SavePageAs -> {
                    Timber.d("Reader save-as selected: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                    pendingSaveAsTarget = effect.target
                    saveAsLauncher.launch("${effect.fileName}.${effect.extension}")
                }
                is ReaderUiEffect.SharePage -> {
                    scope.launch(Dispatchers.IO) {
                        try {
                            Timber.d("Reader share start: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                            val intent = imageSaver.shareImage(effect.target.pageUrl, effect.fileName)
                            withContext(Dispatchers.Main) {
                                context.startActivity(Intent.createChooser(intent, null))
                                onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.Share))
                            }
                            Timber.d("Reader share end: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                        } catch (e: Exception) {
                            Timber.e(e, "Reader share failed: page=%d url=%s", effect.target.pageIndex + 1, effect.target.pageUrl)
                            withContext(Dispatchers.Main) {
                                onEvent(
                                    ReaderEvent.PageActionFailed(
                                        action = ReaderPageAction.Share,
                                        message = e.message ?: fallbackError,
                                    ),
                                )
                            }
                        }
                    }
                }
                is ReaderUiEffect.ShowPageActionResult -> {
                    if (effect.errorMessage == null) {
                        val text =
                            when (effect.action) {
                                ReaderPageAction.QuickSave -> savedToPicturesText
                                ReaderPageAction.SaveAs -> savedToFileText
                                ReaderPageAction.Share -> sharingText
                            }
                        snackbarHostState.showSnackbar(text)
                    } else {
                        errorToastType =
                            when (effect.action) {
                                ReaderPageAction.Share -> ReaderToastType.ShareFailed
                                ReaderPageAction.QuickSave,
                                ReaderPageAction.SaveAs,
                                -> ReaderToastType.SaveFailed
                            }
                        errorMessageEvent = effect.errorMessage
                    }
                }
            }
        }
    }
}
