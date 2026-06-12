package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val username: String = "",
    val displayName: String = "",
    val avatarUri: String = "",
    val bookmarkCount: Int = 0,
    val mangaReadCount: Int = 0,
    val chaptersCompleted: Int = 0,
)

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        preferencesDataStore: UserPreferencesDataStore,
        libraryDao: LibraryDao,
        chapterDao: ChapterDao,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        val uiState: StateFlow<ProfileUiState> =
            combine(
                preferencesDataStore.observeDisplayName(),
                preferencesDataStore.observeAvatarUri(),
                libraryDao.observeCount(),
                libraryDao.observeReadCount(),
            ) { displayName, avatarUri, bookmarks, mangaRead ->
                val user = authRepository.getCurrentUser()
                ProfileUiState(
                    username = user?.email.orEmpty(),
                    displayName = displayName,
                    avatarUri = avatarUri,
                    bookmarkCount = bookmarks,
                    mangaReadCount = mangaRead,
                )
            }.combine(chapterDao.observeCompletedChapterCount()) { state, chapters ->
                state.copy(chaptersCompleted = chapters)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
                ProfileUiState(),
            )

        companion object {
            private const val SUBSCRIPTION_TIMEOUT = 5_000L
        }
    }
