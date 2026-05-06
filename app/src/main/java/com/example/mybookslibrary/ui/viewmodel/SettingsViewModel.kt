package com.example.mybookslibrary.ui.viewmodel

import coil.ImageLoader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed class BackupRestoreResult {
    data class Success(val count: Int) : BackupRestoreResult()
    data class Failure(val message: String) : BackupRestoreResult()
}

data class SettingsUiState(
    val quality: String = "data",
    val themeMode: String = "system",
    val language: String = "en",
    val cacheCleared: Boolean = false,
    val signedOut: Boolean = false,
    val backupResult: BackupRestoreResult? = null,
    val restoreResult: BackupRestoreResult? = null
)

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val libraryRepository: LibraryRepository,
    private val imageLoader: ImageLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val q = preferencesDataStore.getReaderQuality()
            val t = preferencesDataStore.getThemeMode()
            val l = preferencesDataStore.getLanguage()
            _uiState.update { it.copy(quality = q, themeMode = t, language = l) }
        }
    }

    fun toggleQuality() {
        viewModelScope.launch(Dispatchers.IO) {
            val newQuality = if (_uiState.value.quality == "data") "data-saver" else "data"
            preferencesDataStore.setReaderQuality(newQuality)
            _uiState.update { it.copy(quality = newQuality) }
        }
    }

    fun cycleThemeMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val next = when (_uiState.value.themeMode) {
                "system" -> "light"
                "light" -> "dark"
                else -> "system"
            }
            preferencesDataStore.setThemeMode(next)
            _uiState.update { it.copy(themeMode = next) }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesDataStore.setLanguage(language)
            _uiState.update { it.copy(language = language) }
        }
    }

    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
            _uiState.update { it.copy(cacheCleared = true) }
        }
    }

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            // Chỉ reset quality về mặc định, giữ nguyên language + theme
            preferencesDataStore.setReaderQuality("data")
            libraryRepository.clearAll()
            _uiState.update { it.copy(signedOut = true, quality = "data") }
        }
    }

    fun backupLibrary(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = libraryRepository.getAllItems()
                val jsonItems = items.map { entity ->
                    mapOf(
                        "manga_id" to entity.manga_id,
                        "title" to entity.title,
                        "cover_url" to entity.cover_url,
                        "status" to entity.status.name,
                        "last_read_chapter_id" to (entity.last_read_chapter_id ?: ""),
                        "last_read_page_index" to entity.last_read_page_index,
                        "updated_at" to entity.updated_at
                    )
                }
                val json = gson.toJson(jsonItems)
                outputStream.bufferedWriter().use { it.write(json) }
                _uiState.update { it.copy(backupResult = BackupRestoreResult.Success(items.size)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(backupResult = BackupRestoreResult.Failure(e.message ?: "")) }
            }
        }
    }

    fun restoreLibrary(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val rawItems: List<Map<String, Any>> = gson.fromJson(json, type)
                val entities = rawItems.mapNotNull { map ->
                    try {
                        LibraryItemEntity(
                            manga_id = map["manga_id"] as? String ?: return@mapNotNull null,
                            title = map["title"] as? String ?: return@mapNotNull null,
                            cover_url = map["cover_url"] as? String ?: "",
                            status = try {
                                LibraryStatus.valueOf(map["status"] as? String ?: "READING")
                            } catch (_: IllegalArgumentException) {
                                LibraryStatus.READING
                            },
                            last_read_chapter_id = (map["last_read_chapter_id"] as? String)?.ifBlank { null },
                            last_read_page_index = (map["last_read_page_index"] as? Number)?.toInt() ?: 0,
                            updated_at = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                libraryRepository.restoreItems(entities)
                _uiState.update { it.copy(restoreResult = BackupRestoreResult.Success(entities.size)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(restoreResult = BackupRestoreResult.Failure(e.message ?: "")) }
            }
        }
    }
}
