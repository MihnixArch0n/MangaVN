package com.example.mybookslibrary.domain.model

/**
 * Trạng thái xác thực của người dùng, lưu trong DataStore.
 *
 * - [LOGGED_OUT] — mặc định khi cài app mới, chưa đăng nhập lần nào.
 * - [LOGGED_IN] — đã đăng nhập thành công qua Firebase Auth.
 * - [GUEST] — tiếp tục dưới dạng Khách, không có cloud sync.
 */
enum class AuthStatus {
    GUEST,
    LOGGED_IN,
    LOGGED_OUT;

    companion object {
        fun fromString(value: String?): AuthStatus =
            entries.firstOrNull { it.name == value } ?: LOGGED_OUT
    }
}
