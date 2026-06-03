package com.example.mybookslibrary.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (_uiState.value is AuthState.Loading) return
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthState.Error("Username and password cannot be empty")
            return
        }
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.login(username, password)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(username: String, password: String) {
        if (_uiState.value is AuthState.Loading) return
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthState.Error("Username and password cannot be empty")
            return
        }
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.register(username, password)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun googleSignIn(context: Context) {
        if (_uiState.value is AuthState.Loading) return
        _uiState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.googleSignIn(context)
            if (result.isSuccess) {
                _uiState.value = AuthState.Success
            } else {
                _uiState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Google Sign-In failed")
            }
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
    data class Error(val message: String) : AuthState()
}
