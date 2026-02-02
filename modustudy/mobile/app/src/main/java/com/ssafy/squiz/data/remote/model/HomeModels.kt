package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// 홈 화면 데이터
data class HomeData(
    @SerializedName("user") val user: HomeUserInfo,
    @SerializedName("todayScheduleCount") val todayScheduleCount: Int = 0,
    @SerializedName("recommendedStudies") val recommendedStudies: List<StudyDTO> = emptyList(),
    @SerializedName("popularStudies") val popularStudies: List<StudyDTO> = emptyList(),
    @SerializedName("recentStudies") val recentStudies: List<StudyDTO> = emptyList()
)

data class HomeUserInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String? = null
)
