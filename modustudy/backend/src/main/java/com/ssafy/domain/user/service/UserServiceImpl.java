package com.ssafy.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.user.dto.request.ProfileSetupRequest;
import com.ssafy.domain.user.dto.request.UserUpdateRequest;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import com.ssafy.domain.user.dto.response.StatsResponse;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        // 닉네임 중복 체크
        if (request.getNickname() != null &&
                userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 닉네임 설정
        user.setNickname(request.getNickname());

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

        // 닉네임 변경 시 중복 체크
        if (request.getNickname() != null &&
                !request.getNickname().equals(user.getNickname())) {

            if (userRepository.existsByNickname(request.getNickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }

            user.setNickname(request.getNickname());
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

    // ========== 헬퍼 메서드 ========== ← 추가!

    /**
     * List를 JSON 문자열로 변환
     */
    private String convertToJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
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
}