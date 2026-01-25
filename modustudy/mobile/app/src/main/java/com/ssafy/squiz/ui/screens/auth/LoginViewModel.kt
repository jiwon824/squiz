package com.ssafy.squiz.ui.screens.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.ssafy.squiz.data.repository.AuthRepository
import com.ssafy.squiz.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 로그인 화면 ViewModel
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
        const val RC_GOOGLE_SIGN_IN = 9001
    }

    // UI 상태
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 로그인 결과 이벤트
    private val _loginEvent = MutableStateFlow<LoginEvent?>(null)
    val loginEvent: StateFlow<LoginEvent?> = _loginEvent.asStateFlow()

    /**
     * 카카오 로그인
     */
    fun loginWithKakao(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedProvider = "kakao",
                errorMessage = null
            )

            authRepository.loginWithKakao(activity)
                .onSuccess { result ->
                    handleLoginSuccess(result)
                }
                .onFailure { error ->
                    handleLoginError(error)
                }
        }
    }

    /**
     * 네이버 로그인
     */
    fun loginWithNaver(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedProvider = "naver",
                errorMessage = null
            )

            authRepository.loginWithNaver(activity)
                .onSuccess { result ->
                    handleLoginSuccess(result)
                }
                .onFailure { error ->
                    handleLoginError(error)
                }
        }
    }

    /**
     * 구글 로그인 시작
     * Activity에서 startActivityForResult로 호출해야 함
     */
    fun getGoogleSignInIntent(activity: Activity): Intent {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            selectedProvider = "google",
            errorMessage = null
        )

        val googleSignInClient = authRepository.getGoogleSignInClient(activity)
        return googleSignInClient.signInIntent
    }

    /**
     * 구글 로그인 결과 처리
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val serverAuthCode = account?.serverAuthCode

                if (serverAuthCode != null) {
                    authRepository.handleGoogleSignInResult(serverAuthCode)
                        .onSuccess { result ->
                            handleLoginSuccess(result)
                        }
                        .onFailure { error ->
                            handleLoginError(error)
                        }
                } else {
                    handleLoginError(Exception("Google Sign-In: No server auth code"))
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed: ${e.statusCode}")
                handleLoginError(e)
            }
        }
    }

    private fun handleLoginSuccess(result: AuthResult) {
        Log.d(TAG, "로그인 성공: isNewUser=${result.isNewUser}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = null
        )

        _loginEvent.value = if (result.isNewUser) {
            LoginEvent.NavigateToAdditionalInfo
        } else {
            LoginEvent.NavigateToMain
        }
    }

    private fun handleLoginError(error: Throwable) {
        Log.e(TAG, "로그인 실패: ${error.message}")

        val errorMessage = when {
            error.message?.contains("cancelled", ignoreCase = true) == true -> null // 사용자 취소
            error.message?.contains("network", ignoreCase = true) == true -> "네트워크 오류가 발생했습니다."
            else -> "로그인에 실패했습니다. 다시 시도해주세요."
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            selectedProvider = null,
            errorMessage = errorMessage
        )
    }

    /**
     * 이벤트 소비 후 초기화
     */
    fun consumeLoginEvent() {
        _loginEvent.value = null
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * 로그인 UI 상태
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val selectedProvider: String? = null,
    val errorMessage: String? = null
)

/**
 * 로그인 이벤트
 */
sealed class LoginEvent {
    object NavigateToMain : LoginEvent()
    object NavigateToAdditionalInfo : LoginEvent()
}
