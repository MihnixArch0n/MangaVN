package com.example.mybookslibrary.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.userPreferencesDataStore
import com.example.mybookslibrary.ui.navigation.LocalSnackbarHostState
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.adaptiveFormMaxWidth
import com.example.mybookslibrary.ui.util.appString
import kotlinx.coroutines.launch

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesDataStore(context.userPreferencesDataStore) }
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val savedMsg = appString(R.string.profile_saved)

    val currentName by prefs.observeDisplayName()
        .collectAsStateWithLifecycle(initialValue = "")
    val currentAvatar by prefs.observeAvatarUri()
        .collectAsStateWithLifecycle(initialValue = "")

    var displayName by remember(currentName) { mutableStateOf(currentName) }
    var avatarUri by remember(currentAvatar) { mutableStateOf(currentAvatar) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            avatarUri = it.toString()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.profile_edit_title)) },
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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = adaptiveFormMaxWidth())
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ScreenPaddingCompact),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
            ) {
                Spacer(Modifier.height(Dimens.SpacingLg))

                Box(
                    modifier = Modifier
                        .size(Dimens.AvatarLg)
                        .clickable { avatarPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = appString(R.string.profile_change_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Lucide.User,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconXl),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(Dimens.IconLg)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Lucide.Camera,
                            contentDescription = appString(R.string.profile_change_avatar),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(Dimens.SpacingLg),
                        )
                    }
                }

                Text(
                    appString(R.string.profile_change_avatar),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(appString(R.string.profile_display_name)) },
                    placeholder = { Text(appString(R.string.profile_display_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(Dimens.SpacingLg))

                Button(
                    onClick = {
                        scope.launch {
                            prefs.setDisplayName(displayName)
                            prefs.setAvatarUri(avatarUri)
                            // Back ngay sau khi lưu; snackbar hiện song song
                            // (showSnackbar suspend đến khi snackbar tắt — không được chặn back)
                            onBackClick()
                            snackbar.showSnackbar(savedMsg)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(appString(R.string.profile_save))
                }
            }
        }
    }
}
