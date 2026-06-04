package com.example.mybookslibrary.data.repository

import android.content.Context
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.UserDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phủ các hàm delegate session của [AuthRepository] và nhánh lưu user Google thất bại.
 */
class AuthRepositoryCoverageTest {
    private val userDao = mockk<UserDao>()
    private val preferences = mockk<UserPreferencesDataStore>(relaxed = true)
    private val googleSignInClient = mockk<GoogleSignInClient>()
    private val repository = AuthRepository(userDao, preferences, Dispatchers.IO, googleSignInClient)

    @Test
    fun observeLoggedInUserId_delegatesToPrefs() =
        runTest {
            every { preferences.observeLoggedInUserId() } returns flowOf("user-1")

            assertEquals("user-1", repository.observeLoggedInUserId().first())
        }

    @Test
    fun getLoggedInUserId_delegatesToPrefs() =
        runTest {
            coEvery { preferences.getLoggedInUserId() } returns "user-1"

            assertEquals("user-1", repository.getLoggedInUserId())
        }

    @Test
    fun logout_clearsLoggedInUser() =
        runTest {
            repository.logout()

            coVerify { preferences.setLoggedInUserId(null) }
        }

    @Test
    fun googleSignIn_whenUserStillMissingAfterUpsert_returnsFailure() =
        runTest {
            val context = mockk<Context>()
            coEvery { googleSignInClient.getGoogleAccount(context) } returns
                Result.success(GoogleAccount(googleId = "g1", email = "e", displayName = "n"))
            // getByGoogleId luôn null kể cả sau upsert -> nhánh "Failed to save Google user".
            coEvery { userDao.getByGoogleId("g1") } returns null
            coEvery { userDao.upsert(any()) } returns Unit

            assertTrue(repository.googleSignIn(context).isFailure)
        }
}
