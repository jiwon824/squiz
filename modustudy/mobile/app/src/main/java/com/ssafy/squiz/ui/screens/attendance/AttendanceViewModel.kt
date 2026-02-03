package com.ssafy.squiz.ui.screens.attendance

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.ble.BleAdvertiser
import com.ssafy.squiz.ble.BleScanner
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// UI 상태 클래스들
sealed class AttendanceUiState {
    object Idle : AttendanceUiState()
    object Scanning : AttendanceUiState()
    object Advertising : AttendanceUiState()
    data class Success(val message: String) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
}

sealed class SessionAttendanceUiState {
    object Loading : SessionAttendanceUiState()
    data class Success(val info: SessionAttendanceInfoDTO) : SessionAttendanceUiState()
    data class Error(val message: String) : SessionAttendanceUiState()
}

sealed class AttendanceStatsUiState {
    object Loading : AttendanceStatsUiState()
    data class Success(val stats: AttendanceStatsDTO) : AttendanceStatsUiState()
    data class Error(val message: String) : AttendanceStatsUiState()
}

sealed class AttendanceHistoryUiState {
    object Loading : AttendanceHistoryUiState()
    data class Success(val history: List<AttendanceStatusDTO>) : AttendanceHistoryUiState()
    data class Error(val message: String) : AttendanceHistoryUiState()
}

