package com.ssafy.squiz.ui.screens.meeting

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.local.recording.LocalRecording
import com.ssafy.squiz.data.local.recording.LocalRecordingRepository
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.service.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        private const val MAX_DAILY_RECORDING_SECONDS = 2 * 60 * 60  // 2시간
    }

    // 로컬 녹음 Repository
    private var localRecordingRepository: LocalRecordingRepository? = null

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

    // 로컬 녹음 목록 (미업로드)
    private val _localRecordings = MutableStateFlow<List<LocalRecording>>(emptyList())
    val localRecordings: StateFlow<List<LocalRecording>> = _localRecordings.asStateFlow()

    // 선택된 녹음들의 총 시간 (초)
    private val _selectedTotalSeconds = MutableStateFlow(0)
    val selectedTotalSeconds: StateFlow<Int> = _selectedTotalSeconds.asStateFlow()

    // 선택된 녹음 개수
    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    // 오늘 남은 녹음 시간 (초)
    private val _remainingSeconds = MutableStateFlow(MAX_DAILY_RECORDING_SECONDS)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // 녹음 완료 후 옵션 다이얼로그 표시 여부
    private val _showRecordingCompleteDialog = MutableStateFlow(false)
    val showRecordingCompleteDialog: StateFlow<Boolean> = _showRecordingCompleteDialog.asStateFlow()

    // 세션 선택 다이얼로그 표시 여부
    private val _showSessionSelectDialog = MutableStateFlow(false)
    val showSessionSelectDialog: StateFlow<Boolean> = _showSessionSelectDialog.asStateFlow()

    // 스터디 세션 목록
    private val _sessions = MutableStateFlow<List<StudySessionDTO>>(emptyList())
    val sessions: StateFlow<List<StudySessionDTO>> = _sessions.asStateFlow()

    // 업로드 대기 중인 파일 경로 (레거시 - 단일 파일 업로드 시 사용)
    private val _pendingUploadFilePath = MutableStateFlow<String?>(null)

    // 페이지네이션
    private var currentPage = 0
    private var isLastPage = false

    /**
     * Repository 초기화 (Context 필요)
     */
    fun initRepository(context: Context) {
        if (localRecordingRepository == null) {
            localRecordingRepository = LocalRecordingRepository(context)
        }
    }

    /**
     * 로컬 녹음 목록 로드
     */
    fun loadLocalRecordings(studyId: Long) {
        viewModelScope.launch {
            localRecordingRepository?.getUnuploadedRecordings(studyId)?.collectLatest { recordings ->
                _localRecordings.value = recordings
                updateSelectedStats(recordings)
            }
        }
    }

    /**
     * 선택 통계 업데이트
     */
    private fun updateSelectedStats(recordings: List<LocalRecording>) {
        val selected = recordings.filter { it.selected }
        _selectedCount.value = selected.size
        _selectedTotalSeconds.value = selected.sumOf { it.durationSeconds }
    }

    /**
     * 오늘 남은 녹음 시간 로드
     */
    fun loadRemainingTime(studyId: Long) {
        viewModelScope.launch {
            val remaining = localRecordingRepository?.getTodayRemainingSeconds(studyId)
                ?: MAX_DAILY_RECORDING_SECONDS
            _remainingSeconds.value = remaining
        }
    }

    /**
     * 녹음 선택 토글 (순서 부여/해제)
     */
    fun toggleRecordingSelection(recordingId: String, studyId: Long) {
        viewModelScope.launch {
            localRecordingRepository?.toggleSelection(recordingId, studyId)
        }
    }

    /**
     * 모든 선택 해제
     */
    fun clearAllSelections(studyId: Long) {
        viewModelScope.launch {
            localRecordingRepository?.clearAllSelections(studyId)
        }
    }

    /**
     * 녹음 삭제
     */
    fun deleteRecording(recording: LocalRecording) {
        viewModelScope.launch {
            localRecordingRepository?.deleteRecording(recording)
        }
    }

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
     * 녹음 중지 및 로컬 저장
     * 녹음 완료 후 로컬에 저장하고 옵션 다이얼로그 표시
     */
    fun stopRecordingAndSave(context: Context) {
        viewModelScope.launch {
            try {
                val studyId = _currentStudyId.value
                val elapsedSeconds = recordingState.value.elapsedSeconds.toInt()

                Log.d(TAG, "녹음 중지 시작: studyId=$studyId, duration=${elapsedSeconds}초")

                // 녹음 파일 경로 가져오기 (중지 전에)
                val audioFilePath = RecordingService.currentAudioFilePath

                // 로컬 녹음 중지
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
                context.startService(intent)

                Log.d(TAG, "녹음 파일 경로: $audioFilePath")

                if (studyId != null && audioFilePath != null) {
                    // 파일 정보 가져오기
                    val file = File(audioFilePath)
                    val fileSize = file.length()
                    val fileName = file.name

                    // 오디오 길이 가져오기 (MediaMetadataRetriever 사용)
                    val durationSeconds = getDurationFromFile(audioFilePath) ?: elapsedSeconds

                    // 로컬 DB에 저장
                    localRecordingRepository?.saveRecording(
                        filePath = audioFilePath,
                        fileName = fileName,
                        durationSeconds = durationSeconds,
                        fileSizeBytes = fileSize,
                        studyId = studyId,
                        sessionId = null  // 아직 세션 미연결
                    )

                    Log.d(TAG, "로컬 녹음 저장 완료: $fileName, ${durationSeconds}초")

                    // 녹음 완료 다이얼로그 표시
                    _showRecordingCompleteDialog.value = true

                    // 로컬 녹음 목록 새로고침
                    loadLocalRecordings(studyId)
                    loadRemainingTime(studyId)
                } else {
                    Log.w(TAG, "studyId 또는 audioFilePath가 null: studyId=$studyId, audioFilePath=$audioFilePath")
                }

                // 상태 초기화
                _currentMeetingId.value = null
                _currentStudyId.value = null
                _currentSessionId.value = null

            } catch (e: Exception) {
                Log.e(TAG, "녹음 중지 실패", e)
                _uploadState.value = UploadUiState.Error(e.message ?: "녹음 중지 실패")
            }
        }
    }

    /**
     * 파일에서 오디오 길이 추출 (초)
     */
    private fun getDurationFromFile(filePath: String): Int? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            durationMs?.let { (it / 1000).toInt() }
        } catch (e: Exception) {
            Log.e(TAG, "오디오 길이 추출 실패", e)
            null
        }
    }

    /**
     * 녹음 완료 다이얼로그 닫기
     */
    fun dismissRecordingCompleteDialog() {
        _showRecordingCompleteDialog.value = false
    }

    /**
     * 세션 선택 다이얼로그 표시 (업로드용)
     */
    fun showSessionSelectForUpload() {
        _showSessionSelectDialog.value = true
    }

    /**
     * 선택된 녹음들 세션에 업로드 (여러 파일을 한번에 병합하여 업로드)
     */
    fun uploadSelectedRecordings(studyId: Long, sessionId: Long) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadUiState.Uploading(0)

                // selectedOrder 순서대로 정렬된 녹음 목록
                val selectedRecordings = localRecordingRepository?.getSelectedRecordings(studyId) ?: emptyList()
                if (selectedRecordings.isEmpty()) {
                    _uploadState.value = UploadUiState.Error("선택된 녹음이 없습니다.")
                    return@launch
                }

                Log.d(TAG, "선택된 녹음 ${selectedRecordings.size}개 업로드 시작")
                _uploadState.value = UploadUiState.Uploading(30)

                // 여러 파일을 MultipartBody.Part 리스트로 변환
                val audioParts = mutableListOf<MultipartBody.Part>()
                for (recording in selectedRecordings) {
                    val file = File(recording.filePath)
                    if (!file.exists()) {
                        Log.e(TAG, "파일 없음: ${recording.filePath}")
                        continue
                    }
                    val bytes = file.readBytes()
                    val requestBody = bytes.toRequestBody("audio/mp4".toMediaTypeOrNull())
                    // 모든 파트는 'audio' 키로 전송 (백엔드에서 List로 받음)
                    val part = MultipartBody.Part.createFormData("audio", file.name, requestBody)
                    audioParts.add(part)
                }

                if (audioParts.isEmpty()) {
                    _uploadState.value = UploadUiState.Error("유효한 녹음 파일이 없습니다.")
                    return@launch
                }

                _uploadState.value = UploadUiState.Uploading(60)

                // 한 번의 API 호출로 모든 파일 업로드 (백엔드에서 병합)
                val response = RetrofitClient.meetingApi.uploadOfflineRecordings(
                    studyId = studyId,
                    audioFiles = audioParts,
                    sessionId = sessionId,
                    title = null
                )

                _uploadState.value = UploadUiState.Uploading(90)

                if (response.isSuccessful) {
                    Log.d(TAG, "녹음 ${audioParts.size}개 병합 업로드 성공")
                    localRecordingRepository?.markSelectedAsUploaded(studyId, sessionId)
                    _uploadState.value = UploadUiState.Success("${audioParts.size}개 녹음이 병합되어 업로드 완료!")
                } else {
                    Log.e(TAG, "녹음 업로드 실패: ${response.code()}")
                    _uploadState.value = UploadUiState.Error("업로드 실패 (${response.code()})")
                }

                // 목록 새로고침
                loadLocalRecordings(studyId)
                loadMeetings(studyId, refresh = true)

            } catch (e: Exception) {
                Log.e(TAG, "녹음 업로드 실패", e)
                _uploadState.value = UploadUiState.Error(e.message ?: "업로드 중 오류 발생")
            }
        }
    }

    /**
     * 스터디 세션 목록 로드 (이미 업로드된 세션은 제외)
     */
    fun loadSessions(studyId: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "세션 목록 로드 시작: studyId=$studyId")
                val response = RetrofitClient.studyApi.getStudySessions(studyId)
                if (response.isSuccessful) {
                    // 오프라인 세션만 필터링 (isOnline == false 또는 null)
                    val allSessions = response.body() ?: emptyList()
                    Log.d(TAG, "전체 세션 ${allSessions.size}개 로드됨")
                    val offlineSessions = allSessions.filter { it.isOnline != true }
                    Log.d(TAG, "오프라인 세션: ${offlineSessions.size}개")

                    // 이미 미팅이 업로드된 세션 제외
                    if (offlineSessions.isNotEmpty()) {
                        val sessionIds = offlineSessions.map { it.id }
                        try {
                            val uploadedResponse = RetrofitClient.meetingApi.getUploadedSessionIds(studyId, sessionIds)
                            if (uploadedResponse.isSuccessful) {
                                val uploadedIds = uploadedResponse.body()?.data ?: emptyList()
                                Log.d(TAG, "이미 업로드된 세션: ${uploadedIds.size}개 - $uploadedIds")
                                // 업로드된 세션 제외
                                val availableSessions = offlineSessions.filter { it.id !in uploadedIds }
                                _sessions.value = availableSessions
                                Log.d(TAG, "업로드 가능한 세션: ${availableSessions.size}개")
                            } else {
                                Log.w(TAG, "업로드된 세션 확인 실패: ${uploadedResponse.code()}, 전체 세션 표시")
                                _sessions.value = offlineSessions
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "업로드된 세션 확인 오류, 전체 세션 표시: ${e.message}")
                            _sessions.value = offlineSessions
                        }
                    } else {
                        _sessions.value = offlineSessions
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "세션 목록 로드 실패: ${response.code()}, error=$errorBody")
                    _sessions.value = emptyList()
                    // 에러가 발생해도 다이얼로그는 표시 (빈 목록으로)
                }
            } catch (e: Exception) {
                Log.e(TAG, "세션 목록 로드 실패", e)
                _sessions.value = emptyList()
            }
        }
    }

    /**
     * 세션 선택 후 업로드
     */
    fun uploadWithSelectedSession(sessionId: Long?) {
        viewModelScope.launch {
            val studyId = _currentStudyId.value
            val filePath = _pendingUploadFilePath.value

            if (studyId != null && filePath != null) {
                uploadOfflineRecording(studyId, sessionId, filePath)
            }

            // 상태 초기화
            _showSessionSelectDialog.value = false
            _pendingUploadFilePath.value = null
            _currentMeetingId.value = null
            _currentStudyId.value = null
            _currentSessionId.value = null
        }
    }

    /**
     * 세션 선택 다이얼로그 닫기 (업로드 취소)
     */
    fun dismissSessionSelectDialog() {
        _showSessionSelectDialog.value = false
        // 파일 경로는 유지 (다시 선택할 수 있도록)
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
