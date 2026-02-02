package com.ssafy.squiz.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 상태 클래스들
sealed class StudiesUiState {
    object Loading : StudiesUiState()
    data class Success(
        val studies: List<StudyDTO>,
        val totalCount: Long = 0,
        val hasMore: Boolean = false
    ) : StudiesUiState()
    data class Error(val message: String) : StudiesUiState()
}

sealed class StudyDetailUiState {
    object Loading : StudyDetailUiState()
    data class Success(val study: StudyDetailDTO) : StudyDetailUiState()
    data class Error(val message: String) : StudyDetailUiState()
}

class StudyViewModel : ViewModel() {

    // 스터디 목록 상태
    private val _studiesState = MutableStateFlow<StudiesUiState>(StudiesUiState.Loading)
    val studiesState: StateFlow<StudiesUiState> = _studiesState.asStateFlow()

    // 내 스터디 목록 상태
    private val _myStudiesState = MutableStateFlow<StudiesUiState>(StudiesUiState.Loading)
    val myStudiesState: StateFlow<StudiesUiState> = _myStudiesState.asStateFlow()

    // 스터디 상세 상태
    private val _studyDetailState = MutableStateFlow<StudyDetailUiState>(StudyDetailUiState.Loading)
    val studyDetailState: StateFlow<StudyDetailUiState> = _studyDetailState.asStateFlow()

    // 검색 쿼리
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 필터 상태
    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _selectedMeetingType = MutableStateFlow<String?>(null)
    val selectedMeetingType: StateFlow<String?> = _selectedMeetingType.asStateFlow()

    private val _selectedTopicId = MutableStateFlow<Long?>(null)
    val selectedTopicId: StateFlow<Long?> = _selectedTopicId.asStateFlow()

    // 토픽 목록
    private val _topics = MutableStateFlow<List<TopicDTO>>(emptyList())
    val topics: StateFlow<List<TopicDTO>> = _topics.asStateFlow()

    // 페이지네이션
    private var currentPage = 0
    private var isLastPage = false
    private var isLoading = false

    init {
        loadTopics()
    }

    // 토픽 목록 로드 (현재 하드코딩 - 백엔드 API가 생기면 연동)
    private fun loadTopics() {
        viewModelScope.launch {
            _topics.value = listOf(
                TopicDTO(1, "프로그래밍"),
                TopicDTO(2, "외국어"),
                TopicDTO(3, "자격증"),
                TopicDTO(4, "취업"),
                TopicDTO(5, "기타")
            )
        }
    }

    // 스터디 목록 로드 (실제 API 연동)
    fun loadStudies(refresh: Boolean = false) {
        if (isLoading) return
        if (refresh) {
            currentPage = 0
            isLastPage = false
        }
        if (isLastPage && !refresh) return

        isLoading = true
        viewModelScope.launch {
            try {
                if (refresh) {
                    _studiesState.value = StudiesUiState.Loading
                }

                // 검색 쿼리나 필터가 있으면 검색 API 사용
                val response = if (_searchQuery.value.isNotEmpty() || _selectedTopicId.value != null || _selectedMeetingType.value != null) {
                    RetrofitClient.studyApi.searchStudies(
                        keyword = _searchQuery.value.takeIf { it.isNotEmpty() },
                        topicId = _selectedTopicId.value,
                        meetingType = _selectedMeetingType.value,
                        page = currentPage,
                        size = 20
                    )
                } else {
                    // 기본: 모집중인 스터디 목록
                    RetrofitClient.studyApi.getRecruitingStudies(
                        page = currentPage,
                        size = 20
                    )
                }

                if (response.isSuccessful) {
                    val pageResponse = response.body()
                    val studies = pageResponse?.content ?: emptyList()

                    val currentList = if (refresh) emptyList() else {
                        (_studiesState.value as? StudiesUiState.Success)?.studies ?: emptyList()
                    }

                    _studiesState.value = StudiesUiState.Success(
                        studies = currentList + studies,
                        totalCount = pageResponse?.totalElements ?: 0,
                        hasMore = !(pageResponse?.last ?: true)
                    )

                    isLastPage = pageResponse?.last ?: true
                    currentPage++
                } else {
                    _studiesState.value = StudiesUiState.Error("스터디 목록을 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _studiesState.value = StudiesUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            } finally {
                isLoading = false
            }
        }
    }

    // 더 불러오기
    fun loadMore() {
        if (!isLastPage && !isLoading) {
            loadStudies(refresh = false)
        }
    }

    // 내 스터디 로드 (실제 API 연동)
    fun loadMyStudies() {
        viewModelScope.launch {
            _myStudiesState.value = StudiesUiState.Loading
            try {
                val response = RetrofitClient.studyApi.getMyStudies(page = 0, size = 50)

                if (response.isSuccessful) {
                    val pageResponse = response.body()
                    val studies = pageResponse?.content ?: emptyList()

                    _myStudiesState.value = StudiesUiState.Success(
                        studies = studies,
                        totalCount = pageResponse?.totalElements ?: 0
                    )
                } else {
                    _myStudiesState.value = StudiesUiState.Error("내 스터디를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _myStudiesState.value = StudiesUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 스터디 상세 로드 (실제 API 연동)
    fun loadStudyDetail(studyId: Long) {
        viewModelScope.launch {
            _studyDetailState.value = StudyDetailUiState.Loading
            try {
                val response = RetrofitClient.studyApi.getStudyDetail(studyId)

                if (response.isSuccessful) {
                    val detail = response.body()
                    if (detail != null) {
                        _studyDetailState.value = StudyDetailUiState.Success(detail)
                    } else {
                        _studyDetailState.value = StudyDetailUiState.Error("스터디 정보를 찾을 수 없습니다.")
                    }
                } else {
                    _studyDetailState.value = StudyDetailUiState.Error("스터디 상세를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _studyDetailState.value = StudyDetailUiState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 검색
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun search() {
        loadStudies(refresh = true)
    }

    // 필터
    fun updateStatusFilter(status: String?) {
        _selectedStatus.value = status
    }

    fun updateMeetingTypeFilter(type: String?) {
        _selectedMeetingType.value = type
    }

    fun updateTopicFilter(topicId: Long?) {
        _selectedTopicId.value = topicId
        loadStudies(refresh = true)
    }

    fun resetFilters() {
        _selectedStatus.value = null
        _selectedMeetingType.value = null
        _selectedTopicId.value = null
    }

    // 스터디 지원 (실제 API 연동)
    fun applyToStudy(studyId: Long, message: String, onResult: (Result<StudyApplicationDTO>) -> Unit) {
        viewModelScope.launch {
            try {
                val request = StudyApplicationRequest(message = message)
                val response = RetrofitClient.studyApi.applyToStudy(studyId, request)

                if (response.isSuccessful) {
                    val result = StudyApplicationDTO(
                        id = 0,
                        studyId = studyId,
                        userId = 0,
                        message = message,
                        status = "PENDING"
                    )
                    onResult(Result.success(result))
                } else {
                    onResult(Result.failure(Exception("지원에 실패했습니다. (${response.code()})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    // 북마크 토글 (실제 API 연동)
    fun toggleBookmark(studyId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.studyApi.toggleBookmark(studyId)

                if (response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(Exception("북마크 변경에 실패했습니다. (${response.code()})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
