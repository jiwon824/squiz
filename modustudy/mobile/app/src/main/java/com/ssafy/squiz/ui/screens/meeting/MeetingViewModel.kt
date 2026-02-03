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
import okhttp3.RequestBody.Companion.asRequestBody
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
     * 녹음 시작 (서버에 회의 생성 후 로컬 녹음 시작)
     * 서버 API 실패 시에도 로컬 녹음은 진행
     */
    fun startRecording(context: Context, studyId: Long, sessionId: Long? = null, title: String? = null) {
        viewModelScope.launch {
            var meetingId: Long? = null

            // 1. 서버에 녹음 시작 요청 시도 (실패해도 로컬 녹음 진행)
            try {
                val request = RecordingStartRequest(
                    title = title,
                    sessionId = sessionId,
                    type = "OFFLINE"
                )
                val response = RetrofitClient.meetingApi.startRecording(studyId, request)

                if (response.isSuccessful) {
                    meetingId = response.body()?.data?.id
                    Log.d(TAG, "서버 회의 생성 성공: meetingId=$meetingId")
                } else {
                    Log.w(TAG, "서버 회의 생성 실패 (${response.code()}), 로컬 녹음만 진행")
                }
            } catch (e: Exception) {
                Log.w(TAG, "서버 연결 실패, 로컬 녹음만 진행", e)
            }

            // 서버 meetingId가 없으면 임시 ID 생성
            if (meetingId == null) {
                meetingId = System.currentTimeMillis()
                Log.d(TAG, "임시 meetingId 생성: $meetingId")
            }

            _currentMeetingId.value = meetingId
            _currentStudyId.value = studyId

            // 2. 로컬 녹음 서비스 시작
            try {
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                    putExtra(RecordingService.EXTRA_STUDY_ID, studyId)
                    putExtra(RecordingService.EXTRA_SESSION_ID, sessionId ?: -1L)
                    putExtra(RecordingService.EXTRA_MEETING_ID, meetingId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "녹음 서비스 시작 요청: studyId=$studyId, meetingId=$meetingId")
            } catch (e: Exception) {
                Log.e(TAG, "녹음 서비스 시작 실패", e)
                _currentMeetingId.value = null
                _currentStudyId.value = null
            }
        }
    }

    /**
     * 녹음 중지 및 업로드
     */
    fun stopRecordingAndUpload(context: Context) {
        viewModelScope.launch {
            try {
                val meetingId = _currentMeetingId.value
                val studyId = _currentStudyId.value

                // 1. 로컬 녹음 중지
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
                context.startService(intent)

                // 녹음 파일 경로 가져오기
                val audioFilePath = RecordingService.currentAudioFilePath

                if (meetingId != null && studyId != null && audioFilePath != null) {
                    // 2. 오디오 파일 업로드 먼저
                    uploadAudioFile(studyId, meetingId, audioFilePath)

                    // 3. 서버에 녹음 종료 알림 (AI 처리 트리거)
                    try {
                        val endResponse = RetrofitClient.meetingApi.endRecording(studyId, meetingId)
                        if (endResponse.isSuccessful) {
                            Log.d(TAG, "회의 종료 성공, AI 처리 시작됨")
                        } else {
                            Log.w(TAG, "회의 종료 API 실패: ${endResponse.code()}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "회의 종료 API 호출 실패", e)
                    }
                }

                _currentMeetingId.value = null
                _currentStudyId.value = null

            } catch (e: Exception) {
                Log.e(TAG, "녹음 중지 실패", e)
                _uploadState.value = UploadUiState.Error(e.message ?: "녹음 중지 실패")
            }
        }
    }

    /**
     * 오디오 파일 업로드
     */
    private suspend fun uploadAudioFile(studyId: Long, meetingId: Long, filePath: String) {
        try {
            _uploadState.value = UploadUiState.Uploading(0)

            val file = File(filePath)
            if (!file.exists()) {
                _uploadState.value = UploadUiState.Error("녹음 파일을 찾을 수 없습니다.")
                return
            }

            val requestBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("audio", file.name, requestBody)

            val response = RetrofitClient.meetingApi.uploadAudio(
                studyId = studyId,
                meetingId = meetingId,
                audio = multipartBody,
                trackType = "MIXED"
            )

            if (response.isSuccessful) {
                _uploadState.value = UploadUiState.Success("업로드 완료! AI 요약이 곧 생성됩니다.")
                Log.d(TAG, "오디오 업로드 성공: studyId=$studyId, meetingId=$meetingId")

                // 업로드 후 파일 삭제 (선택)
                // file.delete()
            } else {
                _uploadState.value = UploadUiState.Error("업로드 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "오디오 업로드 실패", e)
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
