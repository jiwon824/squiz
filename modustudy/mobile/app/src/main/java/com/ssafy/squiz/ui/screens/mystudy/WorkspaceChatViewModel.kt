package com.ssafy.squiz.ui.screens.mystudy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.base.SquizApplication
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.data.repository.WorkspaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// UI 상태 클래스
sealed class WorkspaceUiState {
    object Loading : WorkspaceUiState()
    data class Success(val workspace: WorkspaceDTO) : WorkspaceUiState()
    data class Error(val message: String) : WorkspaceUiState()
}

sealed class MessagesUiState {
    object Loading : MessagesUiState()
    data class Success(
        val messages: List<MessageDTO>,
        val hasMore: Boolean = false
    ) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

class WorkspaceChatViewModel : ViewModel() {

    private val repository = WorkspaceRepository()

    // 워크스페이스 상태
    private val _workspaceState = MutableStateFlow<WorkspaceUiState>(WorkspaceUiState.Loading)
    val workspaceState: StateFlow<WorkspaceUiState> = _workspaceState.asStateFlow()

    // 메시지 목록 상태
    private val _messagesState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val messagesState: StateFlow<MessagesUiState> = _messagesState.asStateFlow()

    // 메시지 전송 중 상태
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // 접속 중인 사용자
    private val _onlineUsers = MutableStateFlow<List<PresenceDTO>>(emptyList())
    val onlineUsers: StateFlow<List<PresenceDTO>> = _onlineUsers.asStateFlow()

    // 현재 워크스페이스 ID
    private var currentWorkspaceId: Long? = null
    private var currentStudyId: Long? = null

    // 폴링 Job
    private var pollingJob: Job? = null

    // 마지막 메시지 시간 (폴링용)
    private var lastMessageTime: String? = null

    // 페이지네이션
    private var currentPage = 0
    private var isLastPage = false

    // 현재 사용자 ID
    private val currentUserId: Long
        get() = SquizApplication.getInstance().authManager.getCurrentUserId()

    /**
     * 스터디 워크스페이스 로드
     */
    fun loadWorkspace(studyId: Long) {
        currentStudyId = studyId
        viewModelScope.launch {
            _workspaceState.value = WorkspaceUiState.Loading
            repository.getWorkspaceByStudy(studyId)
                .onSuccess { workspace ->
                    currentWorkspaceId = workspace.id
                    _workspaceState.value = WorkspaceUiState.Success(workspace)
                    loadMessages()
                    startPolling()
                }
                .onFailure { error ->
                    _workspaceState.value = WorkspaceUiState.Error(error.message ?: "워크스페이스 로드 실패")
                }
        }
    }

    /**
     * 메시지 목록 로드
     */
    fun loadMessages(refresh: Boolean = true) {
        val workspaceId = currentWorkspaceId ?: return

        if (refresh) {
            currentPage = 0
            isLastPage = false
        }

        viewModelScope.launch {
            if (refresh) {
                _messagesState.value = MessagesUiState.Loading
            }

            repository.getRecentMessages(workspaceId, limit = 50)
                .onSuccess { messages ->
                    // 시간순으로 정렬 (최신이 아래로)
                    val sortedMessages = messages.sortedBy { it.createdAt }
                    _messagesState.value = MessagesUiState.Success(
                        messages = sortedMessages,
                        hasMore = false
                    )
                    // 마지막 메시지 시간 저장
                    lastMessageTime = sortedMessages.lastOrNull()?.createdAt
                }
                .onFailure { error ->
                    _messagesState.value = MessagesUiState.Error(error.message ?: "메시지 로드 실패")
                }
        }
    }

