@file:Suppress("ktlint")

package com.example.mybookslibrary.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.composables.icons.lucide.BookOpen
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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current
    val registerSuccessMsg = appString(R.string.feedback_register_success)

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
            snackbarHostState.showSnackbar(registerSuccessMsg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.auth_register_title)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = adaptiveFormMaxWidth())
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ScreenPaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Lucide.BookOpen,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconXxl),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Dimens.SpacingLg))
            Text(
                text = appString(R.string.auth_create_account),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.SpacingXxl))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(appString(R.string.auth_email)) },
                isError = uiState is AuthState.Error && email.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(appString(R.string.auth_password)) },
                placeholder = { Text(appString(R.string.auth_password_placeholder)) },
                supportingText = {
                    Text(appString(R.string.auth_password_hint), style = MaterialTheme.typography.bodySmall)
                },
                isError = uiState is AuthState.Error && password.isBlank(),
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Lucide.Eye else Lucide.EyeOff
                    val desc = if (passwordVisible) {
                        appString(R.string.cd_hide_password)
                    } else {
                        appString(R.string.cd_show_password)
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = desc)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(appString(R.string.auth_confirm_password)) },
                placeholder = { Text(appString(R.string.auth_confirm_password_placeholder)) },
                isError = password != confirmPassword && confirmPassword.isNotEmpty(),
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingXl))

            if (uiState is AuthState.Error) {
                ErrorMessageBox(
                    message = (uiState as AuthState.Error).message.asString(),
                    modifier = Modifier.padding(bottom = Dimens.SpacingLg),
                )
            } else if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                ErrorMessageBox(
                    message = appString(R.string.auth_passwords_no_match),
                    modifier = Modifier.padding(bottom = Dimens.SpacingLg),
                )
            }

            Button(
                onClick = {
                    if (password == confirmPassword) {
                        viewModel.register(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    uiState !is AuthState.Loading &&
                        password == confirmPassword &&
                        password.isNotEmpty() &&
                        email.isNotEmpty(),
            ) {
                if (uiState is AuthState.Loading) {
                    LoadingIndicator(
                        size = LoadingSize.Small,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(appString(R.string.auth_register_title))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXxl))

            Text(
                appString(R.string.auth_have_account_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(appString(R.string.auth_have_account_action))
            }
        }
        }
    }
}
