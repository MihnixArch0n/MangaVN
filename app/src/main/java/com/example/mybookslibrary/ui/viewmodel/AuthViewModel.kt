package com.example.mybookslibrary.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.R
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun authError(message: String?): UiText =
    when {
        message?.contains("exists") == true ||
            message?.contains("already in use") == true ->
            UiText.Resource(R.string.auth_error_email_exists)
        message?.contains("password") == true ||
            message?.contains("invalid credential") == true ->
            UiText.Resource(R.string.auth_error_invalid_credentials)
        message?.contains("Google") == true -> UiText.Resource(R.string.auth_error_google_save_failed)
        message == null -> UiText.Resource(R.string.error_unexpected)
        else -> UiText.Dynamic(message)
    }

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
        val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

        fun login(
            email: String,
            password: String,
        ) {
            if (_uiState.value is AuthState.Loading) return
            if (email.isBlank() || password.isBlank()) {
                _uiState.value = AuthState.Error(UiText.Resource(R.string.auth_error_empty_fields))
                return
            }
            _uiState.value = AuthState.Loading
            viewModelScope.launch {
                val result = authRepository.signInWithEmail(email, password)
                _uiState.value =
                    if (result.isSuccess) {
                        AuthState.Success
                    } else {
                        AuthState.Error(authError(result.exceptionOrNull()?.message))
                    }
            }
        }

        fun register(
            email: String,
            password: String,
        ) {
            if (_uiState.value is AuthState.Loading) return
            if (email.isBlank() || password.isBlank()) {
                _uiState.value = AuthState.Error(UiText.Resource(R.string.auth_error_empty_fields))
                return
            }
            _uiState.value = AuthState.Loading
            viewModelScope.launch {
                val result = authRepository.registerWithEmail(email, password)
                _uiState.value =
                    if (result.isSuccess) {
                        AuthState.Success
                    } else {
                        AuthState.Error(authError(result.exceptionOrNull()?.message))
                    }
            }
        }

        fun googleSignIn(context: Context) {
            if (_uiState.value is AuthState.Loading) return
            _uiState.value = AuthState.Loading
            viewModelScope.launch {
                val result = authRepository.signInWithGoogle(context)
                _uiState.value =
                    if (result.isSuccess) {
                        AuthState.Success
                    } else {
                        AuthState.Error(authError(result.exceptionOrNull()?.message))
                    }
            }
        }

        fun continueAsGuest() {
            if (_uiState.value is AuthState.Loading) return
            _uiState.value = AuthState.Loading
            viewModelScope.launch {
                authRepository.continueAsGuest()
                _uiState.value = AuthState.Success
            }
        }

        fun resetState() {
            _uiState.value = AuthState.Idle
        }
    }

sealed class AuthState {
    object Idle : AuthState()

    object Loading : AuthState()

    object Success : AuthState()

    data class Error(
        val message: UiText,
    ) : AuthState()
}
