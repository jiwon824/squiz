package com.ssafy.squiz.ui.screens.quiz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 상태 클래스들
sealed class TodayReviewUiState {
    object Loading : TodayReviewUiState()
    data class Success(
        val cards: List<ReviewCardDTO>,
        val dueCount: Int,
        val newCount: Int,
        val totalCount: Int
    ) : TodayReviewUiState()
    data class Error(val message: String) : TodayReviewUiState()
}

sealed class ReviewSessionState {
    object Idle : ReviewSessionState()
    object InProgress : ReviewSessionState()
    object Completed : ReviewSessionState()
}

sealed class ReviewStatsUiState {
    object Loading : ReviewStatsUiState()
    data class Success(val stats: ReviewStatsResponse) : ReviewStatsUiState()
    data class Error(val message: String) : ReviewStatsUiState()
}

sealed class QuizDetailUiState {
    object Loading : QuizDetailUiState()
    data class Success(val quiz: QuizDetailDTO) : QuizDetailUiState()
    data class Error(val message: String) : QuizDetailUiState()
}

// 세션 결과
data class ReviewResult(
    val cardId: Long,
    val rating: FsrsRating
)

class ReviewViewModel : ViewModel() {

    // 오늘 복습 상태
    private val _todayReviewState = MutableStateFlow<TodayReviewUiState>(TodayReviewUiState.Loading)
    val todayReviewState: StateFlow<TodayReviewUiState> = _todayReviewState.asStateFlow()

    // 통계 상태
    private val _statsState = MutableStateFlow<ReviewStatsUiState>(ReviewStatsUiState.Loading)
    val statsState: StateFlow<ReviewStatsUiState> = _statsState.asStateFlow()

    // 세션 상태
    private val _sessionState = MutableStateFlow<ReviewSessionState>(ReviewSessionState.Idle)
    val sessionState: StateFlow<ReviewSessionState> = _sessionState.asStateFlow()

    // 현재 카드 인덱스
    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    // 세션 결과
    private val _sessionResults = MutableStateFlow<List<ReviewResult>>(emptyList())
    val sessionResults: StateFlow<List<ReviewResult>> = _sessionResults.asStateFlow()

    // 퀴즈 상세 상태
    private val _quizDetailState = MutableStateFlow<QuizDetailUiState>(QuizDetailUiState.Loading)
    val quizDetailState: StateFlow<QuizDetailUiState> = _quizDetailState.asStateFlow()

    companion object {
        private const val TAG = "ReviewViewModel"
    }

