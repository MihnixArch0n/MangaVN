package com.example.mybookslibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.util.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorialTopBar(
    onSearchClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val menuExpanded = remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            BrandTitle()
        },
        navigationIcon = {
            DiscoverNavigationMenu(
                expanded = menuExpanded.value,
                onExpandedChange = { menuExpanded.value = it },
                onLibraryClick = onLibraryClick,
                onProfileClick = onProfileClick,
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
    Text(
        appString(R.string.brand_name),
        style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun DiscoverNavigationMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLibraryClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Filled.Menu, appString(R.string.cd_menu), tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DiscoverMenuItem(
                text = appString(R.string.nav_library),
                icon = { Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onExpandedChange(false)
                    onLibraryClick()
                },
            )
            DiscoverMenuItem(
                text = appString(R.string.settings_title),
                icon = { Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    onExpandedChange(false)
                    onProfileClick()
                },
            )
        }
    }
}

@Composable
private fun DiscoverMenuItem(text: String, icon: @Composable () -> Unit, onClick: () -> Unit,) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        leadingIcon = icon,
    )
}

@Composable
private fun DiscoverTopBarActions(onSearchClick: () -> Unit, onProfileClick: () -> Unit,) {
    IconButton(onClick = onSearchClick) {
        Icon(Icons.Filled.Search, appString(R.string.cd_search), tint = MaterialTheme.colorScheme.primary)
    }
    IconButton(onClick = onProfileClick) {
        Box(
            modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Person,
                appString(R.string.cd_profile),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun DiscoverLoadingState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun DiscoverErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit,) {
    Box(
        modifier = modifier.padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = appString(R.string.discover_error_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = appString(R.string.discover_error_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
            ) {
                Text(
                    text = appString(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
