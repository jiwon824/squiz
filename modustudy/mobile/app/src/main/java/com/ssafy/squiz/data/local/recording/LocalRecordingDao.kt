package com.ssafy.squiz.data.local.recording

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 로컬 녹음 DAO
 */
@Dao
interface LocalRecordingDao {

    /**
     * 스터디의 모든 녹음 조회 (미업로드 + 업로드 완료)
     */
    @Query("SELECT * FROM local_recordings WHERE studyId = :studyId ORDER BY createdAt DESC")
    fun getRecordingsByStudyId(studyId: Long): Flow<List<LocalRecording>>

    /**
     * 스터디의 미업로드 녹음만 조회
     */
    @Query("SELECT * FROM local_recordings WHERE studyId = :studyId AND uploaded = 0 ORDER BY createdAt DESC")
    fun getUnuploadedRecordings(studyId: Long): Flow<List<LocalRecording>>

    /**
     * 특정 세션의 녹음 조회
     */
    @Query("SELECT * FROM local_recordings WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getRecordingsBySessionId(sessionId: Long): Flow<List<LocalRecording>>

    /**
     * 선택된 녹음들 조회 (선택 순서대로)
     */
    @Query("SELECT * FROM local_recordings WHERE studyId = :studyId AND selected = 1 ORDER BY selectedOrder ASC")
    suspend fun getSelectedRecordings(studyId: Long): List<LocalRecording>

    /**
     * 현재 최대 선택 순서 조회 (미업로드 + 현재 선택된 녹음만)
     */
    @Query("SELECT COALESCE(MAX(selectedOrder), 0) FROM local_recordings WHERE studyId = :studyId AND uploaded = 0 AND selected = 1")
    suspend fun getMaxSelectedOrder(studyId: Long): Int

    /**
     * 만료된 녹음 조회
     */
    @Query("SELECT * FROM local_recordings WHERE expiresAt < :currentTime")
    suspend fun getExpiredRecordings(currentTime: Long = System.currentTimeMillis()): List<LocalRecording>

    /**
     * 녹음 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: LocalRecording)

    /**
     * 녹음 업데이트
     */
    @Update
    suspend fun update(recording: LocalRecording)

    /**
     * 녹음 삭제
     */
    @Delete
    suspend fun delete(recording: LocalRecording)

    /**
     * ID로 녹음 삭제
     */
    @Query("DELETE FROM local_recordings WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 녹음 선택 (순서 부여)
     */
    @Query("UPDATE local_recordings SET selected = 1, selectedOrder = :order WHERE id = :id")
    suspend fun selectRecording(id: String, order: Int)

    /**
     * 녹음 선택 해제
     */
    @Query("UPDATE local_recordings SET selected = 0, selectedOrder = NULL WHERE id = :id")
    suspend fun deselectRecording(id: String)

    /**
     * ID로 녹음 조회
     */
    @Query("SELECT * FROM local_recordings WHERE id = :id")
    suspend fun getById(id: String): LocalRecording?

    /**
     * 모든 선택 해제
     */
    @Query("UPDATE local_recordings SET selected = 0, selectedOrder = NULL WHERE studyId = :studyId")
    suspend fun clearAllSelections(studyId: Long)

    /**
     * 업로드 완료로 표시
     */
    @Query("UPDATE local_recordings SET uploaded = 1, sessionId = :sessionId WHERE id = :id")
    suspend fun markAsUploaded(id: String, sessionId: Long)

    /**
     * 선택된 녹음들 업로드 완료로 표시
     */
    @Query("UPDATE local_recordings SET uploaded = 1, sessionId = :sessionId WHERE studyId = :studyId AND selected = 1")
    suspend fun markSelectedAsUploaded(studyId: Long, sessionId: Long)

    /**
     * 만료된 녹음 일괄 삭제
     */
    @Query("DELETE FROM local_recordings WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())

    /**
     * 오늘 녹음한 총 시간 (초)
     */
    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0)
        FROM local_recordings
        WHERE studyId = :studyId
        AND createdAt >= :todayStart
    """)
    suspend fun getTodayTotalDuration(studyId: Long, todayStart: Long): Int
}