    // 오늘 복습 로드 (실제 API 연동)
    fun loadTodayReviews() {
        viewModelScope.launch {
            _todayReviewState.value = TodayReviewUiState.Loading
            try {
                Log.d(TAG, "오늘 복습 로드 시작")
                val response = RetrofitClient.reviewApi.getTodayReviews()
                Log.d(TAG, "오늘 복습 API 응답: ${response.code()}")

                if (response.isSuccessful) {
                    val reviewResponse = response.body()?.data
                    Log.d(TAG, "복습 데이터: items=${reviewResponse?.items?.size}, totalCount=${reviewResponse?.totalCount}")

                    // 디버그: 각 카드의 options 확인
                    reviewResponse?.items?.forEachIndexed { index, card ->
                        val options = card.questionDetail?.options
                        Log.d(TAG, "카드[$index] question=${card.question.take(30)}..., options=${options?.size ?: 0}")
                        options?.forEach { option ->
                            Log.d(TAG, "  - option: id=${option.id}, label=${option.label}, text=${option.text}")
                        }
                    }

                    if (reviewResponse != null) {
                        _todayReviewState.value = TodayReviewUiState.Success(
                            cards = reviewResponse.cards,
                            dueCount = reviewResponse.dueCount,
                            newCount = reviewResponse.newCount,
                            totalCount = reviewResponse.totalCount
                        )
                    } else {
                        // 빈 데이터
                        Log.w(TAG, "복습 데이터 없음")
                        _todayReviewState.value = TodayReviewUiState.Success(
                            cards = emptyList(),
                            dueCount = 0,
                            newCount = 0,
                            totalCount = 0
                        )
                    }
                } else if (response.code() == 404) {
                    // API 미구현 또는 데이터 없음
                    Log.w(TAG, "복습 API 404 - 빈 목록으로 처리")
                    _todayReviewState.value = TodayReviewUiState.Success(
                        cards = emptyList(),
                        dueCount = 0,
                        newCount = 0,
                        totalCount = 0
                    )
                } else {
                    Log.e(TAG, "복습 API 실패: ${response.code()}")
                    _todayReviewState.value = TodayReviewUiState.Error("복습 카드를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "복습 로드 실패", e)
                _todayReviewState.value = TodayReviewUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 통계 로드 (실제 API 연동)
    fun loadStats() {
        viewModelScope.launch {
            _statsState.value = ReviewStatsUiState.Loading
            try {
                val response = RetrofitClient.reviewApi.getReviewStats()

                if (response.isSuccessful) {
                    val stats = response.body()?.data
                    if (stats != null) {
                        _statsState.value = ReviewStatsUiState.Success(stats)
                    } else {
                        // 기본 통계 - 백엔드 필드명 사용
                        _statsState.value = ReviewStatsUiState.Success(
                            ReviewStatsResponse(
                                totalItems = 0,
                                dueItems = 0,
                                newItems = 0,
                                learningItems = 0,
                                reviewItems = 0,
                                relearningItems = 0,
                                averageStability = 0.0,
                                totalReps = 0,
                                totalLapses = 0,
                                proficiency = 0.0
                            )
                        )
                    }
                } else {
                    _statsState.value = ReviewStatsUiState.Error("통계를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _statsState.value = ReviewStatsUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 세션 시작
    fun startSession() {
        _sessionState.value = ReviewSessionState.InProgress
        _currentCardIndex.value = 0
        _sessionResults.value = emptyList()
    }

    // 현재 카드 가져오기
    fun getCurrentCard(): ReviewCardDTO? {
        val state = _todayReviewState.value
        if (state is TodayReviewUiState.Success) {
            val index = _currentCardIndex.value
            return if (index < state.cards.size) state.cards[index] else null
        }
        return null
    }

    // 복습 제출 (실제 API 연동)
    fun submitReview(rating: FsrsRating) {
        val currentCard = getCurrentCard() ?: return

        viewModelScope.launch {
            try {
                // API 호출 - 백엔드 형식에 맞게 요청
                val request = ReviewSubmitRequest(
                    contentType = currentCard.contentType ?: "COURSE_QUESTION",
                    contentId = currentCard.contentId ?: currentCard.id,
                    userAnswer = if (rating == FsrsRating.GOOD || rating == FsrsRating.EASY)
                        currentCard.answer else null, // 정답 여부에 따라 userAnswer 설정
                    responseTimeMs = 3000 // 기본 응답 시간
                )
                RetrofitClient.reviewApi.submitReview(request)
                // API 실패 시에도 로컬에서는 진행 (오프라인 대응)
            } catch (e: Exception) {
                // 네트워크 에러 무시 - 오프라인 지원
            }

            // 로컬 결과 저장
            val result = ReviewResult(currentCard.id, rating)
            _sessionResults.value = _sessionResults.value + result

            // 다음 카드로 이동
            val state = _todayReviewState.value
            if (state is TodayReviewUiState.Success) {
                val nextIndex = _currentCardIndex.value + 1
                if (nextIndex >= state.cards.size) {
                    // 세션 완료
                    _sessionState.value = ReviewSessionState.Completed
                } else {
                    _currentCardIndex.value = nextIndex
                }
            }
        }
    }

    // 세션 리셋
    fun resetSession() {
        _sessionState.value = ReviewSessionState.Idle
        _currentCardIndex.value = 0
        _sessionResults.value = emptyList()
    }

    // 퀴즈 상세 로드 (실제 API 연동)
    fun loadQuizDetail(quizId: Long) {
        viewModelScope.launch {
            _quizDetailState.value = QuizDetailUiState.Loading
            try {
                val response = RetrofitClient.reviewApi.getQuizDetail(quizId)

                if (response.isSuccessful) {
                    val quiz = response.body()?.data
                    if (quiz != null) {
                        _quizDetailState.value = QuizDetailUiState.Success(quiz)
                    } else {
                        _quizDetailState.value = QuizDetailUiState.Error("퀴즈를 찾을 수 없습니다.")
                    }
                } else {
                    _quizDetailState.value = QuizDetailUiState.Error("퀴즈를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _quizDetailState.value = QuizDetailUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 퀴즈 결과 제출
    fun submitQuizResult(quizId: Long, answers: List<Int>, onResult: (Result<QuizResultDTO>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.reviewApi.submitQuizResult(quizId, answers)

                if (response.isSuccessful) {
                    val result = response.body()?.data
                    if (result != null) {
                        onResult(Result.success(result))
                    } else {
                        onResult(Result.failure(Exception("결과를 받지 못했습니다.")))
                    }
                } else {
                    onResult(Result.failure(Exception("퀴즈 제출에 실패했습니다. (${response.code()})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
