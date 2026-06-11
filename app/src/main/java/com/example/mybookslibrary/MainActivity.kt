package com.example.mybookslibrary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.domain.model.AuthStatus
import com.example.mybookslibrary.ui.navigation.MainNavHost
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.util.LocalAppLocale
import com.example.mybookslibrary.util.extractMangaIdFromMangaDexUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    @Inject
    lateinit var libraryRepository: LibraryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            // Parse ACTION_SEND intent (e.g. share from Chrome) to extract a manga ID.
            val incomingMangaId = remember(intent) {
                if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                    extractMangaIdFromMangaDexUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
                } else {
                    null
                }
            }
            val language by preferencesDataStore
                .observeLanguage()
                .collectAsStateWithLifecycle(initialValue = "en")
            val themeMode by preferencesDataStore
                .observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = "system")
            val authSessionFlow =
                remember(preferencesDataStore) {
                    preferencesDataStore
                        .observeAuthStatus()
                        .map { status -> AuthSession.Ready(status) }
                }
            val authSession by authSessionFlow.collectAsStateWithLifecycle(initialValue = AuthSession.Loading)

            val darkTheme =
                when (themeMode) {
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
                    when (val session = authSession) {
                        AuthSession.Loading -> AuthLoadingScreen()
                        is AuthSession.Ready -> {
                            LaunchedEffect(session.authStatus) {
                                if (session.authStatus == AuthStatus.LOGGED_IN) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            libraryRepository.performSync()
                                        } catch (e: Exception) {
                                            timber.log.Timber.e(e, "Error syncing on app open or login")
                                        }
                                    }
                                }
                            }
                            MainNavHost(session.authStatus, incomingMangaId)
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            POST_NOTIFICATIONS_REQUEST_CODE,
        )
    }

    private companion object {
        const val POST_NOTIFICATIONS_REQUEST_CODE = 1001
    }
}

private sealed interface AuthSession {
    data object Loading : AuthSession

    data class Ready(
        val authStatus: AuthStatus,
    ) : AuthSession
}

@Composable
@Suppress("FunctionName")
private fun AuthLoadingScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
    )
}
