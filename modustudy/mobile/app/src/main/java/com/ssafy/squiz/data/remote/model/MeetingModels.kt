package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// ========== 회의/세션 녹음 모델 ==========

/**
 * 회의 목록 아이템 DTO
 */
data class MeetingDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("sessionId") val sessionId: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("duration") val duration: Long? = null, // 초 단위
    @SerializedName("status") val status: String? = null, // RECORDING, PROCESSING, COMPLETED, FAILED
    @SerializedName("participantCount") val participantCount: Int? = null,
    @SerializedName("hasSummary") val hasSummary: Boolean = false,
    @SerializedName("hasTranscript") val hasTranscript: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    // 편의 프로퍼티
    val displayDuration: String
        get() {
            val sec = duration ?: return "--:--"
            val hours = sec / 3600
            val minutes = (sec % 3600) / 60
            val seconds = sec % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val statusText: String
        get() = when (status) {
            "RECORDING" -> "녹음 중"
            "PROCESSING" -> "처리 중"
            "COMPLETED" -> "완료"
            "FAILED" -> "실패"
            else -> "대기"
        }
}

/**
 * 회의 상세 DTO (요약 포함)
 */
data class MeetingDetailDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("sessionId") val sessionId: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("participants") val participants: List<MeetingParticipantDTO>? = null,
    @SerializedName("transcript") val transcript: String? = null,
    @SerializedName("summary") val summary: MeetingSummaryDTO? = null,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

/**
 * 회의 참석자 DTO
 */
data class MeetingParticipantDTO(
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("joinedAt") val joinedAt: String? = null,
    @SerializedName("leftAt") val leftAt: String? = null,
    @SerializedName("speakingDuration") val speakingDuration: Long? = null // 초 단위
)

/**
 * AI 회의 요약 DTO
 */
data class MeetingSummaryDTO(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("meetingId") val meetingId: Long? = null,
    @SerializedName("summary") val summary: String? = null, // 전체 요약
    @SerializedName("keyPoints") val keyPoints: List<String>? = null, // 핵심 포인트
    @SerializedName("actionItems") val actionItems: List<ActionItemDTO>? = null, // 액션 아이템
    @SerializedName("decisions") val decisions: List<String>? = null, // 결정 사항
    @SerializedName("topics") val topics: List<DiscussedTopicDTO>? = null, // 논의 주제
    @SerializedName("generatedAt") val generatedAt: String? = null
)

/**
 * 액션 아이템 DTO
 */
data class ActionItemDTO(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("content") val content: String,
    @SerializedName("assignee") val assignee: String? = null, // 담당자
    @SerializedName("dueDate") val dueDate: String? = null,
    @SerializedName("status") val status: String? = null // PENDING, IN_PROGRESS, COMPLETED
)

/**
 * 논의 주제 DTO
 */
data class DiscussedTopicDTO(
    @SerializedName("topic") val topic: String,
    @SerializedName("duration") val duration: Long? = null, // 해당 주제 논의 시간 (초)
    @SerializedName("summary") val summary: String? = null
)

// ========== 녹음 관련 ==========

/**
 * 녹음 시작 요청 (미팅 생성)
 * studyId는 URL path로 전달되므로 body에서 제외
 */
data class RecordingStartRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("sessionId") val sessionId: Long? = null,
    @SerializedName("type") val type: String = "OFFLINE" // OFFLINE, ONLINE
)

/**
 * 녹음 시작 응답
 */
data class RecordingStartResponse(
    @SerializedName("meetingId") val meetingId: Long,
    @SerializedName("uploadUrl") val uploadUrl: String? = null // 사전 서명된 업로드 URL (선택)
)

/**
 * 녹음 종료 요청
 */
data class RecordingEndRequest(
    @SerializedName("meetingId") val meetingId: Long,
    @SerializedName("duration") val duration: Long // 총 녹음 시간 (초)
)

/**
 * 오디오 업로드 응답
 */
data class AudioUploadResponse(
    @SerializedName("meetingId") val meetingId: Long,
    @SerializedName("status") val status: String, // PROCESSING, COMPLETED
    @SerializedName("message") val message: String? = null
)

/**
 * 회의 상태 열거형
 */
enum class MeetingStatus(val label: String) {
    RECORDING("녹음 중"),
    PROCESSING("처리 중"),
    COMPLETED("완료"),
    FAILED("실패")
}

/**
 * 로컬 녹음 상태 (UI용)
 */
data class LocalRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val meetingId: Long? = null,
    val studyId: Long? = null,
    val sessionId: Long? = null,
    val startTime: Long = 0,
    val elapsedSeconds: Long = 0,
    val audioFilePath: String? = null
)

/**
 * 회의 종료 응답 DTO
 */
data class MeetingEndDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("status") val status: String,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("durationSeconds") val durationSeconds: Long? = null
)

/**
 * 녹취록 항목 DTO
 */
data class TranscriptItemDTO(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("timestampSeconds") val timestampSeconds: Int = 0,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    // 타임스탬프 포맷 (MM:SS)
    val formattedTimestamp: String
        get() {
            val mins = timestampSeconds / 60
            val secs = timestampSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}

/**
 * 미팅용 퀴즈 DTO (문제 목록 포함)
 */
data class MeetingQuizDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("questionCount") val questionCount: Int = 0,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("questions") val questions: List<QuizQuestionDTO>? = null
)

/**
 * 퀴즈 문제 DTO
 */
data class QuizQuestionDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("question") val question: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("correctIndex") val correctIndex: Int,
    @SerializedName("explanation") val explanation: String? = null
)
