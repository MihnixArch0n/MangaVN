package com.example.mybookslibrary.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChartNoAxesColumn
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.User
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.ProfileViewModel

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onReadingHistoryClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.profile_title)) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Lucide.ArrowLeft, contentDescription = appString(R.string.cd_back))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = Dimens.ScreenPaddingCompact,
                vertical = Dimens.SpacingLg,
            ),
        ) {
            item {
                ProfileHeader(
                    username = state.username,
                    displayName = state.displayName,
                    avatarUri = state.avatarUri,
                    onEditClick = onEditProfileClick,
                )
            }
            item { Spacer(Modifier.height(Dimens.SpacingXxl)) }
            item {
                ProfileStatsRow(
                    bookmarks = state.bookmarkCount,
                    mangaRead = state.mangaReadCount,
                    chapters = state.chaptersCompleted,
                )
            }
            item { Spacer(Modifier.height(Dimens.SpacingXxl)) }
            item {
                ProfileMenuItem(
                    icon = Lucide.Clock,
                    label = appString(R.string.profile_reading_history),
                    onClick = onReadingHistoryClick,
                )
            }
            item { Spacer(Modifier.height(Dimens.SpacingMd)) }
            item {
                ProfileMenuItem(
                    icon = Lucide.ChartNoAxesColumn,
                    label = appString(R.string.profile_statistics),
                    onClick = onStatisticsClick,
                )
            }
            item { Spacer(Modifier.height(Dimens.SpacingMd)) }
            item {
                ProfileMenuItem(
                    icon = Lucide.Download,
                    label = appString(R.string.profile_downloads),
                    onClick = onDownloadsClick,
                )
            }
            item { Spacer(Modifier.height(Dimens.SpacingMd)) }
            item {
                ProfileMenuItem(
                    icon = Lucide.Settings,
                    label = appString(R.string.profile_settings),
                    onClick = onSettingsClick,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    displayName: String,
    avatarUri: String,
    onEditClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.AvatarMd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUri.isNotBlank()) {
                coil3.compose.AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (username.isNotBlank()) {
                Text(
                    text = (displayName.ifBlank { username }).first().uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    Lucide.User,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.AvatarSm),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(Dimens.SpacingMd))
        Text(
            text = displayName.ifBlank { username.ifBlank { "—" } },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (username.isNotBlank() && displayName.isNotBlank()) {
            Text(
                text = "@$username",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Dimens.SpacingSm))
        androidx.compose.material3.TextButton(onClick = onEditClick) {
            Text(appString(R.string.profile_edit))
        }
    }
}

@Composable
private fun ProfileStatsRow(bookmarks: Int, mangaRead: Int, chapters: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(count = bookmarks, label = appString(R.string.profile_stat_bookmarks))
        StatItem(count = mangaRead, label = appString(R.string.profile_stat_manga_read))
        StatItem(count = chapters, label = appString(R.string.profile_stat_chapters))
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimens.SpacingXs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPaddingCompact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.IconDefault),
            )
            Spacer(Modifier.weight(1f).padding(start = Dimens.SpacingLg))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(8f),
            )
            Icon(
                Lucide.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.IconMd),
            )
        }
    }
}
