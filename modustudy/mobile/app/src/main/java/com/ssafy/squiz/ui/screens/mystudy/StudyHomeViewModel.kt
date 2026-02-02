package com.ssafy.squiz.ui.screens.mystudy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.StudyDetailDTO
import com.ssafy.squiz.data.remote.model.StudySessionDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 스터디 홈 ViewModel
 * 스터디 정보와 다음 세션 정보 관리
 */
class StudyHomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "StudyHomeViewModel"
    }

    // 스터디 상세 정보
    private val _studyDetail = MutableStateFlow<StudyDetailDTO?>(null)
    val studyDetail: StateFlow<StudyDetailDTO?> = _studyDetail.asStateFlow()

    // 다음 세션 정보
    private val _nextSession = MutableStateFlow<StudySessionDTO?>(null)
    val nextSession: StateFlow<StudySessionDTO?> = _nextSession.asStateFlow()

    // 스터디장 여부
    private val _isLeader = MutableStateFlow(false)
    val isLeader: StateFlow<Boolean> = _isLeader.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 에러 메시지
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 스터디 홈 데이터 로드
     */
    fun loadStudyHome(studyId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 스터디 상세 정보 조회
                loadStudyDetail(studyId)

                // 다음 세션 정보 조회
                loadNextSession(studyId)

            } catch (e: Exception) {
                Log.e(TAG, "스터디 홈 데이터 로드 실패", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 스터디 상세 정보 로드
     */
    private suspend fun loadStudyDetail(studyId: Long) {
        try {
            val response = RetrofitClient.studyApi.getStudyDetail(studyId)
            if (response.isSuccessful) {
                val detail = response.body()
                _studyDetail.value = detail
                _isLeader.value = detail?.isLeader ?: false
                Log.d(TAG, "스터디 상세 로드 성공: ${detail?.name}, isLeader=${detail?.isLeader}")
            } else {
                Log.e(TAG, "스터디 상세 로드 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "스터디 상세 로드 실패", e)
        }
    }

    /**
     * 다음 세션 정보 로드
     * GET /api/v1/studies/{studyId}/sessions/next 사용
     */
    private suspend fun loadNextSession(studyId: Long) {
        try {
            val response = RetrofitClient.studyApi.getNextSession(studyId)
            if (response.isSuccessful) {
                val session = response.body()
                _nextSession.value = session
                Log.d(TAG, "다음 세션 로드 성공: id=${session?.id}, scheduledAt=${session?.scheduledAt}")
            } else if (response.code() == 404) {
                // 다음 세션이 없는 경우 - 세션 목록에서 첫 번째 SCHEDULED 세션 찾기
                Log.w(TAG, "다음 세션 없음 (404), 세션 목록에서 검색")
                loadFirstScheduledSession(studyId)
            } else {
                Log.e(TAG, "다음 세션 로드 실패: ${response.code()}")
                // 실패해도 세션 목록에서 시도
                loadFirstScheduledSession(studyId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "다음 세션 로드 실패", e)
            // 예외 발생 시에도 세션 목록에서 시도
            loadFirstScheduledSession(studyId)
        }
    }

    /**
     * 세션 목록에서 첫 번째 예정된 세션 찾기
     */
    private suspend fun loadFirstScheduledSession(studyId: Long) {
        try {
            val response = RetrofitClient.studyApi.getStudySessions(studyId)
            if (response.isSuccessful) {
                val sessions = response.body() ?: emptyList()
                Log.d(TAG, "세션 목록 로드 성공: ${sessions.size}개")

                // SCHEDULED 상태인 세션 중 가장 빠른 것 선택
                val scheduledSession = sessions
                    .filter { it.status == "SCHEDULED" || it.status == null }
                    .minByOrNull { it.scheduledAt ?: "" }

                if (scheduledSession != null) {
                    _nextSession.value = scheduledSession
                    Log.d(TAG, "예정된 세션 찾음: id=${scheduledSession.id}")
                } else if (sessions.isNotEmpty()) {
                    // SCHEDULED 세션이 없으면 첫 번째 세션 사용
                    _nextSession.value = sessions.first()
                    Log.d(TAG, "첫 번째 세션 사용: id=${sessions.first().id}")
                } else {
                    Log.w(TAG, "세션이 없음")
                }
            } else {
                Log.e(TAG, "세션 목록 로드 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "세션 목록 로드 실패", e)
        }
    }

    /**
     * 에러 초기화
     */
    fun clearError() {
        _error.value = null
    }
}
