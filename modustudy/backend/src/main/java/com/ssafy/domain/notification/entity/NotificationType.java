package com.ssafy.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    CHAT("채팅 알림"),
    SCHEDULE("일정 알림"),
    ATTENDANCE("출석 알림"),
    STUDY_UPDATE("스터디 업데이트"),
    STUDY_APPLICATION("스터디 신청 알림"),
    QUIZ("퀴즈 알림"),
    SYSTEM("시스템 알림");

    private final String displayName;
}