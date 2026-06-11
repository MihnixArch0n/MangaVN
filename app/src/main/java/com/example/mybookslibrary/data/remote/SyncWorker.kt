package com.example.mybookslibrary.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mybookslibrary.data.repository.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryRepository: LibraryRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = try {
            Timber.d("SyncWorker: Starting Firestore sync")
            libraryRepository.performSync()
            Timber.d("SyncWorker: Sync completed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Sync failed")
            Result.retry()
        }
}
