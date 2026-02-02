package com.ssafy.squiz.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ssafy.squiz.MainActivity
import com.ssafy.squiz.R
import com.ssafy.squiz.data.remote.model.LocalRecordingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 세션 녹음 서비스
 * Foreground Service로 백그라운드에서도 녹음을 유지합니다.
 */
class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val CHANNEL_NAME = "세션 녹음"

        // 액션 상수
        const val ACTION_START_RECORDING = "com.ssafy.squiz.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.ssafy.squiz.STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.ssafy.squiz.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.ssafy.squiz.RESUME_RECORDING"

        // Extra 키
        const val EXTRA_STUDY_ID = "study_id"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_MEETING_ID = "meeting_id"

        // 전역 상태 (UI에서 접근)
        private val _recordingState = MutableStateFlow(LocalRecordingState())
        val recordingState: StateFlow<LocalRecordingState> = _recordingState.asStateFlow()

        // 현재 녹음 파일 경로
        var currentAudioFilePath: String? = null
            private set
    }

    private val binder = RecordingBinder()
    private var mediaRecorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RecordingService 생성")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val studyId = intent.getLongExtra(EXTRA_STUDY_ID, -1L)
                val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                val meetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1L)

                if (studyId != -1L) {
                    startRecording(studyId, if (sessionId != -1L) sessionId else null, if (meetingId != -1L) meetingId else null)
                }
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
            ACTION_PAUSE_RECORDING -> {
                pauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                resumeRecording()
            }
        }

        return START_STICKY
    }

    /**
     * 녹음 시작
     */
    private fun startRecording(studyId: Long, sessionId: Long?, meetingId: Long?) {
        if (_recordingState.value.isRecording) {
            Log.w(TAG, "이미 녹음 중입니다.")
            return
        }

        try {
            // 녹음 파일 경로 생성
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "recording_${studyId}_${timestamp}.m4a"
            val outputFile = File(getExternalFilesDir(null), fileName)
            currentAudioFilePath = outputFile.absolutePath

            // MediaRecorder 설정
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            // 상태 업데이트
            _recordingState.value = LocalRecordingState(
                isRecording = true,
                isPaused = false,
                meetingId = meetingId,
                studyId = studyId,
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                elapsedSeconds = 0,
                audioFilePath = outputFile.absolutePath
            )

            // 포그라운드 서비스 시작
            startForeground(NOTIFICATION_ID, createNotification(0))

            // 타이머 시작
            startTimer()

            Log.d(TAG, "녹음 시작: $currentAudioFilePath")

        } catch (e: Exception) {
            Log.e(TAG, "녹음 시작 실패", e)
            cleanupRecording()
        }
    }

    /**
     * 녹음 중지
     */
    fun stopRecording(): String? {
        try {
            timerJob?.cancel()

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val filePath = currentAudioFilePath

            // 상태 초기화
            _recordingState.value = LocalRecordingState()

            // 포그라운드 서비스 중지
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()

            Log.d(TAG, "녹음 중지: $filePath")
            return filePath

        } catch (e: Exception) {
            Log.e(TAG, "녹음 중지 실패", e)
            cleanupRecording()
            return null
        }
    }

    /**
     * 녹음 일시정지
     */
    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordingState.value.isRecording && !_recordingState.value.isPaused) {
            try {
                mediaRecorder?.pause()
                timerJob?.cancel()
                _recordingState.value = _recordingState.value.copy(isPaused = true)
                updateNotification()
                Log.d(TAG, "녹음 일시정지")
            } catch (e: Exception) {
                Log.e(TAG, "녹음 일시정지 실패", e)
            }
        }
    }

    /**
     * 녹음 재개
     */
    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _recordingState.value.isRecording && _recordingState.value.isPaused) {
            try {
                mediaRecorder?.resume()
                _recordingState.value = _recordingState.value.copy(isPaused = false)
                startTimer()
                Log.d(TAG, "녹음 재개")
            } catch (e: Exception) {
                Log.e(TAG, "녹음 재개 실패", e)
            }
        }
    }

    /**
     * 타이머 시작 (1초마다 업데이트)
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _recordingState.value.isRecording && !_recordingState.value.isPaused) {
                delay(1000)
                val currentState = _recordingState.value
                _recordingState.value = currentState.copy(
                    elapsedSeconds = currentState.elapsedSeconds + 1
                )
                updateNotification()
            }
        }
    }

    /**
     * 알림 채널 생성
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "스터디 세션 녹음 중 알림"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 알림 생성
     */
    private fun createNotification(elapsedSeconds: Long): Notification {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        val timeText = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val isPaused = _recordingState.value.isPaused
        val statusText = if (isPaused) "일시정지됨" else "녹음 중"

        // 메인 액티비티로 이동하는 Intent
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 녹음 중지 액션
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("스터디 세션 $statusText")
            .setContentText("녹음 시간: $timeText")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_notification, "중지", stopPendingIntent)
            .build()
    }

    /**
     * 알림 업데이트
     */
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(_recordingState.value.elapsedSeconds))
    }

    /**
     * 녹음 정리
     */
    private fun cleanupRecording() {
        timerJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder 해제 실패", e)
        }
        mediaRecorder = null
        _recordingState.value = LocalRecordingState()
        currentAudioFilePath = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "RecordingService 종료")
        serviceScope.cancel()
        cleanupRecording()
        super.onDestroy()
    }
}
