package com.ssafy.squiz.data.repository

import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*

/**
 * Workspace (스터디 채팅) Repository
 */
class WorkspaceRepository {

    private val api = RetrofitClient.workspaceApi

    /**
     * 스터디 워크스페이스 조회
     */
    suspend fun getWorkspaceByStudy(studyId: Long): Result<WorkspaceDTO> {
        return runCatching {
            val response = api.getWorkspaceByStudy(studyId)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("워크스페이스 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 생성
     */
    suspend fun createMessage(
        workspaceId: Long,
        content: String,
        messageType: String = "TEXT",
        fileUrl: String? = null
    ): Result<MessageDTO> {
        return runCatching {
            val request = MessageCreateRequest(
                workspaceId = workspaceId,
                content = content,
                messageType = messageType,
                fileUrl = fileUrl
            )
            val response = api.createMessage(workspaceId, request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("메시지 전송 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 목록 조회 (페이징)
     */
    suspend fun getMessages(
        workspaceId: Long,
        page: Int = 0,
        size: Int = 30
    ): Result<MessagePageResponse> {
        return runCatching {
            val response = api.getMessages(workspaceId, page, size)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("메시지 목록 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * 최근 메시지 조회
     */
    suspend fun getRecentMessages(
        workspaceId: Long,
        limit: Int = 50
    ): Result<List<MessageDTO>> {
        return runCatching {
            val response = api.getRecentMessages(workspaceId, limit)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                throw Exception("최근 메시지 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * 특정 시간 이후 메시지 조회 (폴링용)
     */
    suspend fun getMessagesAfter(
        workspaceId: Long,
        after: String
    ): Result<List<MessageDTO>> {
        return runCatching {
            val response = api.getMessagesAfter(workspaceId, after)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                throw Exception("새 메시지 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 검색
     */
    suspend fun searchMessages(
        workspaceId: Long,
        keyword: String,
        page: Int = 0,
        size: Int = 20
    ): Result<MessagePageResponse> {
        return runCatching {
            val response = api.searchMessages(workspaceId, keyword, page, size)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("메시지 검색 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 수정
     */
    suspend fun updateMessage(
        workspaceId: Long,
        messageId: Long,
        content: String
    ): Result<MessageDTO> {
        return runCatching {
            val request = MessageUpdateRequest(content)
            val response = api.updateMessage(workspaceId, messageId, request)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("메시지 수정 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 삭제
     */
    suspend fun deleteMessage(
        workspaceId: Long,
        messageId: Long
    ): Result<Unit> {
        return runCatching {
            val response = api.deleteMessage(workspaceId, messageId)
            if (!response.isSuccessful) {
                throw Exception("메시지 삭제 실패: ${response.code()}")
            }
        }
    }

    /**
     * 메시지 고정/해제
     */
    suspend fun togglePinMessage(
        workspaceId: Long,
        messageId: Long
    ): Result<MessageDTO> {
        return runCatching {
            val response = api.togglePinMessage(workspaceId, messageId)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                response.body()!!.data!!
            } else {
                throw Exception("메시지 고정 실패: ${response.code()}")
            }
        }
    }

    /**
     * 고정된 메시지 목록
     */
    suspend fun getPinnedMessages(
        workspaceId: Long
    ): Result<List<MessageDTO>> {
        return runCatching {
            val response = api.getPinnedMessages(workspaceId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                throw Exception("고정 메시지 조회 실패: ${response.code()}")
            }
        }
    }

    /**
     * 접속 중인 사용자 목록
     */
    suspend fun getOnlineUsers(
        workspaceId: Long
    ): Result<List<PresenceDTO>> {
        return runCatching {
            val response = api.getOnlineUsers(workspaceId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                throw Exception("접속자 조회 실패: ${response.code()}")
            }
        }
    }
}
