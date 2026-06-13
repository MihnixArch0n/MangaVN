@file:Suppress("ktlint")

package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import com.example.mybookslibrary.data.local.LibraryBackup
import com.example.mybookslibrary.data.local.LibraryBackupItem
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.toBackupItem
import com.example.mybookslibrary.data.repository.AuthRepository
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed class BackupRestoreResult {
    data class Success(
        val count: Int,
    ) : BackupRestoreResult()

    data class Failure(
        val message: String,
    ) : BackupRestoreResult()
}

data class SettingsUiState(
    val quality: String = "data",
    val themeMode: String = "system",
    val language: String = "en",
    val cacheCleared: Boolean = false,
    val signedOut: Boolean = false,
    val backupResult: BackupRestoreResult? = null,
    val restoreResult: BackupRestoreResult? = null,
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean? = null,
    val isGuest: Boolean = false,
    val keepScreenOn: Boolean = false,
    val volumeKeyNav: Boolean = false,
    val autoAdvance: Boolean = false,
    val autoDownloadNext: Boolean = false,
    val newChapterNotifications: Boolean = false,
    val deleteAfterRead: Boolean = false,
    val deleteAfterReadKeep: Int = 2,
)

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@Suppress("TooManyFunctions") // ViewModel màn Settings gom nhiều toggle/setter — tách nhỏ không có lợi
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val preferencesDataStore: UserPreferencesDataStore,
        private val libraryRepository: LibraryRepository,
        private val imageLoader: ImageLoader,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val json: Json,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch(ioDispatcher) {
                val q = preferencesDataStore.getReaderQuality()
                val t = preferencesDataStore.getThemeMode()
                val l = preferencesDataStore.getLanguage()
                val guest = authRepository.getCurrentUser() == null
                val keepScreenOn = preferencesDataStore.getReaderKeepScreenOn()
                val volumeKeyNav = preferencesDataStore.getReaderVolumeKeyNav()
                val autoAdvance = preferencesDataStore.getReaderAutoAdvance()
                val autoDownloadNext = preferencesDataStore.getAutoDownloadNext()
                val newChapterNotifications = preferencesDataStore.getNewChapterNotifications()
                val deleteAfterRead = preferencesDataStore.getDeleteAfterRead()
                val deleteAfterReadKeep = preferencesDataStore.getDeleteAfterReadKeep()
                _uiState.update {
                    it.copy(
                        quality = q,
                        themeMode = t,
                        language = l,
                        isGuest = guest,
                        keepScreenOn = keepScreenOn,
                        volumeKeyNav = volumeKeyNav,
                        autoAdvance = autoAdvance,
                        autoDownloadNext = autoDownloadNext,
                        newChapterNotifications = newChapterNotifications,
                        deleteAfterRead = deleteAfterRead,
                        deleteAfterReadKeep = deleteAfterReadKeep,
                    )
                }
            }
        }

        fun toggleQuality() {
            viewModelScope.launch(ioDispatcher) {
                val newQuality = if (_uiState.value.quality == "data") "data-saver" else "data"
                preferencesDataStore.setReaderQuality(newQuality)
                _uiState.update { it.copy(quality = newQuality) }
            }
        }

        fun toggleKeepScreenOn() {
            viewModelScope.launch(ioDispatcher) {
                val newValue = !_uiState.value.keepScreenOn
                preferencesDataStore.setReaderKeepScreenOn(newValue)
                _uiState.update { it.copy(keepScreenOn = newValue) }
            }
        }

        fun toggleVolumeKeyNav() {
            viewModelScope.launch(ioDispatcher) {
                val newValue = !_uiState.value.volumeKeyNav
                preferencesDataStore.setReaderVolumeKeyNav(newValue)
                _uiState.update { it.copy(volumeKeyNav = newValue) }
            }
        }

        fun toggleAutoAdvance() {
            viewModelScope.launch(ioDispatcher) {
                val newValue = !_uiState.value.autoAdvance
                preferencesDataStore.setReaderAutoAdvance(newValue)
                _uiState.update { it.copy(autoAdvance = newValue) }
            }
        }

        fun toggleAutoDownloadNext() {
            viewModelScope.launch(ioDispatcher) {
                val newValue = !_uiState.value.autoDownloadNext
                preferencesDataStore.setAutoDownloadNext(newValue)
                _uiState.update { it.copy(autoDownloadNext = newValue) }
            }
        }

        fun toggleNewChapterNotifications() {
            viewModelScope.launch(ioDispatcher) {
                val newValue = !_uiState.value.newChapterNotifications
                preferencesDataStore.setNewChapterNotifications(newValue)
                _uiState.update { it.copy(newChapterNotifications = newValue) }
            }
        }

        fun toggleDeleteAfterRead() {
            viewModelScope.launch(ioDispatcher) {
                val newValue = !_uiState.value.deleteAfterRead
                preferencesDataStore.setDeleteAfterRead(newValue)
                _uiState.update { it.copy(deleteAfterRead = newValue) }
            }
        }

        fun setDeleteAfterReadKeep(keep: Int) {
            viewModelScope.launch(ioDispatcher) {
                preferencesDataStore.setDeleteAfterReadKeep(keep)
                _uiState.update { it.copy(deleteAfterReadKeep = keep.coerceIn(KEEP_MIN, KEEP_MAX)) }
            }
        }

        fun cycleThemeMode() {
            viewModelScope.launch(ioDispatcher) {
                val next =
                    when (_uiState.value.themeMode) {
                        "system" -> "light"
                        "light" -> "dark"
                        else -> "system"
                    }
                preferencesDataStore.setThemeMode(next)
                _uiState.update { it.copy(themeMode = next) }
            }
        }

        fun setLanguage(language: String) {
            viewModelScope.launch(ioDispatcher) {
                preferencesDataStore.setLanguage(language)
                _uiState.update { it.copy(language = language) }
            }
        }

        fun clearImageCache() {
            viewModelScope.launch(ioDispatcher) {
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
                _uiState.update { it.copy(cacheCleared = true) }
            }
        }

        fun signOut() {
            viewModelScope.launch(ioDispatcher) {
                try {
                    preferencesDataStore.setReaderQuality("data")
                    authRepository.signOut()
                    libraryRepository.clearAll()
                    _uiState.update { it.copy(signedOut = true, quality = "data") }
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    Timber.e(e, "Sign out failed")
                    _uiState.update { it.copy(signedOut = false) }
                }
            }
        }

        fun forceSync() {
            viewModelScope.launch(ioDispatcher) {
                _uiState.update { it.copy(isSyncing = true, syncSuccess = null) }
                try {
                    libraryRepository.performSync()
                    _uiState.update { it.copy(isSyncing = false, syncSuccess = true) }
                } catch (e: Exception) {
                    Timber.e(e, "Manual sync failed")
                    _uiState.update { it.copy(isSyncing = false, syncSuccess = false) }
                }
            }
        }

        fun deleteAccount() {
            viewModelScope.launch(ioDispatcher) {
                try {
                    libraryRepository.clearAllRemote()
                    preferencesDataStore.setReaderQuality("data")
                    libraryRepository.clearAll()
                    authRepository.deleteAccount().getOrThrow()
                    _uiState.update { it.copy(signedOut = true, quality = "data") }
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    Timber.e(e, "Error deleting account")
                }
            }
        }

        fun backupLibrary(outputStream: OutputStream) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    val items = libraryRepository.getAllItems()
                    val mangaIds = items.mapTo(mutableSetOf()) { it.manga_id }
                    val chapterProgress =
                        libraryRepository.getAllChapterProgress().filter { it.manga_id in mangaIds }
                    val backupJson =
                        json.encodeToString(
                            LibraryBackup(
                                library = items.map { it.toBackupItem() },
                                chapterProgress = chapterProgress.map { it.toBackupItem() },
                            ),
                        )
                    outputStream.bufferedWriter().use { it.write(backupJson) }
                    _uiState.update { it.copy(backupResult = BackupRestoreResult.Success(items.size)) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(backupResult = BackupRestoreResult.Failure(e.message ?: "")) }
                }
            }
        }

        fun restoreLibrary(inputStream: InputStream) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    val backupJson = inputStream.bufferedReader().use { it.readText() }
                    val root = json.parseToJsonElement(backupJson)
                    val backup =
                        if (root is JsonArray) {
                            LibraryBackup(
                                library =
                                    root.mapNotNull { element ->
                                        runCatching {
                                            json.decodeFromJsonElement<LibraryBackupItem>(element)
                                        }.getOrNull()
                                    },
                            )
                        } else {
                            json.decodeFromJsonElement<LibraryBackup>(root)
                        }
                    val entities =
                        backup.library.mapNotNull { item ->
                            runCatching { item.toEntity() }.getOrNull()
                        }
                    val restoredMangaIds = entities.mapTo(mutableSetOf()) { it.manga_id }
                    val chapterProgress =
                        backup.chapterProgress.mapNotNull { item ->
                            runCatching { item.toEntity() }.getOrNull()
                        }.filter { it.manga_id in restoredMangaIds }
                    libraryRepository.restoreBackup(entities, chapterProgress)
                    _uiState.update { it.copy(restoreResult = BackupRestoreResult.Success(entities.size)) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(restoreResult = BackupRestoreResult.Failure(e.message ?: "")) }
                }
            }
        }

        companion object {
            const val KEEP_MIN = 1
            const val KEEP_MAX = 5
        }
    }
