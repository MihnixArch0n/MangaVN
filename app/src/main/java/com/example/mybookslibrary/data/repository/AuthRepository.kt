package com.example.mybookslibrary.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.mybookslibrary.data.local.UserEntity
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.UserDao
import com.example.mybookslibrary.util.AuthSecrets
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    fun observeLoggedInUserId(): Flow<String?> = preferencesDataStore.observeLoggedInUserId()

    suspend fun getLoggedInUserId(): String? = preferencesDataStore.getLoggedInUserId()

    suspend fun logout() {
        preferencesDataStore.setLoggedInUserId(null)
    }

    suspend fun register(username: String, password: String): Result<Unit> {
        val existingUser = userDao.getByUsername(username)
        if (existingUser != null) {
            return Result.failure(Exception("Username already exists"))
        }

        val user = UserEntity(
            username = username,
            password = hashPassword(password)
        )
        userDao.upsert(user)
        return Result.success(Unit)
    }

    suspend fun login(username: String, password: String): Result<Unit> {
        val user = userDao.getByUsername(username)
            ?: return Result.failure(Exception("Invalid username or password"))

        if (user.password != hashPassword(password)) {
            return Result.failure(Exception("Invalid username or password"))
        }

        preferencesDataStore.setLoggedInUserId(user.id.toString())
        return Result.success(Unit)
    }

    suspend fun googleSignIn(context: Context): Result<Unit> {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(AuthSecrets.WEB_CLIENT_ID)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                
                val googleId = googleIdTokenCredential.id
                val email = googleIdTokenCredential.id // Sometimes id is email, or we use displayName
                val displayName = googleIdTokenCredential.displayName ?: "Google User"

                // Check if user exists
                var user = userDao.getByGoogleId(googleId)
                if (user == null) {
                    // Check if email/username exists? We'll just create a new one
                    user = UserEntity(
                        username = displayName,
                        password = "", // No password for Google Sign In
                        email = email,
                        google_id = googleId
                    )
                    userDao.upsert(user)
                    // Get it back to get the auto-generated ID
                    user = userDao.getByGoogleId(googleId)
                }

                if (user != null) {
                    preferencesDataStore.setLoggedInUserId(user.id.toString())
                    return Result.success(Unit)
                } else {
                    return Result.failure(Exception("Failed to save Google user"))
                }
            } else {
                return Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
