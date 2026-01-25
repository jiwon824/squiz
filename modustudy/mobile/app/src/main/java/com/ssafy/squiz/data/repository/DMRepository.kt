package com.ssafy.squiz.data.repository

import android.util.Log
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DM (Direct Message) Repository
 */
class DMRepository {

    companion object {
        private const val TAG = "DMRepository"
    }

    private val api = RetrofitClient.dmApi

    /**
     * DM 대화 목록 조회
     */
    suspend fun getConversations(): Result<List<ConversationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getConversations()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "대화 목록 조회 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getConversations error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 대화 메시지 조회
     */
    suspend fun getMessages(
        conversationId: String,
        cursor: Long? = null,
        limit: Int? = 50
    ): Result<ConversationDetailResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMessages(conversationId, cursor, limit)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "메시지 조회 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMessages error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 새 대화 시작
     */
    suspend fun startConversation(
        partnerId: Long,
        message: String
    ): Result<StartConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = StartConversationRequest(partnerId, message)
            val response = api.startConversation(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "대화 시작 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "startConversation error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 메시지 전송
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String
    ): Result<DMMessageResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SendMessageRequest(content)
            val response = api.sendMessage(conversationId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "메시지 전송 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 메시지 읽음 처리
     */
    suspend fun markAsRead(conversationId: String): Result<ReadMessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.markAsRead(conversationId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "읽음 처리 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "markAsRead error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 대화 삭제
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteConversation(conversationId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error?.message ?: "대화 삭제 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteConversation error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 메시지 삭제
     */
    suspend fun deleteMessage(messageId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteMessage(messageId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error?.message ?: "메시지 삭제 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteMessage error: ${e.message}")
            Result.failure(e)
        }
    }
}
