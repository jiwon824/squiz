package com.ssafy.domain.user.service;

import com.ssafy.domain.user.dto.request.ProfileSetupRequest;
import com.ssafy.domain.user.dto.request.StudyPreferenceRequest;
import com.ssafy.domain.user.dto.request.UserUpdateRequest;
import com.ssafy.domain.user.dto.response.StudyPreferenceResponse;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.dto.response.StatsResponse;

import java.util.Optional;

public interface UserService {

    /**
     * userId로 사용자 조회
     */
    Optional<User> getUserByUserId(String userId);

    /**
     * id로 사용자 조회
     */
    Optional<User> getUserById(Long id);

    /**
     * email로 사용자 조회
     */
    Optional<User> getUserByEmail(String email);

    /**
     * socialAccounts와 함께 사용자 조회
     */
    User getUserWithSocialAccounts(Long userId);

    /**
     * 추가 정보 입력 (최초 로그인)
     */
    User setupProfile(Long userId, ProfileSetupRequest request);

    /**
     * 내 정보 수정
     */
    User updateUserInfo(Long userId, UserUpdateRequest request);

    StatsResponse getServiceStats();

    /**
     * 스터디 선호 설정 조회
     */
    StudyPreferenceResponse getStudyPreference(Long userId);

    /**
     * 스터디 선호 설정 수정
     */
    StudyPreferenceResponse updateStudyPreference(Long userId, StudyPreferenceRequest request);

    /**
     * 프로필 이미지 업로드
     */
    User updateProfileImage(Long userId, org.springframework.web.multipart.MultipartFile file);

    /**
     * 프로필 이미지 삭제 (기본 이미지로 변경)
     */
    User deleteProfileImage(Long userId);

}