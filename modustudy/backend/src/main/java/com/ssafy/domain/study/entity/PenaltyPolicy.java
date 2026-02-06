package com.ssafy.domain.study.entity;

public enum PenaltyPolicy {
    STRICT("엄격"), // 한 번이라도 결석 시 퇴출
    NORMAL("보통"), // 출석률 50% 미만 퇴출
    LENIENT("관대"), // 출석률 30% 미만 퇴출
    RATIO("비율"), // 커스텀 비율
    NONE("없음");  // 패널티 없음

    private final String description;

    PenaltyPolicy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
