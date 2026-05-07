package com.example.mybookslibrary

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class MyBooksLibraryApp : Application() {

    override fun onCreate() {
        super.onCreate()

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
                crashFile.writeText(buildString {
                    appendLine("═══ MyBooksLibrary Crash Report ═══")
                    appendLine("Time: $timestamp")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable.javaClass.name}")
                    appendLine("Message: ${throwable.message}")
                    appendLine()
                    appendLine("Stack Trace:")
                    appendLine(stackTrace)
                })
                Log.e(TAG, "Crash log saved: ${crashFile.absolutePath}")
            } catch (_: Exception) {
                // Không để ghi file lỗi gây thêm crash
            }

            // Chuyển về handler mặc định (hiện dialog crash)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        Log.i(TAG, "App initialized")
    }

    companion object {
        private const val TAG = "MyBooksLibraryApp"
    }
}
