package com.ssafy.squiz.data.local.recording

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 로컬 녹음 파일 엔티티
 * - 녹음 완료 후 로컬에 저장
 * - 세션에 업로드하기 전까지 관리
 * - 7일 후 자동 삭제
 */
@Entity(tableName = "local_recordings")
data class LocalRecording(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // 파일 정보
    val filePath: String,
    val fileName: String,
    val durationSeconds: Int,
    val fileSizeBytes: Long,

    // 스터디/세션 정보
    val studyId: Long,
    val sessionId: Long? = null,  // null이면 미연결

    // 상태
    val uploaded: Boolean = false,
    val selected: Boolean = false,  // UI에서 체크박스 선택 상태
    val selectedOrder: Int? = null, // 선택 순서 (1, 2, 3... 업로드 순서 결정용)

    // 시간
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)  // 7일 후
) {
    /**
     * 시간 포맷팅 (MM:SS 또는 HH:MM:SS)
     */
    fun getFormattedDuration(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d분 %02d초", minutes, seconds)
        }
    }

    /**
     * 생성일 포맷팅
     */
    fun getFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(createdAt))
    }

    /**
     * 만료 여부
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}
