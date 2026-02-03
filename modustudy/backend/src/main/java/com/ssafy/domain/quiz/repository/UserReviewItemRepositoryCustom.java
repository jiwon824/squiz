package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.WrongAnswerSortType;

import java.util.List;

/**
 * UserReviewItem 커스텀 레포지토리 인터페이스
 * QueryDSL을 이용한 동적 쿼리를 지원합니다.
 */
public interface UserReviewItemRepositoryCustom {

    /**
     * 오답 노트 조회 (동적 정렬)
     *
     * @param userId   사용자 ID
     * @param sortType 정렬 방식
     * @return 오답 항목 목록 (lapses > 0)
     */
    List<UserReviewItem> findWrongAnswers(Long userId, WrongAnswerSortType sortType);
}
