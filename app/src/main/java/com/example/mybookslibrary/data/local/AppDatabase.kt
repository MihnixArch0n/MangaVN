package com.example.mybookslibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.local.dao.UserDao

@Database(
    entities = [
        UserEntity::class,
        LibraryItemEntity::class,
        ChapterProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun libraryDao(): LibraryDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val existing = INSTANCE
            if (existing != null) return existing

            return synchronized(this) {
                val again = INSTANCE
                if (again != null) return@synchronized again

                val created = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mybooks_library.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = created
                created
            }
        }
    }
}

