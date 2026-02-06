package com.ssafy.squiz.ui.screens.schedule

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.data.repository.AuthRepository
import com.ssafy.squiz.base.SquizApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 상태 클래스들
sealed class SchedulesState {
    object Loading : SchedulesState()
    data class Success(val schedules: List<ScheduleDTO>) : SchedulesState()
    data class Error(val message: String) : SchedulesState()
}

sealed class CalendarState {
    object Loading : CalendarState()
    data class Success(val data: CalendarData) : CalendarState()
    data class Error(val message: String) : CalendarState()
}

sealed class SessionDetailState {
    object Loading : SessionDetailState()
    data class Success(val session: SessionDTO) : SessionDetailState()
    data class Error(val message: String) : SessionDetailState()
}

sealed class GoogleSyncState {
    object Loading : GoogleSyncState()
    data class Success(val status: GoogleSyncStatus) : GoogleSyncState()
    data class Error(val message: String) : GoogleSyncState()
}

class ScheduleViewModel : ViewModel() {

    companion object {
        private const val TAG = "ScheduleViewModel"
    }

    private val authRepository: AuthRepository by lazy {
        AuthRepository(
            SquizApplication.getInstance(),
            SquizApplication.getInstance().authManager
        )
    }

    // 일정 목록 상태
    private val _schedulesState = MutableStateFlow<SchedulesState>(SchedulesState.Loading)
    val schedulesState: StateFlow<SchedulesState> = _schedulesState.asStateFlow()

