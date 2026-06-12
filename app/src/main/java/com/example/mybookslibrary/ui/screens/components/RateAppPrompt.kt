package com.example.mybookslibrary.ui.screens.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.ui.util.appString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun RateAppPrompt() {
    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesDataStore(context.userPreferencesDataStore) }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val dismissed = prefs.observeRateAppDismissed().first()
        if (dismissed) return@LaunchedEffect

        val firstOpen = prefs.getFirstOpenTime()
        val daysSinceFirstOpen = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - firstOpen,
        )
        if (daysSinceFirstOpen >= MIN_DAYS_BEFORE_PROMPT) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                scope.launch { prefs.setRateAppDismissed(true) }
            },
            title = { Text(appString(R.string.rate_app_title)) },
            text = { Text(appString(R.string.rate_app_body)) },
            confirmButton = {
                AppButton(
                    text = appString(R.string.rate_app_rate),
                    onClick = {
                        showDialog = false
                        scope.launch { prefs.setRateAppDismissed(true) }
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}"),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                )
            },
            dismissButton = {
                AppButton(
                    text = appString(R.string.rate_app_later),
                    onClick = {
                        showDialog = false
                    },
                    style = AppButtonStyle.Text,
                )
            },
        )
    }
}

private const val MIN_DAYS_BEFORE_PROMPT = 7L
