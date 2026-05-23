package com.example.mybookslibrary.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val USER_PREFERENCES_NAME = "user_preferences"

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class UserPreferencesDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val READER_QUALITY = stringPreferencesKey("reader_quality")
        private val LANGUAGE = stringPreferencesKey("language")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DOWNLOAD_ONLY_ON_WIFI = booleanPreferencesKey("download_only_on_wifi")
        private val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")

        private const val DEFAULT_QUALITY = "data"
        private const val DEFAULT_LANGUAGE = "en"
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_DOWNLOAD_ONLY_ON_WIFI = true
    }

    // Chất lượng ảnh reader
    suspend fun getReaderQuality(): String =
        dataStore.data.first()[READER_QUALITY] ?: DEFAULT_QUALITY

    suspend fun setReaderQuality(quality: String) {
        dataStore.edit { it[READER_QUALITY] = quality }
    }

    // Ngôn ngữ
    fun observeLanguage(): Flow<String> = dataStore.data.map { it[LANGUAGE] ?: DEFAULT_LANGUAGE }

    suspend fun getLanguage(): String =
        dataStore.data.first()[LANGUAGE] ?: DEFAULT_LANGUAGE

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[LANGUAGE] = language }
    }

    // Chế độ giao diện: "system", "light", "dark"
    fun observeThemeMode(): Flow<String> = dataStore.data.map { it[THEME_MODE] ?: DEFAULT_THEME }

    suspend fun getThemeMode(): String =
        dataStore.data.first()[THEME_MODE] ?: DEFAULT_THEME

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    fun observeDownloadOnlyOnWifi(): Flow<Boolean> =
        dataStore.data.map { it[DOWNLOAD_ONLY_ON_WIFI] ?: DEFAULT_DOWNLOAD_ONLY_ON_WIFI }

    suspend fun getDownloadOnlyOnWifi(): Boolean =
        dataStore.data.first()[DOWNLOAD_ONLY_ON_WIFI] ?: DEFAULT_DOWNLOAD_ONLY_ON_WIFI

    suspend fun setDownloadOnlyOnWifi(enabled: Boolean) {
        dataStore.edit { it[DOWNLOAD_ONLY_ON_WIFI] = enabled }
    }

    fun observeLoggedInUserId(): Flow<String?> = dataStore.data.map { it[LOGGED_IN_USER_ID] }

    suspend fun getLoggedInUserId(): String? = dataStore.data.first()[LOGGED_IN_USER_ID]

    suspend fun setLoggedInUserId(id: String?) {
        dataStore.edit {
            if (id == null) {
                it.remove(LOGGED_IN_USER_ID)
            } else {
                it[LOGGED_IN_USER_ID] = id
            }
        }
    }


    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
