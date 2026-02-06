package com.ssafy.domain.gamification.config;

import java.util.Map;

/**
 * 경험치 시스템 설정
 * 활동별 경험치, 레벨별 필요 경험치 등 정의
 */
 public final class ExperienceConfig {

    private ExperienceConfig() {}

    // ========== 활동별 기본 경험치 ==========

    /** 스터디 출석 */
    public static final int STUDY_ATTENDANCE = 10;

    /** 퀴즈 정답 */
    public static final int QUIZ_CORRECT = 5;

    /** 퀴즈 오답 */
    public static final int QUIZ_WRONG = 2;

    /** 자료 업로드 */
    public static final int MATERIAL_UPLOAD = 15;

    /** 회고록 작성 */
    public static final int RETROSPECTIVE_WRITE = 20;

    // ========== 첫 활동 보너스 (1회만 지급) ==========

    /** 첫 스터디 생성 */
    public static final int FIRST_STUDY_CREATE_BONUS = 50;

    /** 첫 스터디 가입 */
    public static final int FIRST_STUDY_JOIN_BONUS = 50;

    /** 첫 친구 채팅 (친구별 1회) */
    public static final int FIRST_FRIEND_CHAT_BONUS = 10;

    /** 첫 스터디장 리뷰 작성 */
    public static final int FIRST_LEADER_REVIEW_BONUS = 30;

    // ========== 채팅 (현재 비활성화됨) ==========

    /** 채팅 참여 (1회당) - 현재 비활성화 */
    public static final int CHAT_MESSAGE = 1;

    /** 채팅 일일 최대 경험치 - 현재 비활성화 */
    public static final int CHAT_DAILY_MAX = 10;

    // ========== 연속 활동 보너스 ==========

    /** 3일 연속 활동 보너스 */
    public static final int STREAK_3_DAYS = 10;

    /** 7일 연속 활동 보너스 */
    public static final int STREAK_7_DAYS = 30;

    /** 14일 연속 활동 보너스 */
    public static final int STREAK_14_DAYS = 50;

    /** 30일 연속 활동 보너스 */
    public static final int STREAK_30_DAYS = 100;

    // ========== 레벨 시스템 ==========

    /**
     * 레벨별 필요 경험치 (해당 레벨에서 다음 레벨로 가기 위해 필요한 XP)
     * Level 1 → 2: 100 XP
     * Level 2 → 3: 150 XP
     * Level 3 → 4: 200 XP
     * Level 4 → 5: 300 XP
     * Level 5 → 6: 500 XP
     * Level 6: 최대 레벨
     */
    public static final Map<Integer, Integer> LEVEL_REQUIRED_XP = Map.of(
            1, 100,
            2, 150,
            3, 200,
            4, 300,
            5, 500
    );

    /** 최대 레벨 */
    public static final int MAX_LEVEL = 6;

    /**
     * 레벨별 이름
     */
    public static final Map<Integer, String> LEVEL_NAMES = Map.of(
            1, "새싹",
            2, "학습자",
            3, "열공러",
            4, "성실러",
            5, "마스터",
            6, "그랜드마스터"
    );

    /**
     * 현재 레벨에서 다음 레벨로 가기 위해 필요한 경험치 반환
     * @param level 현재 레벨
     * @return 필요 경험치 (최대 레벨이면 0)
     */
    public static int getRequiredXpForNextLevel(int level) {
        return LEVEL_REQUIRED_XP.getOrDefault(level, 0);
    }

    /**
     * 레벨 이름 반환
     * @param level 레벨
     * @return 레벨 이름
     */
    public static String getLevelName(int level) {
        return LEVEL_NAMES.getOrDefault(level, "새싹");
    }

    /**
     * 연속 활동일에 따른 보너스 경험치 계산
     * @param streak 연속 활동일
     * @return 보너스 경험치 (해당 마일스톤에 도달했을 때만)
     */
    public static int getStreakBonus(int streak) {
        if (streak == 30) return STREAK_30_DAYS;
        if (streak == 14) return STREAK_14_DAYS;
        if (streak == 7) return STREAK_7_DAYS;
        if (streak == 3) return STREAK_3_DAYS;
        return 0;
    }
}
