package com.ssafy.domain.gamification.entity;

/**
 * 배지 카테고리.
 */
 public enum BadgeCategory {
    /** 활동 뱃지 - 첫 활동, 첫 게시글 작성 등 */
    ACTIVITY,

    /** 연속 활동 뱃지 - 7일 연속 활동, 30일 연속 활동 등 */
    STREAK,

    /** 스터디 뱃지 - 스터디 완주, 스터디 리더 경험 등 */
    STUDY,

    /** 출석 뱃지 - 개근왕 등 */
    ATTENDANCE,

    /** 참여 뱃지 - 퀴즈 대회 참가 등 */
    PARTICIPATION,

    /** 퀴즈 뱃지 - 퀴즈왕 TOP3 등 */
    QUIZ,

    /** 마스터 뱃지 - 퀴즈 코스 완료 시 부여 */
    MASTER,

    /** 특별 뱃지 - 이벤트, 특별 조건 달성 등 */
    SPECIAL
}
