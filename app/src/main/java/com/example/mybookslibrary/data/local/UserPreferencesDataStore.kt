package com.example.mybookslibrary.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private const val USER_PREFERENCES_NAME = "user_preferences"

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class UserPreferencesDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val READER_QUALITY = stringPreferencesKey("reader_quality")
        private const val DEFAULT_QUALITY = "data"
    }

    suspend fun getReaderQuality(): String =
        dataStore.data.first { preferences ->
            true
        }.let { preferences ->
            preferences[READER_QUALITY] ?: DEFAULT_QUALITY
        }

    suspend fun setReaderQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[READER_QUALITY] = quality
        }
    }
}


