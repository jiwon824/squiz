package com.ssafy.domain.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.notification.dto.request.FcmTokenDeleteRequest;
import com.ssafy.domain.notification.dto.request.FcmTokenRequest;
import com.ssafy.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.ssafy.domain.notification.entity.*;
import com.ssafy.domain.notification.repository.FcmTokenRepository;
import com.ssafy.domain.notification.repository.NotificationRepository;
import com.ssafy.domain.notification.repository.NotificationSettingRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Long userId;
    private Notification notification1;
    private Notification notification2;
    private Notification notification3;
    private NotificationSetting setting1;
    private FcmToken fcmToken;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        user = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스트유저")
                .name("테스트")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        userId = user.getId();

        // 2. Notification 생성
        notification1 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.SCHEDULE)
                .title("스터디 일정 알림")
                .content("알고리즘 스터디가 1시간 후에 시작됩니다.")
                .referenceType("study_session")
                .isRead(false)
                .build());

        notification2 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.CHAT)
                .title("새 메시지")
                .content("홍길동: 안녕하세요!")
                .referenceType("channel")
                .isRead(false)
                .build());

        notification3 = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(NotificationType.ATTENDANCE)
                .title("출석 체크 시작")
                .content("출석 체크가 시작되었습니다.")
                .referenceType("study_session")
                .isRead(true)
                .build());

        notificationRepository.flush();

        // 3. NotificationSetting 생성
        setting1 = notificationSettingRepository.save(NotificationSetting.builder()
                .userId(userId)
                .notificationType("CHAT")
                .isEnabled(true)
                .build());
        notificationSettingRepository.flush();

        // 4. FcmToken 생성
        fcmToken = fcmTokenRepository.save(FcmToken.builder()
                .userId(userId)
                .token("fcm_token_android_001")
                .deviceType(DeviceType.ANDROID)
                .isActive(true)
                .build());
        fcmTokenRepository.flush();
    }

    // ============================================================
    // 알림 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("알림 목록 조회 성공")
    void getNotifications_Success() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    @DisplayName("알림 목록 조회 - 타입 필터")
    void getNotifications_WithTypeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header("User-Id", userId)
                        .param("type", "CHAT"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("CHAT"));
    }

    @Test
    @DisplayName("알림 목록 조회 - 페이징")
    void getNotifications_WithPaging() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header("User-Id", userId)
                        .param("page", "0")
                        .param("size", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    // ============================================================
    // 읽지 않은 알림 수 조회 테스트
    // ============================================================

    @Test
    @DisplayName("읽지 않은 알림 수 조회 성공")
    void getUnreadCount_Success() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(2))
                .andExpect(jsonPath("$.data.byType.CHAT").value(1))
                .andExpect(jsonPath("$.data.byType.SCHEDULE").value(1))
                .andExpect(jsonPath("$.data.byType.ATTENDANCE").value(0));
    }

    // ============================================================
    // 알림 읽음 처리 테스트
    // ============================================================

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_Success() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", notification1.getId())
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("알림을 읽음 처리했습니다."));

        entityManager.flush();
        entityManager.clear();

        // 읽지 않은 알림 수 확인
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("User-Id", userId))
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 404 반환")
    void markAsRead_NotFound() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", 999L)
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 알림 읽음 처리 시 404 반환")
    void markAsRead_NotOwner() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser")
                .email("other@test.com")
                .nickname("다른유저")
                .name("다른")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", notification1.getId())
                        .header("User-Id", otherUser.getId()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // 전체 읽음 처리 테스트
    // ============================================================

    @Test
    @DisplayName("전체 읽음 처리 성공")
    void markAllAsRead_Success() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readCount").value(2));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("User-Id", userId))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    // ============================================================
    // 알림 설정 조회 테스트
    // ============================================================

    @Test
    @DisplayName("알림 설정 조회 성공")
    void getSettings_Success() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/settings")
                        .header("User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settings.length()").value(NotificationType.values().length));
    }

    // ============================================================
    // 알림 설정 수정 테스트
    // ============================================================

    @Test
    @DisplayName("알림 설정 수정 성공")
    void updateSettings_Success() throws Exception {
        NotificationSettingUpdateRequest request = NotificationSettingUpdateRequest.builder()
                .settings(List.of(
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("CHAT")
                                .isEnabled(false)
                                .build(),
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("QUIZ")
                                .isEnabled(true)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .header("User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("알림 설정이 저장되었습니다."));
    }

    @Test
    @DisplayName("알림 설정 수정 - 유효하지 않은 타입")
    void updateSettings_InvalidType() throws Exception {
        NotificationSettingUpdateRequest request = NotificationSettingUpdateRequest.builder()
                .settings(List.of(
                        NotificationSettingUpdateRequest.SettingItem.builder()
                                .type("INVALID_TYPE")
                                .isEnabled(true)
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .header("User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // FCM 토큰 등록 테스트
    // ============================================================

    @Test
    @DisplayName("FCM 토큰 등록 성공")
    void registerFcmToken_Success() throws Exception {
        FcmTokenRequest request = FcmTokenRequest.builder()
                .token("new_fcm_token_001")
                .deviceType(DeviceType.IOS)
                .build();

        mockMvc.perform(post("/api/v1/notifications/fcm-token")
                        .header("User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("FCM 토큰이 등록되었습니다."));
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 유효성 검사 실패")
    void registerFcmToken_ValidationFail() throws Exception {
        FcmTokenRequest request = FcmTokenRequest.builder()
                .token("")
                .deviceType(DeviceType.ANDROID)
                .build();

        mockMvc.perform(post("/api/v1/notifications/fcm-token")
                        .header("User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // FCM 토큰 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("FCM 토큰 삭제 성공")
    void deleteFcmToken_Success() throws Exception {
        FcmTokenDeleteRequest request = FcmTokenDeleteRequest.builder()
                .token("fcm_token_android_001")
                .build();

        mockMvc.perform(delete("/api/v1/notifications/fcm-token")
                        .header("User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("FCM 토큰이 삭제되었습니다."));
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 존재하지 않는 토큰")
    void deleteFcmToken_NotFound() throws Exception {
        FcmTokenDeleteRequest request = FcmTokenDeleteRequest.builder()
                .token("nonexistent_token")
                .build();

        mockMvc.perform(delete("/api/v1/notifications/fcm-token")
                        .header("User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 다른 사용자의 토큰")
    void deleteFcmToken_NotOwner() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser2")
                .email("other2@test.com")
                .nickname("다른유저2")
                .name("다른2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        FcmTokenDeleteRequest request = FcmTokenDeleteRequest.builder()
                .token("fcm_token_android_001")
                .build();

        mockMvc.perform(delete("/api/v1/notifications/fcm-token")
                        .header("User-Id", otherUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}