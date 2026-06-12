package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.User
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.navigation.LucideSearchIcon
import com.example.mybookslibrary.ui.screens.components.DiscoverSkeletonLoading
import com.example.mybookslibrary.ui.screens.components.ErrorState
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.ui.screens.components.StyledDropdownMenu
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorialTopBar(
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val menuExpanded = remember { mutableStateOf(false) }

    TopAppBar(
        title = { BrandTitle() },
        navigationIcon = {
            DiscoverNavigationMenu(
                expanded = menuExpanded.value,
                onExpandedChange = { menuExpanded.value = it },
                onLibraryClick = onLibraryClick,
                onHistoryClick = onHistoryClick,
                onSettingsClick = onSettingsClick,
            )
        },
        actions = {
            DiscoverTopBarActions(
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
            ),
    )
}

@Composable
private fun BrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Lucide.BookOpen,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconDefault),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(Dimens.SpacingSm))
        Text(
            appString(R.string.brand_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DiscoverNavigationMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLibraryClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Lucide.Menu, appString(R.string.cd_menu), tint = MaterialTheme.colorScheme.primary)
        }
        StyledDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DiscoverMenuItem(
                label = appString(R.string.nav_library),
                icon = Lucide.BookOpen,
                onClick = {
                    onExpandedChange(false)
                    onLibraryClick()
                },
            )
            DiscoverMenuItem(
                label = appString(R.string.reading_history_title),
                icon = Lucide.Clock,
                onClick = {
                    onExpandedChange(false)
                    onHistoryClick()
                },
            )
            DiscoverMenuItem(
                label = appString(R.string.settings_title),
                icon = Lucide.Settings,
                onClick = {
                    onExpandedChange(false)
                    onSettingsClick()
                },
            )
        }
    }
}

@Composable
private fun DiscoverMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
    )
}

@Composable
private fun DiscoverTopBarActions(onSearchClick: () -> Unit, onProfileClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) {
        com.example.mybookslibrary.data.local.UserPreferencesDataStore(
            context.userPreferencesDataStore,
        )
    }
    val avatarUri by prefs.observeAvatarUri()
        .collectAsStateWithLifecycle(initialValue = "")

    IconButton(onClick = onSearchClick) {
        Icon(LucideSearchIcon, appString(R.string.cd_search), tint = MaterialTheme.colorScheme.primary)
    }
    IconButton(onClick = onProfileClick) {
        Box(
            modifier =
                Modifier
                    .size(Dimens.IconLg)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUri.isNotBlank()) {
                coil3.compose.AsyncImage(
                    model = avatarUri,
                    contentDescription = appString(R.string.cd_profile),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Lucide.User,
                    appString(R.string.cd_profile),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimens.IconSm),
                )
            }
        }
    }
}

@Composable
internal fun DiscoverLoadingState(modifier: Modifier = Modifier) {
    DiscoverSkeletonLoading(modifier)
}

@Composable
internal fun DiscoverErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    ErrorState(
        message = appString(R.string.discover_error_title),
        modifier = modifier,
        onRetry = onRetry,
    )
}
