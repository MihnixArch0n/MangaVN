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

    override suspend fun doWork(): Result {
        return try {
            Timber.d("SyncWorker: Bắt đầu đồng bộ Firestore")
            libraryRepository.syncPendingItems()
            Timber.d("SyncWorker: Đồng bộ hoàn tất")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Lỗi đồng bộ")
            Result.retry()
        }
    }
}
