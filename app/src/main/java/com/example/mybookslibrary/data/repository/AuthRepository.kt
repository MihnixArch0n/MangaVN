package com.example.mybookslibrary.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.mybookslibrary.data.local.UserEntity
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.UserDao
import com.example.mybookslibrary.di.IoDispatcher
import com.example.mybookslibrary.util.AuthSecrets
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val preferencesDataStore: UserPreferencesDataStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    fun observeLoggedInUserId(): Flow<String?> = preferencesDataStore.observeLoggedInUserId()

    suspend fun getLoggedInUserId(): String? = preferencesDataStore.getLoggedInUserId()

    suspend fun logout() {
        preferencesDataStore.setLoggedInUserId(null)
    }

    suspend fun register(username: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        val existingUser = userDao.getByUsername(username)
        if (existingUser != null) {
            return@withContext Result.failure(Exception("Username already exists"))
        }

        val user = UserEntity(
            username = username,
            password = hashPassword(password)
        )
        userDao.upsert(user)
        Result.success(Unit)
    }

    suspend fun login(username: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        val user = userDao.getByUsername(username)
            ?: return@withContext Result.failure(Exception("Invalid username or password"))

        if (!verifyPassword(password, user.password)) {
            return@withContext Result.failure(Exception("Invalid username or password"))
        }

        // Migrate dần: user còn lưu hash SHA-256 cũ → re-hash bằng PBKDF2 khi đăng nhập đúng.
        if (!user.password.startsWith("$PBKDF2_PREFIX:")) {
            userDao.upsert(user.copy(password = hashPassword(password)))
        }

        preferencesDataStore.setLoggedInUserId(user.id.toString())
        Result.success(Unit)
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

    // Hash mật khẩu bằng PBKDF2 + salt ngẫu nhiên per-user. Format: "pbkdf2:<iter>:<saltHex>:<hashHex>".
    private fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS)
        return listOf(PBKDF2_PREFIX, PBKDF2_ITERATIONS.toString(), salt.toHex(), hash.toHex())
            .joinToString(":")
    }

    // Xác minh mật khẩu, hỗ trợ cả hash PBKDF2 mới lẫn SHA-256 cũ (legacy) để migrate dần.
    private fun verifyPassword(password: String, stored: String): Boolean {
        if (!stored.startsWith("$PBKDF2_PREFIX:")) {
            return legacySha256(password) == stored
        }
        // Hash hỏng/định dạng sai (DB bị tamper) → coi như không khớp, không để exception thô thoát ra.
        return runCatching {
            val parts = stored.split(":")
            require(parts.size == 4)
            val salt = parts[2].hexToBytes()
            val expected = parts[3].hexToBytes()
            val actual = pbkdf2(password.toCharArray(), salt, parts[1].toInt())
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, PBKDF2_KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun legacySha256(password: String): String =
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private companion object {
        private const val PBKDF2_PREFIX = "pbkdf2"
        private const val PBKDF2_ITERATIONS = 600_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
    }
}