/**
 * 출석 ViewModel
 * BLE 출석 시작/체크 및 출석 현황 관리
 */
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AttendanceViewModel"
        private const val SCAN_TIMEOUT_MS = 30_000L // 30초 스캔 타임아웃
        private const val POLLING_INTERVAL_MS = 3_000L // 3초마다 출석 현황 갱신
    }

    // BLE 컴포넌트
    private val bleAdvertiser = BleAdvertiser(application)
    private val bleScanner = BleScanner(application)

    // 출석 상태
    private val _attendanceState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    val attendanceState: StateFlow<AttendanceUiState> = _attendanceState.asStateFlow()

    // 세션 출석 현황 (스터디장용)
    private val _sessionAttendanceState = MutableStateFlow<SessionAttendanceUiState>(SessionAttendanceUiState.Loading)
    val sessionAttendanceState: StateFlow<SessionAttendanceUiState> = _sessionAttendanceState.asStateFlow()

    // 출석 통계
    private val _statsState = MutableStateFlow<AttendanceStatsUiState>(AttendanceStatsUiState.Loading)
    val statsState: StateFlow<AttendanceStatsUiState> = _statsState.asStateFlow()

    // 출석 히스토리
    private val _historyState = MutableStateFlow<AttendanceHistoryUiState>(AttendanceHistoryUiState.Loading)
    val historyState: StateFlow<AttendanceHistoryUiState> = _historyState.asStateFlow()

    // BLE 상태 (UI 연동용)
    val isAdvertising = bleAdvertiser.isAdvertising
    val isScanning = bleScanner.isScanning
    val foundBeacon = bleScanner.foundBeacon

    // 현재 스터디/세션 정보
    private var currentStudyId: Long? = null
    private var currentSessionId: Long? = null

    // 폴링 Job
    private var pollingJob: Job? = null
    private var scanTimeoutJob: Job? = null

    // ==================== 스터디장용 (BLE 광고) ====================

    /**
     * BLE 출석 시작 (스터디장)
     * BLE Beacon을 광고하고 서버에 출석 세션 시작을 알립니다.
     */
    fun startBleAttendance(studyId: Long, sessionId: Long) {
        currentStudyId = studyId
        currentSessionId = sessionId

        viewModelScope.launch {
            try {
                // 1. 서버에 BLE 출석 시작 요청
                val request = BleAttendanceStartRequest(
                    uuid = BleAdvertiser.SQUIZ_SERVICE_UUID,
                    major = studyId.toInt(),
                    minor = sessionId.toInt()
                )
                val response = RetrofitClient.attendanceApi.startBleAttendance(studyId, sessionId, request)

                if (response.isSuccessful) {
                    // 2. BLE 광고 시작
                    val started = bleAdvertiser.startAdvertising(studyId, sessionId)
                    if (started) {
                        _attendanceState.value = AttendanceUiState.Advertising
                        Log.d(TAG, "BLE 출석 시작 성공: studyId=$studyId, sessionId=$sessionId")

                        // 3. 출석 현황 폴링 시작
                        startPollingSessionAttendance(studyId, sessionId)
                    } else {
                        _attendanceState.value = AttendanceUiState.Error("BLE 광고를 시작할 수 없습니다. Bluetooth를 확인해주세요.")
                    }
                } else {
                    _attendanceState.value = AttendanceUiState.Error("서버 연결 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "BLE 출석 시작 실패", e)
                _attendanceState.value = AttendanceUiState.Error(e.message ?: "출석 시작 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * BLE 출석 중지 (스터디장)
     */
    fun stopBleAttendance() {
        bleAdvertiser.stopAdvertising()
        stopPolling()
        _attendanceState.value = AttendanceUiState.Idle
        Log.d(TAG, "BLE 출석 중지")
    }

    /**
     * 세션 출석 현황 폴링 시작
     */
    private fun startPollingSessionAttendance(studyId: Long, sessionId: Long) {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                loadSessionAttendance(studyId, sessionId)
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    /**
     * 세션 출석 현황 로드
     */
    private suspend fun loadSessionAttendance(studyId: Long, sessionId: Long) {
        try {
            val response = RetrofitClient.attendanceApi.getSessionAttendance(studyId, sessionId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { info ->
                    _sessionAttendanceState.value = SessionAttendanceUiState.Success(info)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "세션 출석 현황 로드 실패", e)
        }
    }

    // ==================== 멤버용 (BLE 스캔) ====================

    /**
     * BLE 출석 체크 시작 (멤버)
     * 스터디장의 BLE Beacon을 스캔하여 출석합니다.
     */
    fun startBleCheck(studyId: Long, sessionId: Long) {
        currentStudyId = studyId
        currentSessionId = sessionId

        _attendanceState.value = AttendanceUiState.Scanning

        // 스캔 타임아웃 설정
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            if (_attendanceState.value == AttendanceUiState.Scanning) {
                stopBleCheck()
                _attendanceState.value = AttendanceUiState.Error("스터디장을 찾을 수 없습니다. 가까이 이동해주세요.")
            }
        }

        // BLE 스캔 시작
        val started = bleScanner.startScanning(studyId, sessionId) { beacon ->
            // Beacon 발견 시 출석 체크
            viewModelScope.launch {
                checkAttendance(studyId, sessionId, beacon)
            }
        }

        if (!started) {
            _attendanceState.value = AttendanceUiState.Error("BLE 스캔을 시작할 수 없습니다. Bluetooth를 확인해주세요.")
            scanTimeoutJob?.cancel()
        }
    }

    /**
     * BLE 출석 체크 중지 (멤버)
     */
    fun stopBleCheck() {
        bleScanner.stopScanning()
        scanTimeoutJob?.cancel()
        if (_attendanceState.value == AttendanceUiState.Scanning) {
            _attendanceState.value = AttendanceUiState.Idle
        }
    }

    /**
     * 서버에 출석 체크 요청
     */
    private suspend fun checkAttendance(studyId: Long, sessionId: Long, beacon: BleScanner.SquizBeacon) {
        try {
            // 스캔 중지
            bleScanner.stopScanning()
            scanTimeoutJob?.cancel()

            val request = BleAttendanceCheckRequest(
                uuid = beacon.uuid,
                major = beacon.major,
                minor = beacon.minor
            )

            val response = RetrofitClient.attendanceApi.checkBleAttendance(studyId, sessionId, request)

            if (response.isSuccessful && response.body()?.success == true) {
                val attendanceResponse = response.body()?.data
                val status = attendanceResponse?.status ?: "PRESENT"

                val message = when (status) {
                    "PRESENT" -> "출석 완료!"
                    "LATE" -> "지각 처리되었습니다."
                    else -> "출석이 확인되었습니다."
                }

                _attendanceState.value = AttendanceUiState.Success(message)
                Log.d(TAG, "출석 체크 성공: $status")
            } else {
                _attendanceState.value = AttendanceUiState.Error("출석 체크 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "출석 체크 실패", e)
            _attendanceState.value = AttendanceUiState.Error(e.message ?: "출석 체크 중 오류가 발생했습니다.")
        }
    }

    // ==================== 공통 ====================

    /**
     * 출석 통계 로드
     * 404 에러 시 기본값 사용 (백엔드 미구현 대응)
     */
    fun loadAttendanceStats(studyId: Long) {
        viewModelScope.launch {
            _statsState.value = AttendanceStatsUiState.Loading
            try {
                val response = RetrofitClient.attendanceApi.getAttendanceStats(studyId)
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { stats ->
                        _statsState.value = AttendanceStatsUiState.Success(stats)
                    } ?: run {
                        _statsState.value = AttendanceStatsUiState.Success(AttendanceStatsDTO())
                    }
                } else if (response.code() == 404) {
                    // 백엔드 API 미구현 시 기본값 사용
                    Log.w(TAG, "출석 통계 API 없음 (404), 기본값 사용")
                    _statsState.value = AttendanceStatsUiState.Success(AttendanceStatsDTO())
                } else {
                    _statsState.value = AttendanceStatsUiState.Success(AttendanceStatsDTO())
                }
            } catch (e: Exception) {
                Log.e(TAG, "출석 통계 로드 실패", e)
                // 에러 발생 시 기본값 사용
                _statsState.value = AttendanceStatsUiState.Success(AttendanceStatsDTO())
            }
        }
    }

    /**
     * 출석 히스토리 로드
     * 404 에러 시 빈 목록 사용 (백엔드 미구현 대응)
     */
    fun loadAttendanceHistory(studyId: Long) {
        viewModelScope.launch {
            _historyState.value = AttendanceHistoryUiState.Loading
            try {
                val response = RetrofitClient.attendanceApi.getAttendanceStatus(studyId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val history = response.body()?.data ?: emptyList()
                    _historyState.value = AttendanceHistoryUiState.Success(history)
                } else if (response.code() == 404) {
                    // 백엔드 API 미구현 시 빈 목록 사용
                    Log.w(TAG, "출석 히스토리 API 없음 (404), 빈 목록 사용")
                    _historyState.value = AttendanceHistoryUiState.Success(emptyList())
                } else {
                    _historyState.value = AttendanceHistoryUiState.Success(emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "출석 히스토리 로드 실패", e)
                // 에러 발생 시 빈 목록 사용
                _historyState.value = AttendanceHistoryUiState.Success(emptyList())
            }
        }
    }

    /**
     * 상태 초기화
     */
    fun resetState() {
        _attendanceState.value = AttendanceUiState.Idle
        bleScanner.clearFoundBeacon()
    }

    /**
     * 폴링 중지
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * BLE 지원 여부 확인
     */
    fun isBleSupported(): Boolean {
        return bleAdvertiser.isSupported() || bleScanner.isSupported()
    }

    /**
     * Bluetooth 활성화 여부 확인
     */
    fun isBluetoothEnabled(): Boolean {
        return bleAdvertiser.isBluetoothEnabled()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
        scanTimeoutJob?.cancel()
        bleAdvertiser.stopAdvertising()
        bleScanner.stopScanning()
    }
}
