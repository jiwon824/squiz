package com.ssafy.squiz.ui.screens.dm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.base.SquizApplication
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.data.repository.DMRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// UI 상태 클래스
sealed class ConversationsUiState {
    object Loading : ConversationsUiState()
    data class Success(val conversations: List<DmConversationDTO>) : ConversationsUiState()
    data class Error(val message: String) : ConversationsUiState()
}

sealed class DmMessagesUiState {
    object Loading : DmMessagesUiState()
    data class Success(
        val messages: List<DirectMessageDTO>,
        val hasMore: Boolean = false
    ) : DmMessagesUiState()
    data class Error(val message: String) : DmMessagesUiState()
}

class DMViewModel : ViewModel() {

    private val repository = DMRepository()

    // 대화방 목록 상태
    private val _conversationsState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Loading)
    val conversationsState: StateFlow<ConversationsUiState> = _conversationsState.asStateFlow()

    // 메시지 목록 상태
    private val _messagesState = MutableStateFlow<DmMessagesUiState>(DmMessagesUiState.Loading)
    val messagesState: StateFlow<DmMessagesUiState> = _messagesState.asStateFlow()

    // 메시지 전송 중 상태
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // 읽지 않은 메시지 수
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // 현재 대화방
    private var currentConversationId: Long? = null
    private var currentPartnerId: Long? = null
    private var currentPartnerNickname: String? = null

    // 폴링 Job
    private var pollingJob: Job? = null

    // 페이지네이션
    private var currentPage = 0
    private var isLastPage = false

    // 현재 사용자 ID
    private val currentUserId: Long
        get() = SquizApplication.getInstance().authManager.getCurrentUserId()

    /**
     * 대화방 목록 로드
     */
    fun loadConversations() {
        viewModelScope.launch {
            _conversationsState.value = ConversationsUiState.Loading
            repository.getConversations()
                .onSuccess { conversations ->
                    _conversationsState.value = ConversationsUiState.Success(
                        conversations.sortedByDescending { it.lastMessageAt }
                    )
                }
                .onFailure { error ->
                    _conversationsState.value = ConversationsUiState.Error(error.message ?: "대화방 목록 로드 실패")
                }
        }
    }

    /**
     * 대화방 메시지 로드
     */
    fun loadMessages(conversationId: Long, partnerId: Long, partnerNickname: String? = null) {
        currentConversationId = conversationId
        currentPartnerId = partnerId
        currentPartnerNickname = partnerNickname
        currentPage = 0
        isLastPage = false

        viewModelScope.launch {
            _messagesState.value = DmMessagesUiState.Loading

            repository.getMessages(conversationId, page = 0)
                .onSuccess { messages ->
                    // 시간순으로 정렬 (최신이 아래로)
                    val sortedMessages = messages.sortedBy { it.createdAt }
                    _messagesState.value = DmMessagesUiState.Success(
                        messages = sortedMessages,
                        hasMore = false
                    )

                    // 읽음 처리
                    markAsRead(conversationId)
                }
                .onFailure { error ->
                    _messagesState.value = DmMessagesUiState.Error(error.message ?: "메시지 로드 실패")
                }

            // 폴링 시작
            startPolling()
        }
    }

    /**
     * 더 많은 메시지 로드
     */
    fun loadMoreMessages() {
        val conversationId = currentConversationId ?: return
        if (isLastPage) return

        viewModelScope.launch {
            repository.getMessages(conversationId, page = currentPage + 1)
                .onSuccess { messages ->
                    val currentMessages = (_messagesState.value as? DmMessagesUiState.Success)?.messages ?: emptyList()
                    val newMessages = messages.sortedBy { it.createdAt }

                    _messagesState.value = DmMessagesUiState.Success(
                        messages = newMessages + currentMessages, // 이전 메시지는 위에 추가
                        hasMore = false
                    )
                    currentPage++
                }
        }
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(content: String) {
        val partnerId = currentPartnerId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            repository.sendMessage(partnerId, content)
                .onSuccess { newMessage ->
                    // 새 메시지를 목록에 추가
                    val currentMessages = (_messagesState.value as? DmMessagesUiState.Success)?.messages ?: emptyList()
                    _messagesState.value = DmMessagesUiState.Success(
                        messages = currentMessages + newMessage,
                        hasMore = false
                    )
                }
                .onFailure { /* 전송 실패 처리 */ }
            _isSending.value = false
        }
    }

    /**
     * 새로운 대화 시작 (사용자 프로필에서 DM 시작 시)
     */
    fun startNewConversation(partnerId: Long, partnerNickname: String, onConversationCreated: (Long) -> Unit) {
        currentPartnerId = partnerId
        currentPartnerNickname = partnerNickname

        // 기존 대화방이 있는지 확인
        val existingConversation = (_conversationsState.value as? ConversationsUiState.Success)
            ?.conversations
            ?.find { it.partnerId == partnerId }

        if (existingConversation != null) {
            onConversationCreated(existingConversation.conversationId)
        } else {
            // 새 대화방은 첫 메시지 전송 시 생성됨
            // 임시 ID로 시작
            onConversationCreated(-1)
        }
    }

    /**
     * 읽음 처리
     */
    private fun markAsRead(conversationId: Long) {
        viewModelScope.launch {
            repository.markAsRead(conversationId)
            // 대화방 목록 새로고침
            loadConversations()
            loadUnreadCount()
        }
    }

    /**
     * 대화방 삭제
     */
    fun deleteConversation(conversationId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
                .onSuccess {
                    onSuccess()
                    loadConversations()
                }
        }
    }

    /**
     * 읽지 않은 메시지 수 조회
     */
    fun loadUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount()
                .onSuccess { count ->
                    _unreadCount.value = count
                }
        }
    }

    /**
     * 폴링 시작
     */
    private fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000) // 5초마다 폴링
                pollNewMessages()
            }
        }
    }

    /**
     * 새 메시지 폴링
     */
    private suspend fun pollNewMessages() {
        val conversationId = currentConversationId ?: return

        repository.getMessages(conversationId, page = 0, size = 10)
            .onSuccess { messages ->
                val currentMessages = (_messagesState.value as? DmMessagesUiState.Success)?.messages ?: emptyList()
                val existingIds = currentMessages.map { it.messageId }.toSet()
                val newMessages = messages.filter { it.messageId !in existingIds }

                if (newMessages.isNotEmpty()) {
                    _messagesState.value = DmMessagesUiState.Success(
                        messages = (currentMessages + newMessages).sortedBy { it.createdAt },
                        hasMore = false
                    )
                    // 읽음 처리
                    markAsRead(conversationId)
                }
            }
    }

    /**
     * 폴링 중지
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * 현재 파트너 정보 가져오기
     */
    fun getPartnerInfo(): Pair<Long?, String?> {
        return Pair(currentPartnerId, currentPartnerNickname)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
