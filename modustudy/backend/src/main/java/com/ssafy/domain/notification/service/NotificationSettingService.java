package com.ssafy.domain.notification.service;

import com.ssafy.common.exception.NotificationException;
import com.ssafy.domain.notification.dto.request.NotificationSettingUpdateRequest;
import com.ssafy.domain.notification.dto.response.NotificationSettingListResponse;
import com.ssafy.domain.notification.dto.response.NotificationSettingResponse;
import com.ssafy.domain.notification.entity.NotificationSetting;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

    /**
     * 알림 설정 조회
     * - 설정이 없는 타입은 기본값(enabled=true)으로 반환
     */
    public NotificationSettingListResponse getSettings(Long userId) {
// 기존 설정 조회
        List<NotificationSetting> existingSettings = notificationSettingRepository.findByUserId(userId);
        Map<String, NotificationSetting> settingMap = existingSettings.stream()
                .collect(Collectors.toMap(NotificationSetting::getNotificationType, s -> s));

        // 모든 타입에 대해 설정 반환 (없으면 기본값)
        List<NotificationSettingResponse> settings = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            NotificationSetting setting = settingMap.get(type.name());
            if (setting != null) {
                settings.add(NotificationSettingResponse.from(setting));
            } else {
                // 기본값: 활성화
                settings.add(NotificationSettingResponse.of(type, true));
            }
        }

        return NotificationSettingListResponse.of(settings);
    }

    /**
     * 알림 설정 수정
     */
    @Transactional
    public void updateSettings(Long userId, NotificationSettingUpdateRequest request) {
        for (NotificationSettingUpdateRequest.SettingItem item : request.getSettings()) {
            // 유효한 알림 타입인지 검증
            try {
                NotificationType.valueOf(item.getType());
            } catch (IllegalArgumentException e) {
                throw new NotificationException.InvalidNotificationTypeException(item.getType());
            }

            // 기존 설정 조회 또는 새로 생성
            NotificationSetting setting = notificationSettingRepository
                    .findByUserIdAndNotificationType(userId, item.getType())
                    .orElse(null);

            if (setting != null) {
                // 기존 설정 업데이트
                setting.updateEnabled(item.getIsEnabled());
            } else {
                // 새로 생성
                NotificationSetting newSetting = NotificationSetting.builder()
                        .userId(userId)
                        .notificationType(item.getType())
                        .isEnabled(item.getIsEnabled())
                        .build();
                notificationSettingRepository.save(newSetting);
            }
        }

}

    /**
     * 특정 알림 타입 활성화 여부 확인
     */
    public boolean isNotificationEnabled(Long userId, NotificationType type) {
        return notificationSettingRepository
                .findByUserIdAndNotificationType(userId, type.name())
                .map(NotificationSetting::getIsEnabled)
                .orElse(true);  // 설정이 없으면 기본값 true
    }

    /**
     * 사용자 기본 알림 설정 초기화 (회원가입 시 호출)
     */
    @Transactional
    public void initializeDefaultSettings(Long userId) {
        for (NotificationType type : NotificationType.values()) {
            NotificationSetting setting = NotificationSetting.builder()
                    .userId(userId)
                    .notificationType(type.name())
                    .isEnabled(true)
                    .build();
            notificationSettingRepository.save(setting);
        }

}
}
