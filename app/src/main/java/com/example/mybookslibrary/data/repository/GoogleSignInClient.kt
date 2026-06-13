package com.example.mybookslibrary.data.repository

import android.content.Context

/**
 * Tài khoản Google trả về sau khi xác thực, đã tách khỏi chi tiết Android CredentialManager
 * để [AuthRepository] có thể test logic lưu user mà không cần framework.
 */
data class GoogleAccount(
    val idToken: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
)

/**
 * Trừu tượng hóa việc lấy tài khoản Google qua Credential Manager. Tách interface giúp
 * AuthRepository không phụ thuộc trực tiếp vào API Android, nhờ đó unit test được.
 */
interface GoogleSignInClient {
    suspend fun getGoogleAccount(context: Context): Result<GoogleAccount>
}
