package com.example.mybookslibrary.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration tests cho AppDatabase.
 *
 * Mỗi khi tăng version phải:
 * 1. Viết Migration(oldVersion, newVersion) trong AppDatabase
 * 2. Thêm test case ở đây
 * 3. Chạy ./gradlew connectedDebugAndroidTest để verify
 *
 * Tại sao quan trọng: thiếu migration = crash app khi user upgrade,
 * mất toàn bộ thư viện manga của họ.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    /**
     * Smoke test: DB v3 (schema hiện tại) có thể mở được và tất cả bảng tồn tại.
     *
     * Dùng inMemoryDatabaseBuilder thay MigrationTestHelper.createDatabase() vì
     * Room 2.8.4 có binary incompatibility: DatabaseBundle$$serializer được compile
     * với Kotlin pre-2.0 (thiếu typeParametersSerializers()), nhưng runtime
     * kotlinx-serialization 1.7+ gọi method đó → AbstractMethodError.
     * Khi Room fix bug này, có thể restore MigrationTestHelper.
     */
    @Test
    fun openCurrentVersion_doesNotThrow() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            db.openHelper.readableDatabase.apply {
                query("SELECT * FROM users LIMIT 1").close()
                query("SELECT * FROM library_items LIMIT 1").close()
                query("SELECT * FROM chapter_progress LIMIT 1").close()
                query("SELECT * FROM download_queue LIMIT 1").close()
            }
        } finally {
            db.close()
        }
    }

    // Template cho migration v3 → v4.
    // Khi thêm tính năng mới cần thay đổi DB schema:
    // 1. Tăng version = 4 trong AppDatabase
    // 2. Viết Migration(3, 4) với các ALTER TABLE / CREATE TABLE cần thiết
    // 3. Tạo helper (xem bên dưới) và uncomment test
    // Lưu ý: MigrationTestHelper.createDatabase() vẫn bị Room 2.8.4 bug —
    //         check xem Room đã fix chưa trước khi dùng lại.
    // @get:Rule
    // val helper = MigrationTestHelper(
    //     InstrumentationRegistry.getInstrumentation(),
    //     AppDatabase::class.java,
    // )
    //
    // @Test
    // fun migrate3To4_preservesExistingData() {
    //     // 1. Tạo DB ở v3 và insert dữ liệu test
    //     helper.createDatabase(TEST_DB, 3).apply {
    //         execSQL("INSERT INTO library_items (manga_id, title, cover_url) VALUES ('m1', 'Naruto', '')")
    //         close()
    //     }
    //
    //     // 2. Chạy migration 3→4
    //     val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)
    //
    //     // 3. Verify data vẫn còn sau migration
    //     val cursor = db.query("SELECT manga_id FROM library_items WHERE manga_id = 'm1'")
    //     assert(cursor.moveToFirst()) { "Data bị mất sau migration 3→4" }
    //     cursor.close()
    //     db.close()
    // }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
