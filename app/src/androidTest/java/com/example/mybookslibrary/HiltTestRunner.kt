package com.example.mybookslibrary

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner cho Hilt instrumented test.
 * Dùng HiltTestApplication thay vì MyBooksLibraryApp để @HiltAndroidTest hoạt động.
 * Khai báo trong build.gradle.kts: testInstrumentationRunner = "...HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
