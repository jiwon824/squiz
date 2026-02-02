package com.ssafy.squiz.data.repository

import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*

/**
 * DM (1:1 채팅) Repository
 */
class DMRepository {

    private val api = RetrofitClient.dmApi

    /**
     * DM 대화방 목록 조회
     */
    suspend fun getConversations(): Result<List<DmConversationDTO>> {
        return runCatching {
            val response = api.getConversations()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                throw Exception("대화방 목록 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * 대화방 메시지 목록 조회
     */
    suspend fun getMessages(
        conversationId: Long,
        page: Int = 0,
        size: Int = 30
    ): Result<List<DirectMessageDTO>> {
        return runCatching {
            val response = api.getMessages(conversationId, page, size)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                throw Exception("메시지 목록 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * DM 메시지 전송
     */
    suspend fun sendMessage(
        receiverId: Long,
        content: String
    ): Result<DirectMessageDTO> {
        return runCatching {
            val request = DirectMessageRequest(receiverId, content)
            val response = api.sendMessage(request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("메시지 전송 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 읽음 처리
     */
    suspend fun markAsRead(conversationId: Long): Result<Unit> {
        return runCatching {
            val response = api.markAsRead(conversationId)
            if (!response.isSuccessful) {
                throw Exception("읽음 처리 실패: ${response.code()}")
            }
        }
    }

    /**
     * 대화방 삭제
     */
    suspend fun deleteConversation(conversationId: Long): Result<Unit> {
        return runCatching {
            val response = api.deleteConversation(conversationId)
            if (!response.isSuccessful) {
                throw Exception("대화방 삭제 실패: ${response.code()}")
            }
        }
    }

    /**
     * 읽지 않은 메시지 총 개수
     */
    suspend fun getUnreadCount(): Result<Int> {
        return runCatching {
            val response = api.getUnreadCount()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.count ?: 0
            } else {
                0
            }
        }
    }
}
