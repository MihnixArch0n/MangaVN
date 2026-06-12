package com.example.mybookslibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.DownloadQueueDao
import com.example.mybookslibrary.data.local.dao.LibraryDao

@Suppress("UnusedPrivateProperty")
private const val PREVIOUS_DATABASE_VERSION = 6
private const val CURRENT_DATABASE_VERSION = 2

@Database(
    entities = [
        LibraryItemEntity::class,
        ChapterProgressEntity::class,
        DownloadQueueEntity::class,
        ChapterMetadataEntity::class,
    ],
    version = CURRENT_DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(LibraryStatusConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    abstract fun chapterDao(): ChapterDao

    abstract fun downloadQueueDao(): DownloadQueueDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val existing = instance
            if (existing != null) return existing

            return synchronized(this) {
                val again = instance
                if (again != null) return@synchronized again

                val created =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "mybooks_library.db",
                        )
                        .addMigrations(migration5To1, migration1To2, migration6To2)
                        .fallbackToDestructiveMigration()
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()

                instance = created
                created
            }
        }

        @Suppress("MagicNumber")
        val migration5To1 =
            object : Migration(5, 1) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `library_items` ADD COLUMN `sync_status` TEXT NOT NULL DEFAULT 'SYNCED'"
                    )
                }
            }

        @Suppress("MagicNumber")
        val migration6To2 =
            object : Migration(6, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `library_items` ADD COLUMN `sync_status` TEXT NOT NULL DEFAULT 'SYNCED'"
                    )
                }
            }

        @Suppress("MagicNumber")
        val migration1To2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `library_items` ADD COLUMN `is_favorite` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
    }
}