    // 캘린더 상태
    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Loading)
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    // 세션 상세 상태
    private val _sessionDetailState = MutableStateFlow<SessionDetailState>(SessionDetailState.Loading)
    val sessionDetailState: StateFlow<SessionDetailState> = _sessionDetailState.asStateFlow()

    // 현재 세션에 녹음이 업로드되었는지 여부
    private val _isRecordingUploaded = MutableStateFlow(false)
    val isRecordingUploaded: StateFlow<Boolean> = _isRecordingUploaded.asStateFlow()

    // Google 동기화 상태
    private val _googleSyncState = MutableStateFlow<GoogleSyncState>(GoogleSyncState.Loading)
    val googleSyncState: StateFlow<GoogleSyncState> = _googleSyncState.asStateFlow()

    // 현재 연/월
    private val _currentYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // 스터디 이름 캐시 (studyId -> studyName)
    private val studyNameCache = mutableMapOf<Long, String>()

    // 일정 목록 로드 (실제 API 연동)
    fun loadSchedules() {
        viewModelScope.launch {
            _schedulesState.value = SchedulesState.Loading
            try {
                // 스터디 목록과 일정을 병렬로 로드
                val studiesDeferred = async { loadMyStudies() }

                // 오늘부터 30일 이후까지의 일정 조회
                val today = java.time.LocalDate.now()
                val endDate = today.plusDays(30)
                val startDateStr = today.toString() // YYYY-MM-DD
                val endDateStr = endDate.toString()

                val response = RetrofitClient.scheduleApi.getSchedules(startDateStr, endDateStr)
                studiesDeferred.await()  // 스터디 목록 로드 완료 대기

                if (response.isSuccessful) {
                    val sessionList = response.body() ?: emptyList()
                    val schedules = sessionList.map { session ->
                        val dto = session.toScheduleDTO()
                        // 스터디명이 없으면 캐시에서 조회
                        if (dto.studyName.startsWith("스터디 #")) {
                            dto.copy(studyName = studyNameCache[dto.studyId] ?: dto.studyName)
                        } else {
                            dto
                        }
                    }
                    _schedulesState.value = SchedulesState.Success(schedules)
                } else {
                    _schedulesState.value = SchedulesState.Error("일정을 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _schedulesState.value = SchedulesState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 내 스터디 목록 로드 (이름 캐시용)
    private suspend fun loadMyStudies() {
        try {
            val response = RetrofitClient.studyApi.getMyStudies()
            if (response.isSuccessful) {
                response.body()?.content?.forEach { study ->
                    studyNameCache[study.id] = study.name
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "스터디 목록 로드 실패: ${e.message}")
        }
    }

    // 캘린더 데이터 로드 (통합 API - 스터디 세션 + Google 캘린더)
    fun loadCalendarData() {
        viewModelScope.launch {
            _calendarState.value = CalendarState.Loading
            try {
                // 스터디 캐시가 비어있으면 먼저 로드
                if (studyNameCache.isEmpty()) {
                    loadMyStudies()
                }

                val year = _currentYear.value
                val month = _currentMonth.value

                // 해당 월의 시작일과 종료일 계산
                val startDate = java.time.LocalDate.of(year, month, 1)
                val endDate = startDate.plusMonths(1).minusDays(1)

                // 통합 API 호출 (스터디 세션 + Google 캘린더 이벤트)
                val response = RetrofitClient.scheduleApi.getAllSchedules(
                    startDate.toString(),
                    endDate.toString()
                )

                if (response.isSuccessful) {
                    val allSchedules = response.body()?.data

                    // 스터디 세션을 ScheduleDTO로 변환
                    val studySchedules = allSchedules?.studySessions?.mapNotNull { session ->
                        session.scheduledAt?.let { scheduledAt ->
                            try {
                                val dateTime = java.time.LocalDateTime.parse(scheduledAt)
                                val studyName = studyNameCache[session.studyId] ?: "스터디 #${session.studyId}"
                                ScheduleDTO(
                                    studyId = session.studyId,
                                    sessionId = session.id,
                                    studyName = studyName,
                                    date = dateTime.toLocalDate().toString(),
                                    startTime = dateTime.toLocalTime().toString().take(5),
                                    endTime = session.durationMinutes?.let {
                                        dateTime.plusMinutes(it.toLong()).toLocalTime().toString().take(5)
                                    } ?: "",
                                    location = session.location,
                                    isOnline = session.isOnline
                                )
                            } catch (e: Exception) { null }
                        }
                    } ?: emptyList()

                    // Google 캘린더 이벤트를 ScheduleDTO로 변환
                    val googleSchedules = allSchedules?.googleEvents?.mapNotNull { event ->
                        event.startTime?.let { startTime ->
                            try {
                                val dateTime = java.time.LocalDateTime.parse(startTime)
                                ScheduleDTO(
                                    studyId = -1,  // Google 이벤트 식별용
                                    sessionId = -1,
                                    studyName = "[Google] ${event.title ?: "일정"}",
                                    date = dateTime.toLocalDate().toString(),
                                    startTime = dateTime.toLocalTime().toString().take(5),
                                    endTime = event.endTime?.let {
                                        try {
                                            java.time.LocalDateTime.parse(it).toLocalTime().toString().take(5)
                                        } catch (e: Exception) { "" }
                                    } ?: "",
                                    location = event.location,
                                    isOnline = false
                                )
                            } catch (e: Exception) { null }
                        }
                    } ?: emptyList()

                    // 모든 일정 병합
                    val schedules = studySchedules + googleSchedules

                    // scheduledDays 추출 (일정이 있는 날짜들)
                    val scheduledDays = schedules.mapNotNull { schedule ->
                        try {
                            java.time.LocalDate.parse(schedule.date).dayOfMonth
                        } catch (e: Exception) { null }
                    }.distinct()

                    val calendarData = CalendarData(
                        scheduledDays = scheduledDays,
                        schedules = schedules
                    )
                    _calendarState.value = CalendarState.Success(calendarData)
                    Log.d(TAG, "캘린더 로드 완료: 스터디 ${studySchedules.size}개, Google ${googleSchedules.size}개")
                } else {
                    _calendarState.value = CalendarState.Error("캘린더 데이터를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "캘린더 로드 실패", e)
                _calendarState.value = CalendarState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 이전 달로 이동
    fun goToPreviousMonth() {
        if (_currentMonth.value == 1) {
            _currentMonth.value = 12
            _currentYear.value -= 1
        } else {
            _currentMonth.value -= 1
        }
        loadCalendarData()
    }

    // 다음 달로 이동
    fun goToNextMonth() {
        if (_currentMonth.value == 12) {
            _currentMonth.value = 1
            _currentYear.value += 1
        } else {
            _currentMonth.value += 1
        }
        loadCalendarData()
    }

    // 세션 상세 로드 (실제 API 연동 + 스터디 정보로 isLeader 확인)
    fun loadSessionDetail(studyId: Long, sessionId: Long) {
        viewModelScope.launch {
            _sessionDetailState.value = SessionDetailState.Loading
            _isRecordingUploaded.value = false  // 초기화
            try {
                // 세션 정보, 스터디 정보, 녹음 업로드 여부를 병렬로 조회
                val sessionDeferred = async {
                    RetrofitClient.scheduleApi.getSessionDetail(studyId, sessionId)
                }
                val studyDeferred = async {
                    try {
                        RetrofitClient.studyApi.getStudyDetail(studyId)
                    } catch (e: Exception) { null }
                }
                // 해당 세션에 녹음이 이미 업로드됐는지 확인
                val uploadedDeferred = async {
                    try {
                        RetrofitClient.meetingApi.getUploadedSessionIds(studyId, listOf(sessionId))
                    } catch (e: Exception) { null }
                }

                val sessionResponse = sessionDeferred.await()
                val studyResponse = studyDeferred.await()
                val uploadedResponse = uploadedDeferred.await()

                // 녹음 업로드 여부 설정
                val uploadedIds = uploadedResponse?.body()?.data ?: emptyList()
                _isRecordingUploaded.value = sessionId in uploadedIds

                if (sessionResponse.isSuccessful) {
                    // 백엔드는 StudySessionDTO를 직접 반환 (ApiResponse 래핑 없음)
                    val studySession = sessionResponse.body()
                    if (studySession != null) {
                        // 현재 사용자 ID와 스터디 리더 ID 비교하여 isLeader 결정
                        val currentUserId = SquizApplication.getInstance().authManager.getCurrentUserId()
                        val studyDetail = studyResponse?.body()
                        val leaderId = studyDetail?.leader?.id
                        val studyName = studyDetail?.name
                        val isLeader = currentUserId > 0 && leaderId != null && currentUserId == leaderId

                        // StudySessionDTO를 SessionDTO로 변환 (isLeader 포함)
                        val sessionDTO = studySession.toSessionDTO(studyName = studyName, isLeader = isLeader)

                        _sessionDetailState.value = SessionDetailState.Success(sessionDTO)
                    } else {
                        _sessionDetailState.value = SessionDetailState.Error("세션 정보를 찾을 수 없습니다.")
                    }
                } else {
                    _sessionDetailState.value = SessionDetailState.Error("세션 정보를 불러오는데 실패했습니다. (${sessionResponse.code()})")
                }
            } catch (e: Exception) {
                _sessionDetailState.value = SessionDetailState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // Google 캘린더 동기화 상태 로드 (실제 API 연동)
    fun loadGoogleSyncStatus() {
        viewModelScope.launch {
            _googleSyncState.value = GoogleSyncState.Loading
            try {
                val response = RetrofitClient.scheduleApi.getGoogleSyncStatus()

                if (response.isSuccessful) {
                    val status = response.body()?.data
                    if (status != null) {
                        _googleSyncState.value = GoogleSyncState.Success(status)
                    } else {
                        // 기본값: 연결되지 않음
                        _googleSyncState.value = GoogleSyncState.Success(
                            GoogleSyncStatus(isConnected = false, autoSync = false)
                        )
                    }
                } else {
                    // API가 없거나 실패해도 기본값 사용
                    _googleSyncState.value = GoogleSyncState.Success(
                        GoogleSyncStatus(isConnected = false, autoSync = false)
                    )
                }
            } catch (e: Exception) {
                // 에러 시에도 기본값 사용 (Google 캘린더는 선택적 기능)
                _googleSyncState.value = GoogleSyncState.Success(
                    GoogleSyncStatus(isConnected = false, autoSync = false)
                )
            }
        }
    }

    /**
     * Google 캘린더 연동을 위한 Sign-In Intent 생성
     */
    fun getGoogleCalendarSignInIntent(activity: Activity): Intent {
        val googleSignInClient = authRepository.getGoogleCalendarSignInClient(activity)
        return googleSignInClient.signInIntent
    }

    /**
     * Google Sign-In 결과 처리 및 백엔드에 연동 요청
     */
    fun handleGoogleCalendarSignInResult(data: Intent?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (data == null) {
                    onError("Google 로그인이 취소되었습니다.")
                    return@launch
                }

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val serverAuthCode = account?.serverAuthCode

                if (serverAuthCode != null) {
                    Log.d(TAG, "Google Calendar 인증 코드 획득, 백엔드 연동 시작")
                    connectGoogleCalendarWithCode(serverAuthCode, onSuccess, onError)
                } else {
                    onError("서버 인증 코드를 받지 못했습니다.")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In 실패: ${e.statusCode}")
                onError("Google 로그인에 실패했습니다. (${e.statusCode})")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In 처리 오류: ${e.message}")
                onError(e.message ?: "알 수 없는 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 백엔드에 Google Calendar 연동 요청
     */
    private suspend fun connectGoogleCalendarWithCode(
        serverAuthCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            _googleSyncState.value = GoogleSyncState.Loading

            val response = RetrofitClient.scheduleApi.connectGoogleCalendar(
                mapOf("code" to serverAuthCode)
            )

            if (response.isSuccessful) {
                val status = response.body()?.data
                if (status != null) {
                    _googleSyncState.value = GoogleSyncState.Success(status)
                    Log.d(TAG, "Google Calendar 연동 성공")
                    onSuccess()
                } else {
                    _googleSyncState.value = GoogleSyncState.Success(
                        GoogleSyncStatus(isConnected = true, autoSync = false)
                    )
                    onSuccess()
                }
            } else {
                val errorMsg = "캘린더 연동에 실패했습니다. (${response.code()})"
                _googleSyncState.value = GoogleSyncState.Error(errorMsg)
                onError(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "네트워크 오류가 발생했습니다."
            _googleSyncState.value = GoogleSyncState.Error(errorMsg)
            onError(errorMsg)
        }
    }

    // Google 캘린더 연결 해제 (실제 API 연동)
    fun disconnectGoogleCalendar() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.scheduleApi.disconnectGoogleCalendar()

                if (response.isSuccessful) {
                    _googleSyncState.value = GoogleSyncState.Success(
                        GoogleSyncStatus(isConnected = false, autoSync = false)
                    )
                } else {
                    _googleSyncState.value = GoogleSyncState.Error("연결 해제에 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _googleSyncState.value = GoogleSyncState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 일정 동기화 (실제 API 연동)
    fun syncSchedules() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.scheduleApi.syncSchedules()

                if (response.isSuccessful) {
                    // 동기화 후 상태 새로고침
                    loadGoogleSyncStatus()
                    loadCalendarData()
                }
            } catch (e: Exception) {
                // 동기화 실패 시 상태 유지
            }
        }
    }
}
