package com.ssafy.squiz.ui.screens.dm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.data.repository.DMRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DM ViewModel
 */
class DMViewModel : ViewModel() {

    private val repository = DMRepository()

    // 대화 목록 상태
    private val _conversationsState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Loading)
    val conversationsState: StateFlow<ConversationsUiState> = _conversationsState.asStateFlow()

    // 현재 대화 상세
    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    // 메시지 목록
    private val _messages = MutableStateFlow<List<DMMessage>>(emptyList())
    val messages: StateFlow<List<DMMessage>> = _messages.asStateFlow()

    // 메시지 전송 상태
    private val _sendingMessage = MutableStateFlow(false)
    val sendingMessage: StateFlow<Boolean> = _sendingMessage.asStateFlow()

    // 현재 대화 정보
    private var currentConversationId: String? = null
    private var currentPartner: DMPartner? = null

    // 더 불러올 메시지가 있는지
    private var hasMoreMessages = false
    private var nextCursor: Long? = null

    // 작업 결과 이벤트
    private val _actionResult = MutableStateFlow<DMActionResult?>(null)
    val actionResult: StateFlow<DMActionResult?> = _actionResult.asStateFlow()

    init {
        loadConversations()
    }

    /**
     * 대화 목록 로드
     */
    fun loadConversations() {
        viewModelScope.launch {
            _conversationsState.value = ConversationsUiState.Loading
            repository.getConversations()
                .onSuccess { conversations ->
                    _conversationsState.value = ConversationsUiState.Success(conversations)
                }
                .onFailure { e ->
                    _conversationsState.value = ConversationsUiState.Error(e.message ?: "대화 목록 로드 실패")
                }
        }
    }

    /**
     * 대화 입장 (메시지 로드)
     */
    fun enterConversation(conversationId: String) {
        currentConversationId = conversationId
        _messages.value = emptyList()
        hasMoreMessages = false
        nextCursor = null

        viewModelScope.launch {
            _chatState.value = ChatUiState.Loading
            repository.getMessages(conversationId)
                .onSuccess { detail ->
                    currentPartner = detail.partner
                    _messages.value = detail.messages
                    hasMoreMessages = detail.hasMore
                    nextCursor = detail.nextCursor
                    _chatState.value = ChatUiState.Success(detail.partner)

                    // 읽음 처리
                    markAsRead(conversationId)
                }
                .onFailure { e ->
                    _chatState.value = ChatUiState.Error(e.message ?: "메시지 로드 실패")
                }
        }
    }

    /**
     * 이전 메시지 더 불러오기
     */
    fun loadMoreMessages() {
        val conversationId = currentConversationId ?: return
        if (!hasMoreMessages || nextCursor == null) return

        viewModelScope.launch {
            repository.getMessages(conversationId, nextCursor)
                .onSuccess { detail ->
                    _messages.value = _messages.value + detail.messages
                    hasMoreMessages = detail.hasMore
                    nextCursor = detail.nextCursor
                }
                .onFailure { /* ignore */ }
        }
    }

    /**
     * 새 대화 시작 (친구에게)
     */
    fun startConversation(partnerId: Long, message: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _sendingMessage.value = true
            repository.startConversation(partnerId, message)
                .onSuccess { response ->
                    _sendingMessage.value = false
                    loadConversations()
                    onSuccess(response.conversationId)
                }
                .onFailure { e ->
                    _sendingMessage.value = false
                    _actionResult.value = DMActionResult.Error(e.message ?: "대화 시작 실패")
                }
        }
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(content: String) {
        val conversationId = currentConversationId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _sendingMessage.value = true
            repository.sendMessage(conversationId, content)
                .onSuccess { newMessage ->
                    _sendingMessage.value = false
                    // 메시지 목록에 추가
                    _messages.value = listOf(
                        DMMessage(
                            id = newMessage.id,
                            content = newMessage.content,
                            senderId = newMessage.senderId,
                            isRead = newMessage.isRead,
                            createdAt = newMessage.createdAt
                        )
                    ) + _messages.value
                }
                .onFailure { e ->
                    _sendingMessage.value = false
                    _actionResult.value = DMActionResult.Error(e.message ?: "메시지 전송 실패")
                }
        }
    }

    /**
     * 읽음 처리
     */
    private fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            repository.markAsRead(conversationId)
            // 대화 목록 새로고침 (안읽은 개수 업데이트)
            loadConversations()
        }
    }

    /**
     * 대화 삭제
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
                .onSuccess {
                    _actionResult.value = DMActionResult.Success("대화가 삭제되었습니다.")
                    loadConversations()
                }
                .onFailure { e ->
                    _actionResult.value = DMActionResult.Error(e.message ?: "삭제 실패")
                }
        }
    }

    /**
     * 메시지 삭제
     */
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
                .onSuccess {
                    // 메시지 목록에서 제거
                    _messages.value = _messages.value.filter { it.id != messageId }
                    _actionResult.value = DMActionResult.Success("메시지가 삭제되었습니다.")
                }
                .onFailure { e ->
                    _actionResult.value = DMActionResult.Error(e.message ?: "삭제 실패")
                }
        }
    }

    /**
     * 대화 나가기
     */
    fun leaveConversation() {
        currentConversationId = null
        currentPartner = null
        _messages.value = emptyList()
        _chatState.value = ChatUiState.Loading
    }

    /**
     * 액션 결과 소비
     */
    fun consumeActionResult() {
        _actionResult.value = null
    }

    /**
     * 현재 파트너 정보 가져오기
     */
    fun getCurrentPartner(): DMPartner? = currentPartner
}

/**
 * 대화 목록 UI 상태
 */
sealed class ConversationsUiState {
    object Loading : ConversationsUiState()
    data class Success(val conversations: List<ConversationResponse>) : ConversationsUiState()
    data class Error(val message: String) : ConversationsUiState()
}

/**
 * 채팅 UI 상태
 */
sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val partner: DMPartner) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

/**
 * DM 액션 결과
 */
sealed class DMActionResult {
    data class Success(val message: String) : DMActionResult()
    data class Error(val message: String) : DMActionResult()
}
