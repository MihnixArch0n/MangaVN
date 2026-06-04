package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.UserEntity
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.UserDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Regression guard cho finding H1: mật khẩu phải được hash kèm salt per-user.
 *
 * Hai user khác nhau dùng cùng mật khẩu phải cho ra hash KHÁC nhau (PBKDF2 +
 * salt per-user), tránh lộ thông tin "trùng mật khẩu" qua DB và rainbow table.
 */
class AuthRepositoryPasswordHashTest {
    private val userDao = mockk<UserDao>()
    private val preferences = mockk<UserPreferencesDataStore>(relaxed = true)
    private val googleSignInClient = mockk<GoogleSignInClient>(relaxed = true)
    private val repository = AuthRepository(userDao, preferences, Dispatchers.IO, googleSignInClient)

    @Test
    fun register_twoUsersSamePassword_produceDifferentHashes() =
        runTest {
            coEvery { userDao.getByUsername(any()) } returns null
            val captured = mutableListOf<UserEntity>()
            coEvery { userDao.upsert(capture(captured)) } returns Unit

            repository.register("alice", "hunter2")
            repository.register("bob", "hunter2")

            // Salt per-user → hai hash phải khác nhau
            assertNotEquals(
                "Hai user cùng mật khẩu phải có hash khác nhau (salt per-user)",
                captured[0].password,
                captured[1].password,
            )
        }
}
