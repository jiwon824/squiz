package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.WrongAnswerSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * UserReviewItem 커스텀 레포지토리 인터페이스
 * QueryDSL을 이용한 동적 쿼리를 지원합니다.
 */
 public interface UserReviewItemRepositoryCustom {

    /**
     * 오답 노트 조회 (동적 정렬 + 페이징)
     *
     * @param userId   사용자 ID
     * @param sortType 정렬 방식
     * @param pageable 페이징 정보
     * @return 오답 항목 페이지 (lapses > 0)
     */
    Page<UserReviewItem> findWrongAnswers(Long userId, WrongAnswerSortType sortType, Pageable pageable);
}
