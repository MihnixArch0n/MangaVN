package com.example.mybookslibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.ui.navigation.MainNavHost
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.LocalAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataStore: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialLang = runBlocking { preferencesDataStore.getLanguage() }
        val initialTheme = runBlocking { preferencesDataStore.getThemeMode() }

        enableEdgeToEdge()
        setContent {
            val language by preferencesDataStore.observeLanguage()
                .collectAsState(initial = initialLang)
            val themeMode by preferencesDataStore.observeThemeMode()
                .collectAsState(initial = initialTheme)

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // LocalAppLocale thay đổi → toàn bộ appString() recompose → chuyển ngôn ngữ mượt mà
            CompositionLocalProvider(LocalAppLocale provides language) {
                MyBooksLibraryTheme(darkTheme = darkTheme) {
                    MainNavHost()
                }
            }
        }
    }
}
