package com.ssafy.domain.quiz.entity;

/**
 * 오답 노트 정렬 방식
 */
public enum WrongAnswerSortType {
    /**
     * 많이 틀린 순 (기본값)
     * - lapses DESC, nextReviewAt ASC
     */
    MOST_WRONG,

    /**
     * FSRS 복습 우선순위
     * - nextReviewAt ASC, stability ASC
     * - 잊어버릴 가능성이 높거나 기억 안정성이 낮은 항목 우선
     */
    FSRS_RECOMMENDED,

    /**
     * 최신순 (가장 최근에 틀린 문제 우선)
     * - lastReviewedAt DESC
     */
    LATEST
}
