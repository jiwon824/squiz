package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// FSRS 복습 시스템

// 복습 카드
data class ReviewCardDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long? = null,
    @SerializedName("studyName") val studyName: String? = null,
    @SerializedName("question") val question: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("difficulty") val difficulty: Float? = null,
    @SerializedName("stability") val stability: Float? = null,
    @SerializedName("dueDate") val dueDate: String? = null,
    @SerializedName("lastReviewDate") val lastReviewDate: String? = null,
    @SerializedName("reviewCount") val reviewCount: Int? = null
)

// 오늘 복습 응답
data class TodayReviewResponse(
    @SerializedName("cards") val cards: List<ReviewCardDTO>,
    @SerializedName("dueCount") val dueCount: Int,
    @SerializedName("newCount") val newCount: Int,
    @SerializedName("totalCount") val totalCount: Int
)

// 복습 제출 요청
data class ReviewSubmitRequest(
    @SerializedName("cardId") val cardId: Long,
    @SerializedName("rating") val rating: Int // 1: Again, 2: Hard, 3: Good, 4: Easy
)

// 복습 통계
data class ReviewStatsResponse(
    @SerializedName("totalCards") val totalCards: Int = 0,
    @SerializedName("reviewedToday") val reviewedToday: Int = 0,
    @SerializedName("streak") val streak: Int = 0,
    @SerializedName("averageRetention") val averageRetention: Float? = null,
    @SerializedName("masteredCards") val masteredCards: Int = 0
)

// FSRS 평점
enum class FsrsRating(val value: Int, val label: String) {
    AGAIN(1, "다시"),
    HARD(2, "어려움"),
    GOOD(3, "좋음"),
    EASY(4, "쉬움")
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
