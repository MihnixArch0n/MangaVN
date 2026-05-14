package com.example.mybookslibrary.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.BackupRestoreResult
import com.example.mybookslibrary.ui.viewmodel.SettingsViewModel

@Suppress("unused")
@Composable
fun SettingScreenContent(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.let { viewModel.backupLibrary(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.let { viewModel.restoreLibrary(it) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(appString(R.string.settings_title), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(32.dp))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_appearance)) }
            item {
                val themeLabel = when (uiState.themeMode) {
                    "light" -> appString(R.string.settings_theme_light)
                    "dark" -> appString(R.string.settings_theme_dark)
                    else -> appString(R.string.settings_theme_system)
                }
                val langLabel = if (uiState.language == "vi") appString(R.string.settings_language_vietnamese) else appString(R.string.settings_language_english)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_dark_mode), themeLabel) { viewModel.cycleThemeMode() }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_language), langLabel) {
                        viewModel.setLanguage(if (uiState.language == "vi") "en" else "vi")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_reading)) }
            item {
                val qualityLabel = if (uiState.quality == "data") appString(R.string.settings_quality_original) else appString(R.string.settings_quality_data_saver)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_image_quality), qualityLabel) { viewModel.toggleQuality() }
                }
                Spacer(Modifier.height(24.dp))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_storage)) }
            item {
                val cacheSubtitle = if (uiState.cacheCleared) appString(R.string.settings_cache_cleared) else appString(R.string.settings_cache_subtitle)
                SettingsCard {
                    SettingsRow(appString(R.string.settings_clear_cache), cacheSubtitle) { viewModel.clearImageCache() }
                }
                Spacer(Modifier.height(24.dp))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_data)) }
            item {
                val backupSub = when (val r = uiState.backupResult) {
                    is BackupRestoreResult.Success -> appString(R.string.settings_backup_success, r.count)
                    is BackupRestoreResult.Failure -> appString(R.string.settings_backup_failed, r.message)
                    null -> appString(R.string.settings_backup_subtitle)
                }
                val restoreSub = when (val r = uiState.restoreResult) {
                    is BackupRestoreResult.Success -> appString(R.string.settings_restore_success, r.count)
                    is BackupRestoreResult.Failure -> appString(R.string.settings_restore_failed, r.message)
                    null -> appString(R.string.settings_restore_subtitle)
                }
                SettingsCard {
                    SettingsRow(appString(R.string.settings_backup), backupSub) { backupLauncher.launch("kanso_library_backup.json") }
                    SettingsDivider()
                    SettingsRow(appString(R.string.settings_restore), restoreSub) { restoreLauncher.launch(arrayOf("application/json")) }
                }
                Spacer(Modifier.height(24.dp))
            }

            item { SettingsSectionLabel(appString(R.string.settings_section_account)) }
            item {
                val signOutTitle = if (uiState.signedOut) appString(R.string.settings_signed_out) else appString(R.string.settings_sign_out)
                val signOutSub = if (uiState.signedOut) appString(R.string.settings_signed_out_subtitle) else appString(R.string.settings_sign_out_subtitle)
                SettingsCard {
                    SettingsRow(signOutTitle, signOutSub, MaterialTheme.colorScheme.tertiary) { viewModel.signOut() }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 20.dp)
        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
}


