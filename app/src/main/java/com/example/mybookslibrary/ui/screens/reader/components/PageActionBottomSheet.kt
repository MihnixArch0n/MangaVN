@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.appString

/**
 * Exhaustive page-action contract emitted by [PageActionBottomSheet].
 *
 * The caller typically receives one of these values in [ReaderScreen] and
 * maps it to the corresponding storage/share operation.
 */
sealed interface PageAction {
    /** Request an immediate gallery save through MediaStore. */
    data object QuickSave : PageAction

    /** Request a Storage Access Framework destination picker. */
    data object SaveAs : PageAction

    /** Request a system share action for the current page image. */
    data object Share : PageAction
}

/**
 * Material 3 [ModalBottomSheet] that exposes page-level actions for the reader.
 *
 * The sheet only forwards the selected [PageAction] to the caller and then
 * requests dismissal; it does not perform any storage or sharing work itself.
 *
 * @param onDismiss Called when the sheet should be hidden.
 * @param onAction Called with the selected [PageAction].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageActionBottomSheet(
    onDismiss: () -> Unit,
    onAction: (PageAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionItem(
                icon = Icons.Outlined.Download,
                label = appString(R.string.reader_action_quick_save),
                onClick = {
                    onAction(PageAction.QuickSave)
                    onDismiss()
                },
            )
            ActionItem(
                icon = Icons.Outlined.Save,
                label = appString(R.string.reader_action_save_as),
                onClick = {
                    onAction(PageAction.SaveAs)
                    onDismiss()
                },
            )
            ActionItem(
                icon = Icons.Outlined.Share,
                label = appString(R.string.reader_action_share),
                onClick = {
                    onAction(PageAction.Share)
                    onDismiss()
                },
            )
        }
    }
}

@Preview(name = "Page Action Sheet", showBackground = true)
@Composable
private fun PageActionBottomSheetPreview() {
    MyBooksLibraryTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            PageActionBottomSheet(
                onDismiss = { },
                onAction = { },
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
