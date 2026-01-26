package com.ssafy.domain.quiz.entity.enums;

/**
 * 섹션 시도 상태.
 *
 * 사용자의 퀴즈 섹션 시도 진행 상태를 나타낸다.
 */
public enum AttemptStatus {

    /**
     * 진행 중 - 사용자가 문제를 풀고 있는 상태.
     * 임시 저장된 답안이 있을 수 있음.
     */
    IN_PROGRESS,

    /**
     * 완료 - 사용자가 답안을 제출하고 채점이 완료된 상태.
     * 더 이상 수정 불가.
     */
    SUBMITTED,

    /**
     * 포기 - 사용자가 시도를 중도 포기한 상태.
     * 점수 미반영.
     */
    ABANDONED
}
