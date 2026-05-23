package com.example.mybookslibrary

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.ui.navigation.MainNavHost
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.LocalAppLocale
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataStore: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            val language by preferencesDataStore.observeLanguage()
                .collectAsStateWithLifecycle(initialValue = "en")
            val themeMode by preferencesDataStore.observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = "system")
            val loggedInUserId by preferencesDataStore.observeLoggedInUserId()
                .collectAsStateWithLifecycle(initialValue = null)


            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) darkStyle else lightStyle,
                    navigationBarStyle = if (darkTheme) darkStyle else lightStyle,
                )
            }

            // LocalAppLocale thay đổi → toàn bộ appString() recompose → chuyển ngôn ngữ mượt mà
            CompositionLocalProvider(LocalAppLocale provides language) {
                MyBooksLibraryTheme(darkTheme = darkTheme) {
                    MainNavHost(loggedInUserId)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            POST_NOTIFICATIONS_REQUEST_CODE
        )
    }

    private companion object {
        const val POST_NOTIFICATIONS_REQUEST_CODE = 1001
    }
}
