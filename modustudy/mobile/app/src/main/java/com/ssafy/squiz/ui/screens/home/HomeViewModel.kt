package com.ssafy.squiz.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 상태 클래스
sealed class HomeState {
    object Loading : HomeState()
    data class Success(val data: HomeData) : HomeState()
    data class Error(val message: String) : HomeState()
}

sealed class NotificationsState {
    object Loading : NotificationsState()
    data class Success(val notifications: List<NotificationDTO>) : NotificationsState()
    data class Error(val message: String) : NotificationsState()
}

// 오늘의 복습 상태
sealed class TodayReviewState {
    object Loading : TodayReviewState()
    data class Success(
        val dueCount: Int,
        val newCount: Int,
        val totalCount: Int,
        val cards: List<ReviewCardDTO>
    ) : TodayReviewState()
    data class Error(val message: String) : TodayReviewState()
}

class HomeViewModel : ViewModel() {

    // 홈 상태
    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    // 읽지 않은 알림 수
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // 알림 상태
    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState.asStateFlow()

    // 오늘의 복습 상태
    private val _todayReviewState = MutableStateFlow<TodayReviewState>(TodayReviewState.Loading)
    val todayReviewState: StateFlow<TodayReviewState> = _todayReviewState.asStateFlow()

    init {
        loadHomeData()
        loadUnreadCount()
        loadTodayReview()
    }

    // 오늘의 복습 로드
    fun loadTodayReview() {
        viewModelScope.launch {
            _todayReviewState.value = TodayReviewState.Loading
            try {
                val response = RetrofitClient.reviewApi.getTodayReviews()
                if (response.isSuccessful) {
                    val reviewResponse = response.body()?.data
                    if (reviewResponse != null) {
                        _todayReviewState.value = TodayReviewState.Success(
                            dueCount = reviewResponse.dueCount,
                            newCount = reviewResponse.newCount,
                            totalCount = reviewResponse.totalCount,
                            cards = reviewResponse.cards
                        )
                    } else {
                        _todayReviewState.value = TodayReviewState.Success(
                            dueCount = 0,
                            newCount = 0,
                            totalCount = 0,
                            cards = emptyList()
                        )
                    }
                } else {
                    _todayReviewState.value = TodayReviewState.Error("복습 데이터를 불러오지 못했습니다.")
                }
            } catch (e: Exception) {
                _todayReviewState.value = TodayReviewState.Error(e.message ?: "네트워크 오류")
            }
        }
    }

    // 홈 데이터 로드 (실제 API 연동)
    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = HomeState.Loading
            try {
                // 여러 API를 병렬로 호출하여 홈 데이터 구성
                val userDeferred = async {
                    try {
                        val response = RetrofitClient.userApi.getMyProfile()
                        if (response.isSuccessful) response.body()?.data else null
                    } catch (e: Exception) { null }
                }

                val recommendedDeferred = async {
                    try {
                        val response = RetrofitClient.studyApi.getRecommendedStudies(limit = 5)
                        if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                    } catch (e: Exception) { emptyList() }
                }

                val recruitingDeferred = async {
                    try {
                        val response = RetrofitClient.studyApi.getRecruitingStudies(page = 0, size = 5)
                        if (response.isSuccessful) response.body()?.content ?: emptyList() else emptyList()
                    } catch (e: Exception) { emptyList() }
                }

                val myStudiesDeferred = async {
                    try {
                        val response = RetrofitClient.studyApi.getMyStudies(page = 0, size = 3)
                        if (response.isSuccessful) response.body()?.content ?: emptyList() else emptyList()
                    } catch (e: Exception) { emptyList() }
                }

                val scheduleCountDeferred = async {
                    try {
                        val response = RetrofitClient.scheduleApi.getTodaySchedules()
                        if (response.isSuccessful) response.body()?.data?.size ?: 0 else 0
                    } catch (e: Exception) { 0 }
                }

                val userProfile = userDeferred.await()
                val recommendedStudies = recommendedDeferred.await()
                val popularStudies = recruitingDeferred.await()
                val recentStudies = myStudiesDeferred.await()
                val scheduleCount = scheduleCountDeferred.await()

                val homeData = HomeData(
                    user = HomeUserInfo(
                        id = userProfile?.id ?: 0,
                        nickname = userProfile?.nickname ?: "사용자",
                        profileImage = userProfile?.profileImage
                    ),
                    todayScheduleCount = scheduleCount,
                    recommendedStudies = recommendedStudies,
                    popularStudies = popularStudies,
                    recentStudies = recentStudies
                )

                _homeState.value = HomeState.Success(homeData)
            } catch (e: Exception) {
                _homeState.value = HomeState.Error(e.message ?: "홈 데이터를 불러오는데 실패했습니다.")
            }
        }
    }

    // 읽지 않은 알림 수 로드 (실제 API 연동)
    private fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.notificationApi.getUnreadCount()
                if (response.isSuccessful) {
                    _unreadCount.value = response.body()?.data ?: 0
                } else {
                    _unreadCount.value = 0
                }
            } catch (e: Exception) {
                _unreadCount.value = 0
            }
        }
    }

    // 알림 목록 로드 (실제 API 연동)
    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = NotificationsState.Loading
            try {
                val response = RetrofitClient.notificationApi.getNotifications(page = 0, size = 50)

                if (response.isSuccessful) {
                    val notificationResponse = response.body()?.data
                    val notifications = notificationResponse?.notifications ?: emptyList()
                    _notificationsState.value = NotificationsState.Success(notifications)
                    _unreadCount.value = notificationResponse?.unreadCount ?: 0
                } else {
                    _notificationsState.value = NotificationsState.Error("알림을 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _notificationsState.value = NotificationsState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 알림 읽음 처리 (실제 API 연동)
    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.notificationApi.markAsRead(notificationId)

                if (response.isSuccessful) {
                    // 로컬 상태 업데이트
                    val currentState = _notificationsState.value
                    if (currentState is NotificationsState.Success) {
                        val updatedList = currentState.notifications.map { notification ->
                            if (notification.id == notificationId) {
                                notification.copy(isRead = true)
                            } else {
                                notification
                            }
                        }
                        _notificationsState.value = NotificationsState.Success(updatedList)
                        _unreadCount.value = updatedList.count { !it.isRead }
                    }
                }
            } catch (e: Exception) {
                // 에러 무시 - UI 상태만 업데이트 시도
            }
        }
    }

    // 모든 알림 읽음 처리 (실제 API 연동)
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.notificationApi.markAllAsRead()

                if (response.isSuccessful) {
                    // 로컬 상태 업데이트
                    val currentState = _notificationsState.value
                    if (currentState is NotificationsState.Success) {
                        val updatedList = currentState.notifications.map { it.copy(isRead = true) }
                        _notificationsState.value = NotificationsState.Success(updatedList)
                        _unreadCount.value = 0
                    }
                }
            } catch (e: Exception) {
                // 에러 무시
            }
        }
    }
}
