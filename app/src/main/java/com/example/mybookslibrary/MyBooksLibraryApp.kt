package com.example.mybookslibrary

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import coil3.SingletonImageLoader
import com.example.mybookslibrary.data.remote.SyncWorker
import java.util.concurrent.TimeUnit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class MyBooksLibraryApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    @android.annotation.SuppressLint("LogNotTimber")
    override fun onCreate() {
        super.onCreate()
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Timber.plant(Timber.DebugTree())
        }

        SingletonImageLoader.setSafe {
            EntryPointAccessors
                .fromApplication(
                    this,
                    ImageLoaderEntryPoint::class.java,
                ).imageLoader()
        }

        // Bắt toàn bộ uncaught exception → ghi log ra file + logcat
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            // Log ra logcat với tag dễ filter
            Log.e(TAG, "═══ UNCAUGHT CRASH ═══")
            Log.e(TAG, "Thread: ${thread.name}")
            Log.e(TAG, "Exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
            Log.e(TAG, stackTrace)

            // Ghi ra file trong app cache dir
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val crashFile = File(cacheDir, "crash_$timestamp.txt")
                crashFile.writeText(
                    buildString {
                        appendLine("═══ MyBooksLibrary Crash Report ═══")
                        appendLine("Time: $timestamp")
                        appendLine("Thread: ${thread.name}")
                        appendLine("Exception: ${throwable.javaClass.name}")
                        appendLine("Message: ${throwable.message}")
                        appendLine()
                        appendLine("Stack Trace:")
                        appendLine(stackTrace)
                    },
                )
                Log.e(TAG, "Crash log saved: ${crashFile.absolutePath}")
            } catch (_: Exception) {
                // Không để ghi file lỗi gây thêm crash
            }

            // Chuyển về handler mặc định (hiện dialog crash)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.i(TAG, "App initialized")
        setupSyncWorker()
    }

    private fun setupSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LibrarySyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    companion object {
        private const val TAG = "MyBooksLibraryApp"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageLoaderEntryPoint {
    fun imageLoader(): coil3.ImageLoader
}
