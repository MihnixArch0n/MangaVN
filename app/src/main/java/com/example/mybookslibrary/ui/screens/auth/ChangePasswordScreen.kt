package com.example.mybookslibrary.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.navigation.LocalSnackbarHostState
import com.example.mybookslibrary.ui.screens.components.ErrorMessageBox
import com.example.mybookslibrary.ui.screens.components.LoadingIndicator
import com.example.mybookslibrary.ui.screens.components.LoadingSize
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.adaptiveFormMaxWidth
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.viewmodel.AuthState
import com.example.mybookslibrary.ui.viewmodel.AuthViewModel

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current
    val changedMsg = appString(R.string.feedback_password_changed)

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            onBackClick()
            snackbarHostState.showSnackbar(changedMsg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.auth_change_password_title)) },
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
                    .padding(Dimens.ScreenPaddingMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg),
            ) {
                PasswordField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = appString(R.string.auth_current_password),
                    passwordVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                )

                PasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = appString(R.string.auth_new_password),
                    passwordVisible = passwordVisible,
                    onToggleVisibility = null,
                    supportingText = appString(R.string.auth_password_hint),
                )

                PasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = appString(R.string.auth_confirm_new_password),
                    passwordVisible = passwordVisible,
                    onToggleVisibility = null,
                    isError = newPassword != confirmPassword && confirmPassword.isNotEmpty(),
                )

                if (uiState is AuthState.Error) {
                    ErrorMessageBox(
                        message = (uiState as AuthState.Error).message.asString(),
                    )
                }

                Spacer(Modifier.height(Dimens.SpacingSm))

                Button(
                    onClick = {
                        viewModel.changePassword(currentPassword, newPassword, confirmPassword)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        uiState !is AuthState.Loading &&
                            currentPassword.isNotEmpty() &&
                            newPassword.isNotEmpty() &&
                            confirmPassword.isNotEmpty(),
                ) {
                    if (uiState is AuthState.Loading) {
                        LoadingIndicator(
                            size = LoadingSize.Small,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(appString(R.string.auth_change_password_title))
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    passwordVisible: Boolean,
    onToggleVisibility: (() -> Unit)?,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation =
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon =
            if (onToggleVisibility != null) {
                {
                    val image = if (passwordVisible) Lucide.Eye else Lucide.EyeOff
                    val desc = if (passwordVisible) {
                        appString(R.string.cd_hide_password)
                    } else {
                        appString(R.string.cd_show_password)
                    }
                    IconButton(onClick = onToggleVisibility) {
                        Icon(imageVector = image, contentDescription = desc)
                    }
                }
            } else {
                null
            },
        supportingText =
            if (supportingText != null) {
                { Text(supportingText, style = MaterialTheme.typography.bodySmall) }
            } else {
                null
            },
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
