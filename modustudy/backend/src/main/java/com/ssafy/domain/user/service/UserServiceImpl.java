package com.ssafy.domain.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.user.dto.request.ProfileSetupRequest;
import com.ssafy.domain.user.dto.request.StudyPreferenceRequest;
import com.ssafy.domain.user.dto.request.UserUpdateRequest;
import com.ssafy.domain.user.dto.response.StudyPreferenceResponse;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.material.service.FileStorageService;
import com.ssafy.domain.material.service.FileStorageService.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.ssafy.domain.user.dto.response.StatsResponse;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    // 프로필 이미지용 허용 확장자
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByEmail(userId);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 추가 정보 입력 (최초 로그인)
     */
    @Override
    @Transactional
    public User setupProfile(Long userId, ProfileSetupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 닉네임 필수 체크
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        // 닉네임 길이 체크 (50자 제한)
        if (request.getNickname().length() > 50) {
            throw new IllegalArgumentException("닉네임은 50자 이내로 입력해주세요.");
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 닉네임 설정
        user.setNickname(request.getNickname().trim());

        // 실명 설정
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        // 비밀번호 설정 (암호화!)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.setPassword(encodedPassword);
        }

        // 관심 분야 설정 (선택) ← 추가!
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            user.setInterests(convertToJson(request.getInterests()));
        }

        // 기술 스택 설정 (선택) ← 추가!
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            user.setTechStacks(convertToJson(request.getTechStacks()));
        }

        return userRepository.save(user);
    }

    /**
     * 내 정보 수정
     */
    @Override
    @Transactional
    public User updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 닉네임 필수 체크
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        // 닉네임 길이 체크 (50자 제한)
        if (request.getNickname().length() > 50) {
            throw new IllegalArgumentException("닉네임은 50자 이내로 입력해주세요.");
        }

        String trimmedNickname = request.getNickname().trim();

        // 닉네임 변경 시 중복 체크
        if (!trimmedNickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(trimmedNickname)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(trimmedNickname);
        }

        // 실명 업데이트
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        // 자기소개 업데이트
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserWithSocialAccounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // LAZY 로딩 강제 초기화
        user.getSocialAccounts().size();

        return user;
    }

    // ========== 스터디 선호 설정 ==========

    @Override
    @Transactional(readOnly = true)
    public StudyPreferenceResponse getStudyPreference(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return StudyPreferenceResponse.builder()
                .techStacks(parseJsonList(user.getTechStacks()))
                .availableDays(parseJsonList(user.getAvailableDays()))
                .preferredTimeSlots(parseJsonList(user.getPreferredTimeSlots()))
                .preferredDurationWeeks(user.getPreferredDurationWeeks())
                .build();
    }

    @Override
    @Transactional
    public StudyPreferenceResponse updateStudyPreference(Long userId, StudyPreferenceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기술 스택 업데이트
        if (request.getTechStacks() != null) {
            user.setTechStacks(convertToJson(request.getTechStacks()));
        }

        // 가능 요일 업데이트
        if (request.getAvailableDays() != null) {
            user.setAvailableDays(convertToJson(request.getAvailableDays()));
        }

        // 선호 시간대 업데이트
        if (request.getPreferredTimeSlots() != null) {
            user.setPreferredTimeSlots(convertToJson(request.getPreferredTimeSlots()));
        }

        // 선호 기간 업데이트
        if (request.getPreferredDurationWeeks() != null) {
            int weeks = Math.max(2, Math.min(8, request.getPreferredDurationWeeks()));
            user.setPreferredDurationWeeks(weeks);
        }

        User savedUser = userRepository.save(user);

        return StudyPreferenceResponse.builder()
                .techStacks(parseJsonList(savedUser.getTechStacks()))
                .availableDays(parseJsonList(savedUser.getAvailableDays()))
                .preferredTimeSlots(parseJsonList(savedUser.getPreferredTimeSlots()))
                .preferredDurationWeeks(savedUser.getPreferredDurationWeeks())
                .build();
    }

    // ========== 헬퍼 메서드 ==========

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List를 JSON 문자열로 변환
     */
    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * JSON 문자열을 List<String>으로 변환
     */
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 서비스 통계 조회
     */
    @Override
    public StatsResponse getServiceStats() {
        // 전체 사용자 수
        long totalUsers = userRepository.count();

        // 활성 사용자 수
        long activeUsers = userRepository.countByIsActive(true);

        // 오늘 가입자 수
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayNewUsers = userRepository.countByCreatedAtAfter(todayStart);

        // TODO: 스터디 관련 통계 (Study 도메인 구현 후 추가)
        long totalStudies = 0L;
        long activeStudies = 0L;

        return StatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .todayNewUsers(todayNewUsers)
                .totalStudies(totalStudies)
                .activeStudies(activeStudies)
                .build();
    }

    // ========== 프로필 이미지 ==========

    /**
     * 프로필 이미지 업로드
     */
    @Override
    @Transactional
    public User updateProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 파일 유효성 검사
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        // 이미지 확장자 검사
        String extension = getFileExtension(fileName).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
        }

        // 기존 프로필 이미지 삭제 (로컬 업로드 파일인 경우에만)
        if (user.getProfileImage() != null && !user.getProfileImage().isBlank()) {
            String oldPath = user.getProfileImage();
            // /uploads/ 로 시작하는 로컬 파일만 삭제 (외부 URL은 건너뛰기)
            if (oldPath.startsWith("/uploads/")) {
                oldPath = oldPath.substring("/uploads/".length());
                fileStorageService.delete(oldPath);
            }
        }

        // 새 이미지 업로드
        FileUploadResult result = fileStorageService.upload(file, "profiles");

        // URL 형식으로 저장 (프론트엔드에서 바로 사용 가능)
        String imageUrl = "/uploads/" + result.filePath();
        user.setProfileImage(imageUrl);

        return userRepository.save(user);
    }

    /**
     * 프로필 이미지 삭제 (기본 이미지로 변경)
     */
    @Override
    @Transactional
    public User deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 프로필 이미지 삭제 (로컬 업로드 파일인 경우에만)
        if (user.getProfileImage() != null && !user.getProfileImage().isBlank()) {
            String oldPath = user.getProfileImage();
            // /uploads/ 로 시작하는 로컬 파일만 삭제 (외부 URL은 건너뛰기)
            if (oldPath.startsWith("/uploads/")) {
                oldPath = oldPath.substring("/uploads/".length());
                fileStorageService.delete(oldPath);
            }
        }

        // 프로필 이미지를 null로 설정 (프론트에서 기본 이미지 표시)
        user.setProfileImage(null);

        return userRepository.save(user);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