    /**
     * 더 많은 메시지 로드 (페이징)
     */
    fun loadMoreMessages() {
        val workspaceId = currentWorkspaceId ?: return
        if (isLastPage) return

        viewModelScope.launch {
            repository.getMessages(workspaceId, page = currentPage + 1)
                .onSuccess { pageResponse ->
                    val currentMessages = (_messagesState.value as? MessagesUiState.Success)?.messages ?: emptyList()
                    val newMessages = pageResponse.content.sortedBy { it.createdAt }

                    _messagesState.value = MessagesUiState.Success(
                        messages = newMessages + currentMessages, // 이전 메시지는 위에 추가
                        hasMore = !pageResponse.last
                    )
                    currentPage++
                    isLastPage = pageResponse.last
                }
                .onFailure { /* 무시 */ }
        }
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(content: String) {
        val workspaceId = currentWorkspaceId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            repository.createMessage(workspaceId, content)
                .onSuccess { newMessage ->
                    // 새 메시지를 목록에 추가
                    val currentMessages = (_messagesState.value as? MessagesUiState.Success)?.messages ?: emptyList()
                    _messagesState.value = MessagesUiState.Success(
                        messages = currentMessages + newMessage,
                        hasMore = false
                    )
                    lastMessageTime = newMessage.createdAt
                }
                .onFailure { /* 전송 실패 처리 */ }
            _isSending.value = false
        }
    }

    /**
     * 메시지 삭제
     */
    fun deleteMessage(messageId: Long) {
        val workspaceId = currentWorkspaceId ?: return

        viewModelScope.launch {
            repository.deleteMessage(workspaceId, messageId)
                .onSuccess {
                    // 목록에서 제거
                    val currentMessages = (_messagesState.value as? MessagesUiState.Success)?.messages ?: emptyList()
                    _messagesState.value = MessagesUiState.Success(
                        messages = currentMessages.filter { it.id != messageId },
                        hasMore = false
                    )
                }
        }
    }

    /**
     * 메시지 수정
     */
    fun updateMessage(messageId: Long, content: String) {
        val workspaceId = currentWorkspaceId ?: return

        viewModelScope.launch {
            repository.updateMessage(workspaceId, messageId, content)
                .onSuccess { updatedMessage ->
                    // 목록에서 업데이트
                    val currentMessages = (_messagesState.value as? MessagesUiState.Success)?.messages ?: emptyList()
                    _messagesState.value = MessagesUiState.Success(
                        messages = currentMessages.map {
                            if (it.id == messageId) updatedMessage else it
                        },
                        hasMore = false
                    )
                }
        }
    }

    /**
     * 메시지 고정/해제
     */
    fun togglePinMessage(messageId: Long) {
        val workspaceId = currentWorkspaceId ?: return

        viewModelScope.launch {
            repository.togglePinMessage(workspaceId, messageId)
                .onSuccess { updatedMessage ->
                    val currentMessages = (_messagesState.value as? MessagesUiState.Success)?.messages ?: emptyList()
                    _messagesState.value = MessagesUiState.Success(
                        messages = currentMessages.map {
                            if (it.id == messageId) updatedMessage else it
                        },
                        hasMore = false
                    )
                }
        }
    }

    /**
     * 새 메시지 폴링 시작
     */
    private fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(3000) // 3초마다 폴링
                pollNewMessages()
            }
        }
    }

    /**
     * 새 메시지 폴링
     */
    private suspend fun pollNewMessages() {
        val workspaceId = currentWorkspaceId ?: return
        val after = lastMessageTime ?: return

        repository.getMessagesAfter(workspaceId, after)
            .onSuccess { newMessages ->
                if (newMessages.isNotEmpty()) {
                    val currentMessages = (_messagesState.value as? MessagesUiState.Success)?.messages ?: emptyList()
                    val existingIds = currentMessages.map { it.id }.toSet()
                    val uniqueNewMessages = newMessages.filter { it.id !in existingIds }

                    if (uniqueNewMessages.isNotEmpty()) {
                        _messagesState.value = MessagesUiState.Success(
                            messages = (currentMessages + uniqueNewMessages).sortedBy { it.createdAt },
                            hasMore = false
                        )
                        lastMessageTime = uniqueNewMessages.maxByOrNull { it.createdAt }?.createdAt
                    }
                }
            }
    }

    /**
     * 폴링 중지
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * 접속 중인 사용자 조회
     */
    fun loadOnlineUsers() {
        val workspaceId = currentWorkspaceId ?: return

        viewModelScope.launch {
            repository.getOnlineUsers(workspaceId)
                .onSuccess { users ->
                    _onlineUsers.value = users
                }
        }
    }

    /**
     * 내 메시지인지 확인
     */
    fun isMyMessage(message: MessageDTO): Boolean {
        return message.userId == currentUserId
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
