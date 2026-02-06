package com.ssafy.domain.study.entity;

public enum Status {
    DRAFT("임시저장"),
    SCHEDULED("모집예정"),
    RECRUITING("모집중"),
    RECRUIT_CLOSED("모집완료/시작대기"),
    PENDING("확정대기"), // 인원 부족 시 임시 채널
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String description;

    Status(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
