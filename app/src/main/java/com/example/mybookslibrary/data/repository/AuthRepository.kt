package com.example.mybookslibrary.data.repository

import android.content.Context
import com.example.mybookslibrary.data.local.UserEntity
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.UserDao
import com.example.mybookslibrary.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
    @Inject
    constructor(
        private val userDao: UserDao,
        private val preferencesDataStore: UserPreferencesDataStore,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val googleSignInClient: GoogleSignInClient,
    ) {
        /** Reactive stream ID người dùng đang đăng nhập; emit `null` khi chưa đăng nhập. */
        fun observeLoggedInUserId(): Flow<String?> = preferencesDataStore.observeLoggedInUserId()

        /** Lấy ID người dùng đang đăng nhập một lần; `null` nếu chưa đăng nhập. */
        suspend fun getLoggedInUserId(): String? = preferencesDataStore.getLoggedInUserId()

        /** Xóa session đăng nhập hiện tại. */
        suspend fun logout() {
            preferencesDataStore.setLoggedInUserId(null)
        }

        /**
         * Đăng ký tài khoản mới. Password hash bằng PBKDF2 + salt ngẫu nhiên.
         * Thất bại nếu username đã tồn tại.
         */
        suspend fun register(
            username: String,
            password: String,
        ): Result<Unit> =
            withContext(ioDispatcher) {
                val existingUser = userDao.getByUsername(username)
                if (existingUser != null) {
                    return@withContext Result.failure(Exception("Username already exists"))
                }

                val user =
                    UserEntity(
                        username = username,
                        password = hashPassword(password),
                    )
                userDao.upsert(user)
                Result.success(Unit)
            }

        /**
         * Đăng nhập bằng username/password. Tự migrate hash SHA-256 cũ sang PBKDF2
         * khi xác thực thành công lần đầu.
         */
        suspend fun login(
            username: String,
            password: String,
        ): Result<Unit> =
            withContext(ioDispatcher) {
                val user =
                    userDao.getByUsername(username)
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

        /**
         * Đăng nhập bằng Google. Tự tạo [UserEntity] mới nếu Google ID chưa có trong DB.
         */
        suspend fun googleSignIn(context: Context): Result<Unit> {
            val account =
                googleSignInClient
                    .getGoogleAccount(context)
                    .getOrElse { return Result.failure(it) }

            var user = userDao.getByGoogleId(account.googleId)
            if (user == null) {
                userDao.upsert(
                    UserEntity(
                        username = account.displayName,
                        password = "", // Đăng nhập Google không dùng mật khẩu nội bộ
                        email = account.email,
                        google_id = account.googleId,
                    ),
                )
                user = userDao.getByGoogleId(account.googleId)
            }

            return if (user != null) {
                preferencesDataStore.setLoggedInUserId(user.id.toString())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save Google user"))
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
        private fun verifyPassword(
            password: String,
            stored: String,
        ): Boolean {
            if (!stored.startsWith("$PBKDF2_PREFIX:")) {
                return legacySha256(password) == stored
            }
            // Hash hỏng/định dạng sai (DB bị tamper) → false; chỉ bắt exception có thể xảy ra,
            // không nuốt Error hay NPE do bug code.
            return try {
                val parts = stored.split(":")
                require(parts.size == 4)
                val salt = parts[2].hexToBytes()
                val expected = parts[3].hexToBytes()
                val actual = pbkdf2(password.toCharArray(), salt, parts[1].toInt())
                MessageDigest.isEqual(actual, expected)
            } catch (_: IllegalArgumentException) {
                false
            } catch (_: GeneralSecurityException) {
                false
            }
        }

        private fun pbkdf2(
            password: CharArray,
            salt: ByteArray,
            iterations: Int,
        ): ByteArray {
            val spec = PBEKeySpec(password, salt, iterations, PBKDF2_KEY_LENGTH_BITS)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        }

        private fun legacySha256(password: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

        private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        private companion object {
            private const val PBKDF2_PREFIX = "pbkdf2"
            private const val PBKDF2_ITERATIONS = 600_000
            private const val PBKDF2_KEY_LENGTH_BITS = 256
            private const val SALT_BYTES = 16
        }
    }
