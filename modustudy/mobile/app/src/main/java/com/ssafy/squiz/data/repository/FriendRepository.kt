package com.ssafy.squiz.data.repository

import android.util.Log
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 친구 Repository
 */
class FriendRepository {

    companion object {
        private const val TAG = "FriendRepository"
    }

    private val api = RetrofitClient.friendApi

    /**
     * 친구 목록 조회
     */
    suspend fun getFriends(status: String? = null): Result<FriendListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFriends(status)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "친구 목록 조회 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFriends error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 사용자 검색
     */
    suspend fun searchUsers(keyword: String): Result<List<UserSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchUsers(keyword)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "사용자 검색 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchUsers error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 친구 요청 보내기
     */
    suspend fun sendFriendRequest(userId: Long): Result<FriendRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.sendFriendRequest(FriendRequestBody(userId))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "친구 요청 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendFriendRequest error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 받은 친구 요청 목록
     */
    suspend fun getReceivedRequests(): Result<List<ReceivedFriendRequest>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReceivedRequests()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "받은 요청 조회 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getReceivedRequests error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 보낸 친구 요청 목록
     */
    suspend fun getSentRequests(): Result<List<SentFriendRequest>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSentRequests()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "보낸 요청 조회 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSentRequests error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 친구 요청 수락
     */
    suspend fun acceptFriendRequest(requestId: Long): Result<AcceptFriendResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.acceptFriendRequest(requestId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "친구 요청 수락 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "acceptFriendRequest error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 친구 요청 거절
     */
    suspend fun rejectFriendRequest(requestId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.rejectFriendRequest(requestId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error?.message ?: "친구 요청 거절 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "rejectFriendRequest error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 친구 삭제
     */
    suspend fun deleteFriend(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteFriend(friendId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error?.message ?: "친구 삭제 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteFriend error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 사용자 차단
     */
    suspend fun blockUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.blockUser(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error?.message ?: "차단 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "blockUser error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 차단 해제
     */
    suspend fun unblockUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.unblockUser(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error?.message ?: "차단 해제 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "unblockUser error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 차단 목록 조회
     */
    suspend fun getBlockedUsers(): Result<List<BlockedUser>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBlockedUsers()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error?.message ?: "차단 목록 조회 실패"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getBlockedUsers error: ${e.message}")
            Result.failure(e)
        }
    }
}
