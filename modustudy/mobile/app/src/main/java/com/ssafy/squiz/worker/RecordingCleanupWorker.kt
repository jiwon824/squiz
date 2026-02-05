package com.ssafy.squiz.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ssafy.squiz.data.local.recording.LocalRecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 만료된 로컬 녹음 파일을 자동 삭제하는 Worker
 * 7일이 지난 미업로드 녹음 파일들을 정리합니다.
 */
class RecordingCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RecordingCleanupWorker"
        private const val WORK_NAME = "recording_cleanup_work"

        /**
         * 하루에 한 번 실행되는 주기적 정리 작업 스케줄링
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)  // 배터리 부족 시 실행 안 함
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<RecordingCleanupWorker>(
                1, TimeUnit.DAYS  // 하루에 한 번 실행
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)  // 앱 시작 후 1시간 뒤부터 시작
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,  // 기존 작업 유지
                    cleanupRequest
                )

            Log.d(TAG, "녹음 파일 정리 작업 스케줄링 완료")
        }

        /**
         * 즉시 정리 작업 실행 (디버깅/테스트용)
         */
        fun runOnce(context: Context) {
            val cleanupRequest = OneTimeWorkRequestBuilder<RecordingCleanupWorker>()
                .build()

            WorkManager.getInstance(context)
                .enqueue(cleanupRequest)

            Log.d(TAG, "녹음 파일 정리 작업 즉시 실행 요청")
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "만료된 녹음 파일 정리 시작")

                val repository = LocalRecordingRepository(applicationContext)
                repository.deleteExpiredRecordings()

                Log.d(TAG, "만료된 녹음 파일 정리 완료")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "녹음 파일 정리 실패", e)
                Result.retry()  // 실패 시 재시도
            }
        }
    }
}
