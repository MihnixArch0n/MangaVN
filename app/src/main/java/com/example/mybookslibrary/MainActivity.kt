package com.example.mybookslibrary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.download.DownloadNotifier
import com.example.mybookslibrary.data.notification.NewChapterCheckWorker
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.domain.model.AuthStatus
import com.example.mybookslibrary.ui.navigation.LocalWindowWidthSizeClass
import com.example.mybookslibrary.ui.navigation.MainNavHost
import com.example.mybookslibrary.ui.navigation.Reader
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

    @Inject
    lateinit var downloadNotifier: DownloadNotifier

    private var incomingReader by androidx.compose.runtime.mutableStateOf<Reader?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingReader = intent.toNotificationReaderRoute()
        incomingReader?.let { downloadNotifier.dismissFinishedNotification(it.chapterId) }
        requestNotificationPermissionIfNeeded()
        com.example.mybookslibrary.data.notification.ReadingReminderWorker.schedule(this)
        // Worker tự bỏ qua nếu toggle tắt (mặc định tắt) → schedule vô điều kiện như ReadingReminderWorker.
        NewChapterCheckWorker.schedule(this)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
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
            val onboardingDone by preferencesDataStore
                .observeOnboardingWelcomeDone()
                .collectAsStateWithLifecycle(initialValue = null)
            val tourDone by preferencesDataStore
                .observeInAppTourDone()
                .collectAsStateWithLifecycle(initialValue = null)
            val onboardingScope = rememberCoroutineScope()

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
            CompositionLocalProvider(
                LocalAppLocale provides language,
                LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass,
            ) {
                MyBooksLibraryTheme(darkTheme = darkTheme) {
                    val session = authSession
                    val resolvedOnboarding = onboardingDone
                    val resolvedTour = tourDone
                    if (session !is AuthSession.Ready || resolvedOnboarding == null || resolvedTour == null) {
                        AuthLoadingScreen()
                    } else {
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
                        MainNavHost(
                            authStatus = session.authStatus,
                            incomingMangaId = incomingMangaId,
                            incomingReader = incomingReader,
                            onIncomingReaderConsumed = ::consumeIncomingReaderIntent,
                            onboardingWelcomeDone = resolvedOnboarding,
                            onWelcomeDone = {
                                onboardingScope.launch {
                                    preferencesDataStore.setOnboardingWelcomeDone(true)
                                }
                            },
                            inAppTourDone = resolvedTour,
                            onTourDone = {
                                onboardingScope.launch {
                                    preferencesDataStore.setInAppTourDone(true)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingReader = intent.toNotificationReaderRoute()
        incomingReader?.let { downloadNotifier.dismissFinishedNotification(it.chapterId) }
    }

    private fun consumeIncomingReaderIntent() {
        incomingReader = null
        setIntent(
            Intent(intent).apply {
                action = null
                removeExtra(EXTRA_MANGA_ID)
                removeExtra(EXTRA_CHAPTER_ID)
                removeExtra(EXTRA_CHAPTER_TITLE)
            },
        )
    }

    // Lật trang bằng phím âm lượng khi reader đang lắng nghe (xem ReaderVolumeKeyHandler).
    // Volume up = trang trước, volume down = trang kế.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val handler = com.example.mybookslibrary.ui.screens.reader.ReaderVolumeKeyHandler.onVolumeKey
        if (handler != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> if (handler(true)) return true
                KeyEvent.KEYCODE_VOLUME_DOWN -> if (handler(false)) return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            POST_NOTIFICATIONS_REQUEST_CODE,
        )
    }

    companion object {
        const val POST_NOTIFICATIONS_REQUEST_CODE = 1001

        const val ACTION_READ_DOWNLOADED_CHAPTER = "com.example.mybookslibrary.action.READ_DOWNLOADED_CHAPTER"
        const val EXTRA_MANGA_ID = "manga_id"
        const val EXTRA_CHAPTER_ID = "chapter_id"
        const val EXTRA_CHAPTER_TITLE = "chapter_title"
    }
}

private fun Intent?.toNotificationReaderRoute(): Reader? {
    if (this?.action != MainActivity.ACTION_READ_DOWNLOADED_CHAPTER) return null
    val mangaId = getStringExtra(MainActivity.EXTRA_MANGA_ID).orEmpty()
    val chapterId = getStringExtra(MainActivity.EXTRA_CHAPTER_ID).orEmpty()
    if (mangaId.isBlank() || chapterId.isBlank()) return null
    return Reader(
        mangaId = mangaId,
        chapterId = chapterId,
        chapterTitle = getStringExtra(MainActivity.EXTRA_CHAPTER_TITLE).orEmpty(),
        startPageIndex = 0,
    )
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
