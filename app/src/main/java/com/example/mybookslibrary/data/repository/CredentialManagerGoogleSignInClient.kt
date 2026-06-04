package com.example.mybookslibrary.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.mybookslibrary.util.AuthSecrets
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject

/**
 * Triển khai [GoogleSignInClient] bằng Android Credential Manager. Lớp này chứa toàn bộ
 * phụ thuộc framework (không unit test) — logic lưu user nằm ở AuthRepository.
 */
class CredentialManagerGoogleSignInClient
    @Inject
    constructor() : GoogleSignInClient {
        // CredentialManager ném nhiều loại exception (GetCredentialException + runtime) ở ranh giới
        // Android — gom tất cả về Result.failure thay vì để bung ra UI.
        @Suppress("TooGenericExceptionCaught")
        override suspend fun getGoogleAccount(context: Context): Result<GoogleAccount> {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption =
                GetGoogleIdOption
                    .Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(AuthSecrets.WEB_CLIENT_ID)
                    .build()
            val request =
                GetCredentialRequest
                    .Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

            return try {
                val response = credentialManager.getCredential(context = context, request = request)
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(
                        GoogleAccount(
                            googleId = token.id,
                            // id thường là email với Google ID token; giữ nguyên hành vi cũ.
                            email = token.id,
                            displayName = token.displayName ?: "Google User",
                        ),
                    )
                } else {
                    Result.failure(IllegalStateException("Unexpected credential type"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
