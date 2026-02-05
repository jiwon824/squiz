package com.ssafy.squiz.ui.screens.meeting

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.service.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

// UI 상태 클래스들
sealed class MeetingsUiState {
    object Loading : MeetingsUiState()
    data class Success(
        val meetings: List<MeetingDTO>,
        val totalCount: Long = 0,
        val hasMore: Boolean = false
    ) : MeetingsUiState()
    data class Error(val message: String) : MeetingsUiState()
}

sealed class MeetingDetailUiState {
    object Loading : MeetingDetailUiState()
    data class Success(val meeting: MeetingDetailDTO) : MeetingDetailUiState()
    data class Error(val message: String) : MeetingDetailUiState()
}

sealed class UploadUiState {
    object Idle : UploadUiState()
    data class Uploading(val progress: Int = 0) : UploadUiState()
    data class Success(val message: String) : UploadUiState()
    data class Error(val message: String) : UploadUiState()
}

class MeetingViewModel : ViewModel() {

    companion object {
        private const val TAG = "MeetingViewModel"
    }

    // 회의 목록 상태
    private val _meetingsState = MutableStateFlow<MeetingsUiState>(MeetingsUiState.Loading)
    val meetingsState: StateFlow<MeetingsUiState> = _meetingsState.asStateFlow()

    // 회의 상세 상태
    private val _meetingDetailState = MutableStateFlow<MeetingDetailUiState>(MeetingDetailUiState.Loading)
    val meetingDetailState: StateFlow<MeetingDetailUiState> = _meetingDetailState.asStateFlow()

    // 업로드 상태
    private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    // 현재 회의 ID (녹음 시작 시 설정)
    private val _currentMeetingId = MutableStateFlow<Long?>(null)
    val currentMeetingId: StateFlow<Long?> = _currentMeetingId.asStateFlow()

    // 현재 스터디 ID (녹음 시작 시 설정)
    private val _currentStudyId = MutableStateFlow<Long?>(null)
    val currentStudyId: StateFlow<Long?> = _currentStudyId.asStateFlow()

    // 현재 세션 ID (녹음 시작 시 설정, 오프라인 녹음이 어느 세션에 해당하는지)
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    // 녹음 상태 (RecordingService에서 가져옴)
    val recordingState = RecordingService.recordingState

    // 페이지네이션
    private var currentPage = 0
    private var isLastPage = false

    /**
     * 회의 목록 로드
     * 404 에러 시 빈 목록 사용 (백엔드 미구현 대응)
     */
    fun loadMeetings(studyId: Long, refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            isLastPage = false
        }
        if (isLastPage && !refresh) return

