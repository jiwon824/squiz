package com.ssafy.squiz.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
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

    // 일정 목록 상태
    private val _schedulesState = MutableStateFlow<SchedulesState>(SchedulesState.Loading)
    val schedulesState: StateFlow<SchedulesState> = _schedulesState.asStateFlow()

    // 캘린더 상태
    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Loading)
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    // 세션 상세 상태
    private val _sessionDetailState = MutableStateFlow<SessionDetailState>(SessionDetailState.Loading)
    val sessionDetailState: StateFlow<SessionDetailState> = _sessionDetailState.asStateFlow()

    // Google 동기화 상태
    private val _googleSyncState = MutableStateFlow<GoogleSyncState>(GoogleSyncState.Loading)
    val googleSyncState: StateFlow<GoogleSyncState> = _googleSyncState.asStateFlow()

    // 현재 연/월
    private val _currentYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // 일정 목록 로드 (실제 API 연동)
    fun loadSchedules() {
        viewModelScope.launch {
            _schedulesState.value = SchedulesState.Loading
            try {
                val response = RetrofitClient.scheduleApi.getSchedules()

                if (response.isSuccessful) {
                    val scheduleListResponse = response.body()?.data
                    val schedules = scheduleListResponse?.sessions ?: emptyList()
                    _schedulesState.value = SchedulesState.Success(schedules)
                } else {
                    _schedulesState.value = SchedulesState.Error("일정을 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _schedulesState.value = SchedulesState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 캘린더 데이터 로드 (실제 API 연동)
    fun loadCalendarData() {
        viewModelScope.launch {
            _calendarState.value = CalendarState.Loading
            try {
                val year = _currentYear.value
                val month = _currentMonth.value

                val response = RetrofitClient.scheduleApi.getMonthSchedules(year, month)

                if (response.isSuccessful) {
                    val calendarData = response.body()?.data
                    if (calendarData != null) {
                        _calendarState.value = CalendarState.Success(calendarData)
                    } else {
                        // 데이터가 없으면 빈 캘린더
                        _calendarState.value = CalendarState.Success(
                            CalendarData(scheduledDays = emptyList(), schedules = emptyList())
                        )
                    }
                } else {
                    _calendarState.value = CalendarState.Error("캘린더 데이터를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
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

    // 세션 상세 로드 (실제 API 연동)
    fun loadSessionDetail(studyId: Long, sessionId: Long) {
        viewModelScope.launch {
            _sessionDetailState.value = SessionDetailState.Loading
            try {
                val response = RetrofitClient.scheduleApi.getSessionDetail(studyId, sessionId)

                if (response.isSuccessful) {
                    val session = response.body()?.data
                    if (session != null) {
                        _sessionDetailState.value = SessionDetailState.Success(session)
                    } else {
                        _sessionDetailState.value = SessionDetailState.Error("세션 정보를 찾을 수 없습니다.")
                    }
                } else {
                    _sessionDetailState.value = SessionDetailState.Error("세션 정보를 불러오는데 실패했습니다. (${response.code()})")
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

    // Google 캘린더 연결
    fun connectGoogleCalendar() {
        viewModelScope.launch {
            // TODO: OAuth 인증 후 연결
            loadGoogleSyncStatus()
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
