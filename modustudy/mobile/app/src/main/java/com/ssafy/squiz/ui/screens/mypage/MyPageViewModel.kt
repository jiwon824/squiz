package com.ssafy.squiz.ui.screens.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 상태 클래스들
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfileDTO, val stats: GamificationStats? = null) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Saved : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class NotificationSettingsState {
    object Loading : NotificationSettingsState()
    data class Success(val settings: NotificationSettings) : NotificationSettingsState()
    data class Error(val message: String) : NotificationSettingsState()
}

sealed class ActivityStatsState {
    object Loading : ActivityStatsState()
    data class Success(val stats: ActivityStats) : ActivityStatsState()
    data class Error(val message: String) : ActivityStatsState()
}

sealed class GrassState {
    object Loading : GrassState()
    data class Success(val data: GrassData) : GrassState()
    data class Error(val message: String) : GrassState()
}

sealed class ActivityDetailState {
    object Loading : ActivityDetailState()
    data class Success(val detail: ActivityDetail) : ActivityDetailState()
    data class Error(val message: String) : ActivityDetailState()
}

sealed class PrivacySettingsState {
    object Loading : PrivacySettingsState()
    data class Success(val settings: PrivacySettings) : PrivacySettingsState()
    data class Error(val message: String) : PrivacySettingsState()
}

class MyPageViewModel : ViewModel() {

    // 프로필 상태
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // 저장 상태
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // 알림 설정 상태
    private val _notificationSettingsState = MutableStateFlow<NotificationSettingsState>(NotificationSettingsState.Loading)
    val notificationSettingsState: StateFlow<NotificationSettingsState> = _notificationSettingsState.asStateFlow()

    // 활동 통계 상태
    private val _activityStatsState = MutableStateFlow<ActivityStatsState>(ActivityStatsState.Loading)
    val activityStatsState: StateFlow<ActivityStatsState> = _activityStatsState.asStateFlow()

    // 잔디 상태
    private val _grassState = MutableStateFlow<GrassState>(GrassState.Loading)
    val grassState: StateFlow<GrassState> = _grassState.asStateFlow()

    // 활동 상세 상태
    private val _activityDetailState = MutableStateFlow<ActivityDetailState>(ActivityDetailState.Loading)
    val activityDetailState: StateFlow<ActivityDetailState> = _activityDetailState.asStateFlow()

    // 개인정보 설정 상태
    private val _privacySettingsState = MutableStateFlow<PrivacySettingsState>(PrivacySettingsState.Loading)
    val privacySettingsState: StateFlow<PrivacySettingsState> = _privacySettingsState.asStateFlow()

    // 프로필 로드 (실제 API 연동)
    fun loadMyProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                // 프로필과 통계를 병렬로 로드
                val profileResponse = RetrofitClient.userApi.getMyProfile()
                val statsResponse = try {
                    RetrofitClient.gamificationApi.getMyStats()
                } catch (e: Exception) {
                    null
                }

