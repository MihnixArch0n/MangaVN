package com.example.mybookslibrary.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mybookslibrary.domain.model.AuthStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val USER_PREFERENCES_NAME = "user_preferences"

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME,
)

class UserPreferencesDataStore(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val READER_QUALITY = stringPreferencesKey("reader_quality")
        private val LANGUAGE = stringPreferencesKey("language")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DOWNLOAD_ONLY_ON_WIFI = booleanPreferencesKey("download_only_on_wifi")
        private val PREFERRED_CHAPTER_LANGUAGE = stringPreferencesKey("preferred_chapter_language")
        private val ONBOARDING_WELCOME_DONE = booleanPreferencesKey("onboarding_welcome_done")
        private val READER_HINT_DONE = booleanPreferencesKey("reader_hint_done")
        private val IN_APP_TOUR_DONE = booleanPreferencesKey("in_app_tour_done")
        private val FIRST_OPEN_TIME = longPreferencesKey("first_open_time")
        private val RATE_APP_DISMISSED = booleanPreferencesKey("rate_app_dismissed")
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val AVATAR_URI = stringPreferencesKey("avatar_uri")
        private val AUTH_STATUS = stringPreferencesKey("auth_status")
        private val FIREBASE_UID = stringPreferencesKey("firebase_uid")

        private const val DEFAULT_QUALITY = "data"
        private const val DEFAULT_LANGUAGE = "vi"
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_DOWNLOAD_ONLY_ON_WIFI = true
    }

    // Đọc prefs an toàn: file prefs hỏng (IOException) thì trả prefs rỗng thay vì ném,
    // tránh crash ở các điểm gọi lúc khởi động.
    private val safeData: Flow<Preferences> =
        dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    // ─── Auth Status ───────────────────────────────────────────────

    fun observeAuthStatus(): Flow<AuthStatus> = safeData.map {
        AuthStatus.fromString(it[AUTH_STATUS])
    }

    suspend fun getAuthStatus(): AuthStatus =
        AuthStatus.fromString(safeData.first()[AUTH_STATUS])

    suspend fun updateAuthStatus(status: AuthStatus) {
        dataStore.edit { it[AUTH_STATUS] = status.name }
    }

    // ─── Firebase UID ──────────────────────────────────────────────

    fun observeFirebaseUid(): Flow<String?> = safeData.map { it[FIREBASE_UID] }

    suspend fun getFirebaseUid(): String? = safeData.first()[FIREBASE_UID]

    suspend fun updateFirebaseUid(uid: String?) {
        dataStore.edit {
            if (uid == null) {
                it.remove(FIREBASE_UID)
            } else {
                it[FIREBASE_UID] = uid
            }
        }
    }

    // ─── Chất lượng ảnh reader ─────────────────────────────────────

    suspend fun getReaderQuality(): String = safeData.first()[READER_QUALITY] ?: DEFAULT_QUALITY

    suspend fun setReaderQuality(quality: String) {
        dataStore.edit { it[READER_QUALITY] = quality }
    }

    // ─── Ngôn ngữ ──────────────────────────────────────────────────

    fun observeLanguage(): Flow<String> = safeData.map { it[LANGUAGE] ?: DEFAULT_LANGUAGE }

    suspend fun getLanguage(): String = safeData.first()[LANGUAGE] ?: DEFAULT_LANGUAGE

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[LANGUAGE] = language }
    }

    // Ngôn ngữ chapter ưu tiên (rỗng/all = hiển thị tất cả, hoặc fallback theo logic)
    fun observePreferredChapterLanguage(): Flow<String> = safeData.map { it[PREFERRED_CHAPTER_LANGUAGE] ?: "" }

    suspend fun setPreferredChapterLanguage(language: String) {
        dataStore.edit { it[PREFERRED_CHAPTER_LANGUAGE] = language }
    }

    // ─── Chế độ giao diện ──────────────────────────────────────────

    fun observeThemeMode(): Flow<String> = safeData.map { it[THEME_MODE] ?: DEFAULT_THEME }

    suspend fun getThemeMode(): String = safeData.first()[THEME_MODE] ?: DEFAULT_THEME

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    // ─── Download ──────────────────────────────────────────────────

    fun observeDownloadOnlyOnWifi(): Flow<Boolean> = safeData.map {
        it[DOWNLOAD_ONLY_ON_WIFI] ?: DEFAULT_DOWNLOAD_ONLY_ON_WIFI
    }

    suspend fun getDownloadOnlyOnWifi(): Boolean = safeData.first()[DOWNLOAD_ONLY_ON_WIFI]
        ?: DEFAULT_DOWNLOAD_ONLY_ON_WIFI

    suspend fun setDownloadOnlyOnWifi(enabled: Boolean) {
        dataStore.edit { it[DOWNLOAD_ONLY_ON_WIFI] = enabled }
    }

    // ─── Clear ─────────────────────────────────────────────────────

    /** Onboarding: welcome carousel đã xem xong. */
    fun observeOnboardingWelcomeDone(): Flow<Boolean> =
        safeData.map { it[ONBOARDING_WELCOME_DONE] ?: false }

    suspend fun setOnboardingWelcomeDone(done: Boolean) {
        dataStore.edit { it[ONBOARDING_WELCOME_DONE] = done }
    }

    /** Onboarding: reader hint đã xem xong. */
    fun observeReaderHintDone(): Flow<Boolean> =
        safeData.map { it[READER_HINT_DONE] ?: false }

    suspend fun setReaderHintDone(done: Boolean) {
        dataStore.edit { it[READER_HINT_DONE] = done }
    }

    /** In-app guided tour (coach marks) đã hoàn thành. */
    fun observeInAppTourDone(): Flow<Boolean> =
        safeData.map { it[IN_APP_TOUR_DONE] ?: false }

    suspend fun setInAppTourDone(done: Boolean) {
        dataStore.edit { it[IN_APP_TOUR_DONE] = done }
    }

    suspend fun getFirstOpenTime(): Long {
        val stored = safeData.first()[FIRST_OPEN_TIME]
        if (stored == null) {
            val now = System.currentTimeMillis()
            dataStore.edit { it[FIRST_OPEN_TIME] = now }
            return now
        }
        return stored
    }

    fun observeRateAppDismissed(): Flow<Boolean> =
        safeData.map { it[RATE_APP_DISMISSED] ?: false }

    suspend fun setRateAppDismissed(dismissed: Boolean) {
        dataStore.edit { it[RATE_APP_DISMISSED] = dismissed }
    }

    fun observeDisplayName(): Flow<String> =
        safeData.map { it[DISPLAY_NAME] ?: "" }

    suspend fun setDisplayName(name: String) {
        dataStore.edit { it[DISPLAY_NAME] = name }
    }

    fun observeAvatarUri(): Flow<String> =
        safeData.map { it[AVATAR_URI] ?: "" }

    suspend fun setAvatarUri(uri: String) {
        dataStore.edit { it[AVATAR_URI] = uri }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
