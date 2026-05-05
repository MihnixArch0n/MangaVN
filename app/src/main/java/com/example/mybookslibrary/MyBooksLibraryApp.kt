package com.example.mybookslibrary

import android.app.Application
import android.util.Log
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.di.IoDispatcher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyBooksLibraryApp : Application() {

	@Inject
	lateinit var libraryRepository: LibraryRepository

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	private val appScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

	override fun onCreate() {
		super.onCreate()

		appScope.launch {
			runCatching {
				libraryRepository.debugClearAndReseed()
				Log.d(TAG, "Startup mock reseed completed")
			}.onFailure { throwable ->
				Log.e(TAG, "Startup mock reseed failed", throwable)
			}
		}
	}

	companion object {
		private const val TAG = "MyBooksLibraryApp"
	}
}

