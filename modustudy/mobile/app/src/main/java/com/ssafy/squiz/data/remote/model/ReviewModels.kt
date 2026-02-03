package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// FSRS 복습 시스템

// 복습 카드 (백엔드 ReviewItemDto에 맞춤)
data class ReviewCardDTO(
    @SerializedName("reviewItemId") val id: Long,
    @SerializedName("contentType") val contentType: String? = null,
    @SerializedName("contentId") val contentId: Long? = null,
    @SerializedName("stability") val stability: Double? = null,
    @SerializedName("difficulty") val difficulty: Double? = null,
    @SerializedName("state") val state: Int? = null,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("lapses") val lapses: Int? = null,
    @SerializedName("nextReviewAt") val nextReviewAt: String? = null,
    @SerializedName("question") val questionDetail: QuestionDetailDTO? = null
) {
    // UI 호환성을 위한 편의 프로퍼티
    val question: String get() = questionDetail?.questionText ?: ""
    val answer: String get() = questionDetail?.correctAnswer ?: ""
    val studyName: String? get() = questionDetail?.category
}

// 문제 상세 정보 (백엔드 QuestionDetail에 맞춤)
data class QuestionDetailDTO(
    @SerializedName("questionNumber") val questionNumber: Int? = null,
    @SerializedName("questionText") val questionText: String? = null,
    @SerializedName("questionType") val questionType: String? = null,
    @SerializedName("options") val options: List<OptionItemDTO>? = null,
    @SerializedName("correctAnswer") val correctAnswer: String? = null,
    @SerializedName("explanation") val explanation: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("lastReviewAt") val lastReviewAt: String? = null
)

// 객관식 보기 아이템
data class OptionItemDTO(
    @SerializedName("id") val id: String? = null,         // 백엔드에서 "A", "B", "C", "D" 등으로 전송
    @SerializedName("label") val label: String? = null,   // 호환성
    @SerializedName("text") val text: String? = null
) {
    // id 또는 label 중 존재하는 값을 반환
    val optionLabel: String get() = id ?: label ?: ""
}

// 오늘 복습 응답 (백엔드 TodayReviewResponse에 맞춤)
data class TodayReviewResponse(
    @SerializedName("items") val items: List<ReviewCardDTO>,
    @SerializedName("totalCount") val totalCount: Int
) {
    // UI 호환성을 위한 편의 프로퍼티
    val cards: List<ReviewCardDTO> get() = items
    val dueCount: Int get() = items.count { (it.state ?: 0) >= 1 }
    val newCount: Int get() = items.count { (it.state ?: 0) == 0 }
}

// 복습 제출 요청 (백엔드 ReviewSubmitRequest에 맞춤)
data class ReviewSubmitRequest(
    @SerializedName("contentType") val contentType: String, // COURSE_QUESTION, STUDY_QUESTION
    @SerializedName("contentId") val contentId: Long,
    @SerializedName("userAnswer") val userAnswer: String? = null,
    @SerializedName("responseTimeMs") val responseTimeMs: Long = 3000
)

// 복습 통계 (백엔드 ReviewStatsResponse에 맞춤)
data class ReviewStatsResponse(
    @SerializedName("totalItems") val totalItems: Int = 0,
    @SerializedName("dueItems") val dueItems: Int = 0,
    @SerializedName("newItems") val newItems: Int = 0,
    @SerializedName("learningItems") val learningItems: Int = 0,
    @SerializedName("reviewItems") val reviewItems: Int = 0,
    @SerializedName("relearningItems") val relearningItems: Int = 0,
    @SerializedName("averageStability") val averageStability: Double = 0.0,
    @SerializedName("totalReps") val totalReps: Int = 0,
    @SerializedName("totalLapses") val totalLapses: Int = 0,
    @SerializedName("proficiency") val proficiency: Double = 0.0
) {
    // UI 호환성을 위한 편의 프로퍼티
    val totalCards: Int get() = totalItems
    val reviewedToday: Int get() = totalReps
    val streak: Int get() = 0 // 백엔드에서 제공하지 않음
    val averageRetention: Float? get() = proficiency.toFloat()
    val masteredCards: Int get() = reviewItems
}

// FSRS 평점
enum class FsrsRating(val value: Int, val label: String) {
    AGAIN(1, "다시"),
    HARD(2, "어려움"),
    GOOD(3, "좋음"),
    EASY(4, "쉬움")
}

// 틀린 문제 정렬 타입
enum class WrongAnswerSortType(val value: String) {
    MOST_WRONG("MOST_WRONG"),         // 많이 틀린 순
    FSRS_RECOMMENDED("FSRS_RECOMMENDED") // 복습 우선순위
}

// 퀴즈 관련

// 퀴즈 목록 아이템
data class QuizDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("questionCount") val questionCount: Int = 0,
    @SerializedName("timeLimit") val timeLimit: Int? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("difficulty") val difficulty: String? = null
)

// 퀴즈 상세
data class QuizDetailDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("questions") val questions: List<QuestionDTO>,
    @SerializedName("timeLimit") val timeLimit: Int? = null
)

// 문제
data class QuestionDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("question") val question: String,
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("correctAnswer") val correctAnswer: Int? = null,
    @SerializedName("explanation") val explanation: String? = null,
    @SerializedName("type") val type: String? = null // MULTIPLE_CHOICE, SHORT_ANSWER, etc.
)

// 퀴즈 결과
data class QuizResultDTO(
    @SerializedName("attemptId") val attemptId: Long,
    @SerializedName("quizId") val quizId: Long,
    @SerializedName("score") val score: Int,
    @SerializedName("correctCount") val correctCount: Int,
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("timeSpent") val timeSpent: Int? = null,
    @SerializedName("completedAt") val completedAt: String? = null
)