                if (profileResponse.isSuccessful) {
                    val profile = profileResponse.body()?.data
                    val stats = statsResponse?.body()?.data
                    if (profile != null) {
                        _profileState.value = ProfileState.Success(profile, stats)
                    } else {
                        _profileState.value = ProfileState.Error("프로필 정보를 찾을 수 없습니다.")
                    }
                } else {
                    _profileState.value = ProfileState.Error("프로필을 불러오는데 실패했습니다. (${profileResponse.code()})")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 프로필 업데이트 (실제 API 연동)
    fun updateProfile(nickname: String, bio: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val request = UpdateProfileRequest(nickname = nickname, bio = bio)
                val response = RetrofitClient.userApi.updateProfile(request)

                if (response.isSuccessful) {
                    val updatedProfile = response.body()?.data
                    if (updatedProfile != null) {
                        _profileState.value = ProfileState.Success(updatedProfile)
                    }
                    _saveState.value = SaveState.Saved
                    onSuccess()
                } else {
                    _saveState.value = SaveState.Error("프로필 저장에 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 알림 설정 로드 (실제 API 연동)
    fun loadNotificationSettings() {
        viewModelScope.launch {
            _notificationSettingsState.value = NotificationSettingsState.Loading
            try {
                val response = RetrofitClient.userApi.getNotificationSettings()

                if (response.isSuccessful) {
                    val settings = response.body()?.data
                    if (settings != null) {
                        _notificationSettingsState.value = NotificationSettingsState.Success(settings)
                    } else {
                        // 기본값 사용
                        _notificationSettingsState.value = NotificationSettingsState.Success(
                            NotificationSettings(
                                pushEnabled = true,
                                studyAlertEnabled = true,
                                chatAlertEnabled = true,
                                friendAlertEnabled = true
                            )
                        )
                    }
                } else {
                    _notificationSettingsState.value = NotificationSettingsState.Error("설정을 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _notificationSettingsState.value = NotificationSettingsState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 알림 설정 업데이트 (실제 API 연동)
    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.updateNotificationSettings(settings)

                if (response.isSuccessful) {
                    val updatedSettings = response.body()?.data ?: settings
                    _notificationSettingsState.value = NotificationSettingsState.Success(updatedSettings)
                } else {
                    _notificationSettingsState.value = NotificationSettingsState.Error("설정 저장에 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _notificationSettingsState.value = NotificationSettingsState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 활동 통계 로드 (실제 API 연동)
    fun loadActivityStats() {
        viewModelScope.launch {
            _activityStatsState.value = ActivityStatsState.Loading
            try {
                val response = RetrofitClient.userApi.getActivityStats()

                if (response.isSuccessful) {
                    val stats = response.body()?.data
                    if (stats != null) {
                        _activityStatsState.value = ActivityStatsState.Success(stats)
                    } else {
                        // 기본값 사용
                        _activityStatsState.value = ActivityStatsState.Success(
                            ActivityStats(
                                totalStudyDays = 0,
                                currentStreak = 0,
                                attendanceRate = 0,
                                totalStudyTime = 0
                            )
                        )
                    }
                } else {
                    _activityStatsState.value = ActivityStatsState.Error("통계를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _activityStatsState.value = ActivityStatsState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 잔디 데이터 로드 (실제 API 연동 - Gamification contributions)
    fun loadGrassData(year: Int, month: Int) {
        viewModelScope.launch {
            _grassState.value = GrassState.Loading
            try {
                val response = RetrofitClient.userApi.getGrassData(year, month)

                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        _grassState.value = GrassState.Success(data)
                    } else {
                        // 빈 데이터 생성 (contributions 비어있음)
                        _grassState.value = GrassState.Success(
                            GrassData(year = year, month = month, contributions = emptyList())
                        )
                    }
                } else {
                    _grassState.value = GrassState.Error("잔디 데이터를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _grassState.value = GrassState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 활동 상세 로드 (실제 API 연동)
    fun loadActivityDetail(date: String) {
        viewModelScope.launch {
            _activityDetailState.value = ActivityDetailState.Loading
            try {
                val response = RetrofitClient.userApi.getActivityDetail(date)

                if (response.isSuccessful) {
                    val detail = response.body()?.data
                    if (detail != null) {
                        _activityDetailState.value = ActivityDetailState.Success(detail)
                    } else {
                        // 빈 활동 (studyTime은 computed property)
                        _activityDetailState.value = ActivityDetailState.Success(
                            ActivityDetail(
                                date = date,
                                activities = emptyList()
                            )
                        )
                    }
                } else {
                    _activityDetailState.value = ActivityDetailState.Error("활동 상세를 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _activityDetailState.value = ActivityDetailState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 개인정보 설정 로드 (실제 API 연동)
    fun loadPrivacySettings() {
        viewModelScope.launch {
            _privacySettingsState.value = PrivacySettingsState.Loading
            try {
                val response = RetrofitClient.userApi.getPrivacySettings()

                if (response.isSuccessful) {
                    val settings = response.body()?.data
                    if (settings != null) {
                        _privacySettingsState.value = PrivacySettingsState.Success(settings)
                    } else {
                        // 기본값 사용
                        _privacySettingsState.value = PrivacySettingsState.Success(
                            PrivacySettings(profilePublic = true, activityPublic = true)
                        )
                    }
                } else {
                    _privacySettingsState.value = PrivacySettingsState.Error("설정을 불러오는데 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _privacySettingsState.value = PrivacySettingsState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }

    // 개인정보 설정 업데이트 (실제 API 연동)
    fun updatePrivacySettings(settings: PrivacySettings) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.userApi.updatePrivacySettings(settings)

                if (response.isSuccessful) {
                    val updatedSettings = response.body()?.data ?: settings
                    _privacySettingsState.value = PrivacySettingsState.Success(updatedSettings)
                } else {
                    _privacySettingsState.value = PrivacySettingsState.Error("설정 저장에 실패했습니다. (${response.code()})")
                }
            } catch (e: Exception) {
                _privacySettingsState.value = PrivacySettingsState.Error(e.message ?: "네트워크 오류가 발생했습니다.")
            }
        }
    }
}
