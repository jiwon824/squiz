package com.ssafy.squiz.data.local.recording

import android.content.Context
import com.ssafy.squiz.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Calendar

/**
 * 로컬 녹음 Repository
 */
class LocalRecordingRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).localRecordingDao()

    /**
     * 스터디의 미업로드 녹음 목록
     */
    fun getUnuploadedRecordings(studyId: Long): Flow<List<LocalRecording>> {
        return dao.getUnuploadedRecordings(studyId)
    }

    /**
     * 스터디의 모든 녹음 목록
     */
    fun getAllRecordings(studyId: Long): Flow<List<LocalRecording>> {
        return dao.getRecordingsByStudyId(studyId)
    }

    /**
     * 새 녹음 저장
     */
    suspend fun saveRecording(
        filePath: String,
        fileName: String,
        durationSeconds: Int,
        fileSizeBytes: Long,
        studyId: Long,
        sessionId: Long? = null
    ): LocalRecording {
        val recording = LocalRecording(
            filePath = filePath,
            fileName = fileName,
            durationSeconds = durationSeconds,
            fileSizeBytes = fileSizeBytes,
            studyId = studyId,
            sessionId = sessionId
        )
        dao.insert(recording)
        return recording
    }

    /**
     * 선택 토글 (순서 부여/해제)
     */
    suspend fun toggleSelection(id: String, studyId: Long) {
        val recording = dao.getById(id) ?: return
        if (recording.selected) {
            // 선택 해제
            dao.deselectRecording(id)
        } else {
            // 선택 - 현재 최대 순서 + 1 부여
            val nextOrder = dao.getMaxSelectedOrder(studyId) + 1
            dao.selectRecording(id, nextOrder)
        }
    }

    /**
     * 모든 선택 해제
     */
    suspend fun clearAllSelections(studyId: Long) {
        dao.clearAllSelections(studyId)
    }

    /**
     * 선택된 녹음들 조회
     */
    suspend fun getSelectedRecordings(studyId: Long): List<LocalRecording> {
        return dao.getSelectedRecordings(studyId)
    }

    /**
     * 선택된 녹음들 업로드 완료 처리
     */
    suspend fun markSelectedAsUploaded(studyId: Long, sessionId: Long) {
        dao.markSelectedAsUploaded(studyId, sessionId)
    }

    /**
     * 녹음 삭제 (파일도 함께 삭제)
     */
    suspend fun deleteRecording(recording: LocalRecording) {
        // 파일 삭제
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // DB에서 삭제
        dao.delete(recording)
    }

    /**
     * ID로 녹음 삭제
     */
    suspend fun deleteRecordingById(id: String, filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.deleteById(id)
    }

    /**
     * 만료된 녹음들 삭제
     */
    suspend fun deleteExpiredRecordings() {
        val expired = dao.getExpiredRecordings()
        expired.forEach { recording ->
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dao.deleteExpired()
    }

    /**
     * 오늘 녹음 가능한 남은 시간 (초)
     * 하루 최대 2시간 (7200초)
     */
    suspend fun getTodayRemainingSeconds(studyId: Long): Int {
        val todayStart = getTodayStartMillis()
        val usedSeconds = dao.getTodayTotalDuration(studyId, todayStart)
        val maxSeconds = 2 * 60 * 60  // 2시간
        return maxOf(0, maxSeconds - usedSeconds)
    }

    /**
     * 오늘 자정 timestamp
     */
    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
