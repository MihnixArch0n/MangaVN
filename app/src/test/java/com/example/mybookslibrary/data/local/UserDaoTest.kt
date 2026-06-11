package com.example.mybookslibrary.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_thenGetById_returnsUser() =
        runTest {
            val dao = database.userDao()
            dao.upsert(user(id = 1, username = "alice"))

            assertEquals("alice", dao.getById(1)?.username)
            assertNull(dao.getById(99))
        }

    @Test
    fun getByUsername_matchesExactUsername() =
        runTest {
            val dao = database.userDao()
            dao.upsert(user(id = 1, username = "alice"))
            dao.upsert(user(id = 2, username = "bob"))

            assertEquals(2L, dao.getByUsername("bob")?.id)
            assertNull(dao.getByUsername("carol"))
        }

    @Test
    fun getByGoogleId_returnsLinkedUser() =
        runTest {
            val dao = database.userDao()
            dao.upsert(user(id = 1, username = "alice", googleId = "google-123"))
            dao.upsert(user(id = 2, username = "bob", googleId = null))

            assertEquals("alice", dao.getByGoogleId("google-123")?.username)
            assertNull(dao.getByGoogleId("google-999"))
        }

    @Test
    fun upsert_withSameId_replacesExistingRow() =
        runTest {
            val dao = database.userDao()
            dao.upsert(user(id = 1, username = "alice"))

            dao.upsert(user(id = 1, username = "alice-renamed"))

            assertEquals("alice-renamed", dao.getById(1)?.username)
        }

    @Test
    fun observeLatestUser_returnsMostRecentlyCreated() =
        runTest {
            val dao = database.userDao()
            dao.upsert(user(id = 1, username = "older", createdAt = 1_000L))
            dao.upsert(user(id = 2, username = "newer", createdAt = 2_000L))

            assertEquals("newer", dao.observeLatestUser().first()?.username)
        }

    @Test
    fun observeLatestUser_emitsNullWhenNoUsers() =
        runTest {
            assertNull(database.userDao().observeLatestUser().first())
        }

    private fun user(
        id: Long,
        username: String,
        googleId: String? = null,
        createdAt: Long = 1_000L,
    ) = UserEntity(
        id = id,
        username = username,
        password = "hashed-password",
        created_at = createdAt,
        google_id = googleId,
    )
}
