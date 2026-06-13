package com.example.mybookslibrary.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.adaptiveMaxWidth
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.navigation.LocalBottomNavPadding
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.BackupRestoreResult
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel
import com.example.mybookslibrary.util.isOpenLinksGranted
import com.example.mybookslibrary.util.openAppLinkSettings

@Suppress("unused", "CyclomaticComplexMethod", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bottomNavPadding = LocalBottomNavPadding.current
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val backupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            context.contentResolver.openOutputStream(uri)?.let { viewModel.backupLibrary(it) }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            context.contentResolver.openInputStream(uri)?.let { viewModel.restoreLibrary(it) }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Lucide.ArrowLeft, contentDescription = appString(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.widthIn(max = adaptiveMaxWidth()),
            contentPadding =
                PaddingValues(
                    start = Dimens.ScreenPaddingCompact,
                    end = Dimens.ScreenPaddingCompact,
                    top = Dimens.SpacingLg,
                    bottom = bottomNavPadding + Dimens.SpacingLg,
                ),
        ) {
            item { SettingsSectionLabel(appString(R.string.settings_section_appearance)) }
            item {
                val themeLabel =
                    when (uiState.themeMode) {
                        "light" -> appString(R.string.settings_theme_light)
                        "dark" -> appString(R.string.settings_theme_dark)
                        else -> appString(R.string.settings_theme_system)
                    }
                val langLabel =
                    if (uiState.language ==
                        "vi"
                    ) {
                        appString(R.string.settings_language_vietnamese)
                    } else {
                        appString(R.string.settings_language_english)
                    }
                SettingsCard {
                    SettingsRow(appString(R.string.settings_dark_mode), themeLabel) { viewModel.cycleThemeMode() }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_language), langLabel) {
                        viewModel.setLanguage(if (uiState.language == "vi") "en" else "vi")
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_reading)) }
            item {
                val qualityLabel =
                    if (uiState.quality ==
                        "data"
                    ) {
                        appString(R.string.settings_quality_original)
                    } else {
                        appString(R.string.settings_quality_data_saver)
                    }
                val keepScreenOnLabel =
                    if (uiState.keepScreenOn) appString(R.string.settings_on) else appString(R.string.settings_off)
                val volumeKeyLabel =
                    if (uiState.volumeKeyNav) appString(R.string.settings_on) else appString(R.string.settings_off)
                val autoAdvanceLabel =
                    if (uiState.autoAdvance) appString(R.string.settings_on) else appString(R.string.settings_off)
                val autoDownloadNextLabel =
                    if (uiState.autoDownloadNext) appString(R.string.settings_on) else appString(R.string.settings_off)
                val newChapterNotificationsLabel =
                    if (uiState.newChapterNotifications) {
                        appString(R.string.settings_on)
                    } else {
                        appString(R.string.settings_off)
                    }
                SettingsCard {
                    SettingsRow(appString(R.string.settings_image_quality), qualityLabel) { viewModel.toggleQuality() }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_keep_screen_on), keepScreenOnLabel) {
                        viewModel.toggleKeepScreenOn()
                    }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_volume_key_nav), volumeKeyLabel) {
                        viewModel.toggleVolumeKeyNav()
                    }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_auto_advance), autoAdvanceLabel) {
                        viewModel.toggleAutoAdvance()
                    }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_auto_download_next), autoDownloadNextLabel) {
                        viewModel.toggleAutoDownloadNext()
                    }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_new_chapter_notifications), newChapterNotificationsLabel) {
                        viewModel.toggleNewChapterNotifications()
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_storage)) }
            item {
                val cacheSubtitle =
                    if (uiState.cacheCleared) {
                        appString(
                            R.string.settings_cache_cleared,
                        )
                    } else {
                        appString(R.string.settings_cache_subtitle)
                    }
                val deleteAfterReadLabel =
                    if (uiState.deleteAfterRead) appString(R.string.settings_on) else appString(R.string.settings_off)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_clear_cache), cacheSubtitle) { viewModel.clearImageCache() }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_delete_after_read), deleteAfterReadLabel) {
                        viewModel.toggleDeleteAfterRead()
                    }
                    if (uiState.deleteAfterRead) {
                        SettingsDivider()
                        SettingsStepperRow(
                            title = appString(R.string.settings_delete_after_read_keep),
                            value = uiState.deleteAfterReadKeep,
                            min = SettingsViewModel.KEEP_MIN,
                            max = SettingsViewModel.KEEP_MAX,
                            onChange = { viewModel.setDeleteAfterReadKeep(it) },
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_data)) }
            item {
                val syncSub = when {
                    uiState.isSyncing -> appString(R.string.settings_sync_syncing)
                    uiState.syncSuccess == true -> appString(R.string.settings_sync_success)
                    uiState.syncSuccess == false -> appString(R.string.settings_sync_failed)
                    else -> appString(R.string.settings_sync_subtitle)
                }
                val backupSub =
                    when (val r = uiState.backupResult) {
                        is BackupRestoreResult.Success -> appString(R.string.settings_backup_success, r.count)
                        is BackupRestoreResult.Failure -> appString(R.string.settings_backup_failed, r.message)
                        null -> appString(R.string.settings_backup_subtitle)
                    }
                val restoreSub =
                    when (val r = uiState.restoreResult) {
                        is BackupRestoreResult.Success -> appString(R.string.settings_restore_success, r.count)
                        is BackupRestoreResult.Failure -> appString(R.string.settings_restore_failed, r.message)
                        null -> appString(R.string.settings_restore_subtitle)
                    }
                SettingsCard {
                    if (!uiState.isGuest) {
                        SettingsRow(appString(R.string.settings_sync), syncSub) {
                            if (!uiState.isSyncing) viewModel.forceSync()
                        }
                        SettingsDivider()
                    }
                    SettingsRow(appString(R.string.settings_backup), backupSub) {
                        backupLauncher.launch("kanso_library_backup.json")
                    }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_restore), restoreSub) {
                        restoreLauncher.launch(arrayOf("application/json"))
                    }
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_links)) }
            item {
                SettingsCard {
                    OpenLinksRow(context = context)
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
            }

            item { SettingsSectionLabel(appString(R.string.settings_about)) }
            item {
                val versionName =
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
                    } catch (_: Exception) {
                        "—"
                    }
                SettingsCard {
                    SettingsRow(
                        title = appString(R.string.about_version, versionName),
                        subtitle = appString(R.string.settings_about_subtitle),
                        onClick = {},
                    )
                }
                Spacer(Modifier.height(Dimens.SpacingXl))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_account)) }
            item {
                var showDeleteAccountDialog by remember { mutableStateOf(false) }

                val signOutTitle =
                    if (uiState.signedOut) {
                        appString(R.string.settings_signed_out)
                    } else {
                        appString(R.string.settings_sign_out)
                    }
                val signOutSub =
                    if (uiState.signedOut) {
                        appString(
                            R.string.settings_signed_out_subtitle,
                        )
                    } else {
                        appString(R.string.settings_sign_out_subtitle)
                    }
                SettingsCard {
                    if (!uiState.isGuest) {
                        SettingsRow(
                            title = appString(R.string.settings_change_password),
                            subtitle = appString(R.string.settings_change_password_subtitle),
                            onClick = onChangePasswordClick,
                        )
                        SettingsDivider()
                    }
                    SettingsRow(
                        title = signOutTitle,
                        subtitle = signOutSub,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showSignOutConfirm = true },
                    )
                    if (!uiState.isGuest) {
                        SettingsDivider()
                        SettingsRow(
                            title = appString(R.string.settings_delete_account),
                            subtitle = appString(R.string.settings_delete_account_subtitle),
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteAccountDialog = true },
                        )
                    }
                }

                if (showDeleteAccountDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDeleteAccountDialog = false },
                        title = { Text(appString(R.string.settings_delete_account)) },
                        text = { Text(appString(R.string.settings_delete_account_desc)) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showDeleteAccountDialog = false
                                    viewModel.deleteAccount()
                                }
                            ) {
                                Text(
                                    text = appString(R.string.settings_delete_account_confirm),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showDeleteAccountDialog = false }
                            ) {
                                Text(appString(R.string.action_cancel))
                            }
                        }
                    )
                }
            }
        }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(appString(R.string.sign_out_confirm_title)) },
            text = { Text(appString(R.string.sign_out_confirm_body)) },
            confirmButton = {
                com.example.mybookslibrary.ui.screens.components.AppButton(
                    text = appString(R.string.settings_sign_out),
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                    },
                )
            },
            dismissButton = {
                com.example.mybookslibrary.ui.screens.components.AppButton(
                    text = appString(R.string.action_cancel),
                    onClick = { showSignOutConfirm = false },
                    style = com.example.mybookslibrary.ui.screens.components.AppButtonStyle.Text,
                )
            },
        )
    }

}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Dimens.SpacingSm, start = Dimens.SpacingXs),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpacingXl, vertical = Dimens.SpacingLg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Spacer(Modifier.height(Dimens.SpacingXxs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsStepperRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingXl, vertical = Dimens.SpacingMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(value - 1) }, enabled = value > min) {
                Icon(Lucide.ChevronDown, appString(R.string.cd_decrease), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { onChange(value + 1) }, enabled = value < max) {
                Icon(Lucide.ChevronUp, appString(R.string.cd_increase), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(Dimens.BorderThin)
            .padding(horizontal = Dimens.SpacingXl)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * Row that shows the "Open Supported Links" permission status.
 * Tapping Grant opens the system App Settings → Open by default screen so the
 * user can enable the deep-link permission manually.
 * The granted state is re-evaluated every time the composable enters composition
 * (i.e. after returning from system Settings).
 */
@Composable
private fun OpenLinksRow(context: android.content.Context) {
    // Re-check on every composition so it reflects changes made in system Settings.
    var granted by remember { mutableStateOf(isOpenLinksGranted(context)) }

    // Refresh when the user comes back from system Settings.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { /* no-op */ }
    }
    // Use lifecycle observer to re-check when Activity resumes.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                granted = isOpenLinksGranted(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingXl, vertical = Dimens.SpacingMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = Dimens.SpacingMd)) {
            Text(
                appString(R.string.settings_open_links),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.SpacingXxs))
            Text(
                appString(R.string.settings_open_links_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = { openAppLinkSettings(context) },
            enabled = !granted,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm,
            ),
        ) {
            Text(
                text = if (granted) {
                    appString(R.string.settings_open_links_granted)
                } else {
                    appString(R.string.settings_open_links_grant)
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
