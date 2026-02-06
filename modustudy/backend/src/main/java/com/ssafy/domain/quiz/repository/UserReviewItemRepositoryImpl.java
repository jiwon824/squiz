package com.ssafy.domain.quiz.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.domain.quiz.entity.QUserReviewItem;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.WrongAnswerSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * QueryDSL을 이용한 동적 쿼리 구현
 */
 @Repository
 @RequiredArgsConstructor
 public class UserReviewItemRepositoryImpl implements UserReviewItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserReviewItem> findWrongAnswers(Long userId, WrongAnswerSortType sortType, Pageable pageable) {
        QUserReviewItem item = QUserReviewItem.userReviewItem;

        // 정렬 방식 결정
        OrderSpecifier<?>[] orderSpecifiers = getOrderSpecifiers(sortType, item);

        List<UserReviewItem> content = queryFactory
                .selectFrom(item)
                .where(
                        item.userId.eq(userId),
                        item.lapses.gt(0) // lapses > 0
                )
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        item.userId.eq(userId),
                        item.lapses.gt(0));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 정렬 방식에 따른 OrderSpecifier 배열 반환
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(WrongAnswerSortType sortType, QUserReviewItem item) {
        if (sortType == null) {
            sortType = WrongAnswerSortType.MOST_WRONG; // 기본값
        }

        return switch (sortType) {
            case MOST_WRONG ->
                // 많이 틀린 순: lapses 내림차순 (같으면 nextReviewAt 오름차순)
                new OrderSpecifier<?>[] {
                        item.lapses.desc(),
                        item.nextReviewAt.asc()
                };
            case FSRS_RECOMMENDED ->
                // FSRS 복습 우선순위: stability 오름차순 (낮을수록 불안정 → 복습 우선)
                // 같은 stability면 lapses가 많은 순
                new OrderSpecifier<?>[] {
                        item.stability.asc(),
                        item.lapses.desc()
                };
            case LATEST ->
                // 최신순: lastReviewedAt 내림차순 (가장 최근에 틀린 문제 우선)
                // lastReviewedAt이 null인 경우 createdAt 기준
                new OrderSpecifier<?>[] {
                        item.lastReviewedAt.desc().nullsLast(),
                        item.createdAt.desc()
                };
        };
    }
}
