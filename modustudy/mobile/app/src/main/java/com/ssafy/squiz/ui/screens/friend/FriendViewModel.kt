package com.ssafy.squiz.ui.screens.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.data.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 친구 ViewModel
 */
class FriendViewModel : ViewModel() {

    private val repository = FriendRepository()

    // 친구 목록 상태
    private val _friendListState = MutableStateFlow<FriendListUiState>(FriendListUiState.Loading)
    val friendListState: StateFlow<FriendListUiState> = _friendListState.asStateFlow()

    // 사용자 검색 결과
    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    // 받은 친구 요청
    private val _receivedRequests = MutableStateFlow<List<ReceivedFriendRequest>>(emptyList())
    val receivedRequests: StateFlow<List<ReceivedFriendRequest>> = _receivedRequests.asStateFlow()

    // 보낸 친구 요청
    private val _sentRequests = MutableStateFlow<List<SentFriendRequest>>(emptyList())
    val sentRequests: StateFlow<List<SentFriendRequest>> = _sentRequests.asStateFlow()

    // 차단 목록
    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    // 작업 결과 이벤트
    private val _actionResult = MutableStateFlow<ActionResult?>(null)
    val actionResult: StateFlow<ActionResult?> = _actionResult.asStateFlow()

    init {
        loadFriends()
    }

    /**
     * 친구 목록 로드
     */
    fun loadFriends() {
        viewModelScope.launch {
            _friendListState.value = FriendListUiState.Loading
            repository.getFriends()
                .onSuccess { data ->
                    _friendListState.value = FriendListUiState.Success(data)
                }
                .onFailure { e ->
                    _friendListState.value = FriendListUiState.Error(e.message ?: "친구 목록 로드 실패")
                }
        }
    }

    /**
     * 사용자 검색
     */
    fun searchUsers(keyword: String) {
        if (keyword.length < 2) {
            _searchState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            repository.searchUsers(keyword)
                .onSuccess { results ->
                    _searchState.value = SearchUiState.Success(results)
                }
                .onFailure { e ->
                    _searchState.value = SearchUiState.Error(e.message ?: "검색 실패")
                }
        }
    }

    /**
     * 검색 초기화
     */
    fun clearSearch() {
        _searchState.value = SearchUiState.Idle
    }

    /**
     * 친구 요청 보내기
     */
    fun sendFriendRequest(userId: Long) {
        viewModelScope.launch {
            repository.sendFriendRequest(userId)
                .onSuccess {
                    _actionResult.value = ActionResult.Success("친구 요청을 보냈습니다.")
                    // 검색 결과 새로고침
                    val currentSearch = _searchState.value
                    if (currentSearch is SearchUiState.Success) {
                        val updatedResults = currentSearch.results.map { user ->
                            if (user.id == userId) user.copy(friendStatus = FriendStatus.PENDING_SENT)
                            else user
                        }
                        _searchState.value = SearchUiState.Success(updatedResults)
                    }
                }
                .onFailure { e ->
                    _actionResult.value = ActionResult.Error(e.message ?: "친구 요청 실패")
                }
        }
    }

    /**
     * 받은 친구 요청 로드
     */
    fun loadReceivedRequests() {
        viewModelScope.launch {
            repository.getReceivedRequests()
                .onSuccess { requests ->
                    _receivedRequests.value = requests
                }
                .onFailure { /* ignore */ }
        }
    }

    /**
     * 보낸 친구 요청 로드
     */
    fun loadSentRequests() {
        viewModelScope.launch {
            repository.getSentRequests()
                .onSuccess { requests ->
                    _sentRequests.value = requests
                }
                .onFailure { /* ignore */ }
        }
    }

    /**
     * 친구 요청 수락
     */
    fun acceptFriendRequest(requestId: Long) {
        viewModelScope.launch {
            repository.acceptFriendRequest(requestId)
                .onSuccess {
                    _actionResult.value = ActionResult.Success("친구 요청을 수락했습니다.")
                    loadReceivedRequests()
                    loadFriends()
                }
                .onFailure { e ->
                    _actionResult.value = ActionResult.Error(e.message ?: "수락 실패")
                }
        }
    }

    /**
     * 친구 요청 거절
     */
    fun rejectFriendRequest(requestId: Long) {
        viewModelScope.launch {
            repository.rejectFriendRequest(requestId)
                .onSuccess {
                    _actionResult.value = ActionResult.Success("친구 요청을 거절했습니다.")
                    loadReceivedRequests()
                }
                .onFailure { e ->
                    _actionResult.value = ActionResult.Error(e.message ?: "거절 실패")
                }
        }
    }

    /**
     * 친구 삭제
     */
    fun deleteFriend(friendId: Long) {
        viewModelScope.launch {
            repository.deleteFriend(friendId)
                .onSuccess {
                    _actionResult.value = ActionResult.Success("친구가 삭제되었습니다.")
                    loadFriends()
                }
                .onFailure { e ->
                    _actionResult.value = ActionResult.Error(e.message ?: "삭제 실패")
                }
        }
    }

    /**
     * 사용자 차단
     */
    fun blockUser(userId: Long) {
        viewModelScope.launch {
            repository.blockUser(userId)
                .onSuccess {
                    _actionResult.value = ActionResult.Success("사용자를 차단했습니다.")
                    loadFriends()
                    loadBlockedUsers()
                }
                .onFailure { e ->
                    _actionResult.value = ActionResult.Error(e.message ?: "차단 실패")
                }
        }
    }

    /**
     * 차단 해제
     */
    fun unblockUser(userId: Long) {
        viewModelScope.launch {
            repository.unblockUser(userId)
                .onSuccess {
                    _actionResult.value = ActionResult.Success("차단을 해제했습니다.")
                    loadBlockedUsers()
                }
                .onFailure { e ->
                    _actionResult.value = ActionResult.Error(e.message ?: "차단 해제 실패")
                }
        }
    }

    /**
     * 차단 목록 로드
     */
    fun loadBlockedUsers() {
        viewModelScope.launch {
            repository.getBlockedUsers()
                .onSuccess { users ->
                    _blockedUsers.value = users
                }
                .onFailure { /* ignore */ }
        }
    }

    /**
     * 액션 결과 소비
     */
    fun consumeActionResult() {
        _actionResult.value = null
    }
}

/**
 * 친구 목록 UI 상태
 */
sealed class FriendListUiState {
    object Loading : FriendListUiState()
    data class Success(val data: FriendListResponse) : FriendListUiState()
    data class Error(val message: String) : FriendListUiState()
}

/**
 * 검색 UI 상태
 */
sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<UserSearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

/**
 * 액션 결과
 */
sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Error(val message: String) : ActionResult()
}