        viewModelScope.launch {
            try {
                if (refresh || _meetingsState.value is MeetingsUiState.Loading) {
                    _meetingsState.value = MeetingsUiState.Loading
                }

                val response = RetrofitClient.meetingApi.getMeetings(
                    studyId = studyId,
                    page = currentPage,
                    size = 20
                )

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val pageResponse = apiResponse?.data
                    val meetings = pageResponse?.content ?: emptyList()

                    val currentList = if (refresh) emptyList() else {
                        (_meetingsState.value as? MeetingsUiState.Success)?.meetings ?: emptyList()
                    }

                    _meetingsState.value = MeetingsUiState.Success(
                        meetings = currentList + meetings,
                        totalCount = pageResponse?.totalElements ?: 0,
                        hasMore = !(pageResponse?.last ?: true)
                    )

                    isLastPage = pageResponse?.last ?: true
                    currentPage++
                } else if (response.code() == 404) {
                    // 백엔드 API 미구현 시 빈 목록 사용
                    Log.w(TAG, "회의 목록 API 없음 (404), 빈 목록 사용")
                    _meetingsState.value = MeetingsUiState.Success(
                        meetings = emptyList(),
                        totalCount = 0,
                        hasMore = false
                    )
                    isLastPage = true
                } else {
                    // 기타 에러도 빈 목록으로 처리
                    _meetingsState.value = MeetingsUiState.Success(
                        meetings = emptyList(),
                        totalCount = 0,
                        hasMore = false
                    )
                    isLastPage = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "회의 목록 로드 실패", e)
                // 에러 발생 시 빈 목록으로 처리
                _meetingsState.value = MeetingsUiState.Success(
                    meetings = emptyList(),
                    totalCount = 0,
                    hasMore = false
                )
                isLastPage = true
            }
        }
    }

    /**
     * 더 불러오기
     */
    fun loadMore(studyId: Long) {
        if (!isLastPage) {
            loadMeetings(studyId, refresh = false)
        }
    }

    /**
     * 회의 상세 로드
     */
    fun loadMeetingDetail(studyId: Long, meetingId: Long) {
        viewModelScope.launch {
            _meetingDetailState.value = MeetingDetailUiState.Loading
            try {
                val response = RetrofitClient.meetingApi.getMeetingDetail(studyId, meetingId)

                if (response.isSuccessful) {
                    val detail = response.body()?.data
                    if (detail != null) {
                        _meetingDetailState.value = MeetingDetailUiState.Success(detail)
                    } else {
                        _meetingDetailState.value = MeetingDetailUiState.Error("회의 정보를 찾을 수 없습니다.")
                    }
                } else {
                    _meetingDetailState.value = MeetingDetailUiState.Error("회의 상세를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "회의 상세 로드 실패", e)
                _meetingDetailState.value = MeetingDetailUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 녹음 시작 (오프라인 녹음 - 회의 생성 없이 로컬 녹음만 진행)
     * 녹음 종료 시 서버에 업로드하면 회의가 자동 생성됨
     * @param sessionId 연결할 세션 ID (세션 상세 화면에서 녹음 시작 시 자동 설정)
     */
    fun startRecording(context: Context, studyId: Long, sessionId: Long? = null, title: String? = null) {
        viewModelScope.launch {
            _currentStudyId.value = studyId
            _currentSessionId.value = sessionId  // 세션 ID 저장 (업로드 시 사용)

            // 로컬 녹음 서비스 시작 (서버 회의 생성 없이)
            try {
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                    putExtra(RecordingService.EXTRA_STUDY_ID, studyId)
                    putExtra(RecordingService.EXTRA_SESSION_ID, sessionId ?: -1L)
                    putExtra(RecordingService.EXTRA_MEETING_ID, -1L) // 오프라인 녹음은 meetingId 없음
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "오프라인 녹음 시작: studyId=$studyId, sessionId=$sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "녹음 서비스 시작 실패", e)
                _currentStudyId.value = null
                _currentSessionId.value = null
            }
        }
    }

    /**
     * 녹음 중지 및 업로드 (오프라인 녹음용 - 새 엔드포인트 사용)
     * 세션 ID가 설정된 경우 해당 세션과 연결된 미팅으로 생성됨
     */
    fun stopRecordingAndUpload(context: Context) {
        viewModelScope.launch {
            try {
                val studyId = _currentStudyId.value
                val sessionId = _currentSessionId.value  // 저장된 세션 ID 가져오기

                // 1. 로컬 녹음 중지
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
                context.startService(intent)

                // 녹음 파일 경로 가져오기
                val audioFilePath = RecordingService.currentAudioFilePath

                if (studyId != null && audioFilePath != null) {
                    // 2. 오프라인 녹음 업로드 (회의 자동 생성 + AI 처리 트리거)
                    uploadOfflineRecording(studyId, sessionId, audioFilePath)
                }

                _currentMeetingId.value = null
                _currentStudyId.value = null
                _currentSessionId.value = null  // 세션 ID 초기화

            } catch (e: Exception) {
                Log.e(TAG, "녹음 중지 실패", e)
                _uploadState.value = UploadUiState.Error(e.message ?: "녹음 중지 실패")
            }
        }
    }

    /**
     * 오프라인 녹음 업로드 (회의 자동 생성)
     * @param sessionId 연결할 세션 ID (세션에서 녹음 시작한 경우 해당 세션과 연결)
     */
    private suspend fun uploadOfflineRecording(studyId: Long, sessionId: Long?, filePath: String) {
        try {
            _uploadState.value = UploadUiState.Uploading(0)

            val file = File(filePath)
            if (!file.exists()) {
                _uploadState.value = UploadUiState.Error("녹음 파일을 찾을 수 없습니다.")
                return
            }

            // Content-Length 불일치 방지를 위해 파일을 ByteArray로 완전히 읽은 후 전송
            val bytes = file.readBytes()
            val requestBody = bytes.toRequestBody("audio/mp4".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("audio", file.name, requestBody)

            val response = RetrofitClient.meetingApi.uploadOfflineRecording(
                studyId = studyId,
                audio = multipartBody,
                sessionId = sessionId,  // 세션 ID 전달
                title = null
            )

            if (response.isSuccessful) {
                val meetingId = response.body()?.data?.id
                _uploadState.value = UploadUiState.Success("업로드 완료! 미팅 리포트가 곧 생성됩니다.\n웹에서 확인하세요.")
                Log.d(TAG, "오프라인 녹음 업로드 성공: studyId=$studyId, sessionId=$sessionId, meetingId=$meetingId")

                // 업로드 후 파일 삭제 (선택)
                // file.delete()
            } else {
                _uploadState.value = UploadUiState.Error("업로드 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "오프라인 녹음 업로드 실패", e)
            _uploadState.value = UploadUiState.Error(e.message ?: "업로드 중 오류 발생")
        }
    }

    /**
     * 업로드 상태 초기화
     */
    fun resetUploadState() {
        _uploadState.value = UploadUiState.Idle
    }

    /**
     * 회의 삭제
     */
    fun deleteMeeting(studyId: Long, meetingId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.meetingApi.deleteMeeting(studyId, meetingId)
                if (response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(Exception("삭제 실패: ${response.code()}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
