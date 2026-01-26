package com.ssafy.domain.study.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.domain.study.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import java.util.*;

import static com.ssafy.domain.study.entity.QStudy.study;

/**
 * QueryDSL을 이용한 동적 쿼리 구현
 */
@Repository
@RequiredArgsConstructor
public class StudyRepositoryImpl implements StudyRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Page<Study> searchStudies(StudySearchCondition condition, Pageable pageable) {

        // 검색 쿼리
        List<Study> content = queryFactory
                .selectFrom(study)
                .leftJoin(study.topic).fetchJoin()      // Topic N+1 방지
                .leftJoin(study.format).fetchJoin()     // Format N+1 방지
                .where(
                        isNotDraft(),
                        isPublicEq(condition.getIsPublic()),
                        keywordContains(condition.getKeyword()),
                        topicIdEq(condition.getTopicId()),
                        parentTopicIdEq(condition.getParentTopicId()),
                        formatIdEq(condition.getFormatId()),
                        studyTypeEq(condition.getStudyType()),
                        meetingTypeEq(condition.getMeetingType()),
                        statusEq(condition.getStatus()),
                        regionIdEq(condition.getRegionId()),
                        difficultyEq(condition.getDifficulty()),
                        targetOrgTypeEq(condition.getTargetOrgType()),
                        scheduleDaysContains(condition.getScheduleDays()),
                        maxMembersLessThan(condition.getMaxMembersLessThan())
                )
                .orderBy(study.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리 (성능 최적화)
        JPAQuery<Long> countQuery = queryFactory
                .select(study.count())
                .from(study)
                .where(
                        isNotDraft(),
                        isPublicEq(condition.getIsPublic()),
                        keywordContains(condition.getKeyword()),
                        topicIdEq(condition.getTopicId()),
                        parentTopicIdEq(condition.getParentTopicId()),
                        formatIdEq(condition.getFormatId()),
                        studyTypeEq(condition.getStudyType()),
                        meetingTypeEq(condition.getMeetingType()),
                        statusEq(condition.getStatus()),
                        regionIdEq(condition.getRegionId()),
                        difficultyEq(condition.getDifficulty()),
                        targetOrgTypeEq(condition.getTargetOrgType()),
                        scheduleDaysContains(condition.getScheduleDays()),
                        maxMembersLessThan(condition.getMaxMembersLessThan())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // ===== 동적 조건 메서드 =====

    /**
     * DRAFT 상태 제외
     */
    private BooleanExpression isNotDraft() {
        return study.status.ne(Status.DRAFT);
    }

    /**
     * 공개 여부 필터
     */
    private BooleanExpression isPublicEq(Boolean isPublic) {
        return isPublic != null ? study.isPublic.eq(isPublic) : null;
    }

    /**
     * 키워드 검색 (이름 + 설명 + 주제명)
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return study.name.containsIgnoreCase(keyword)
                .or(study.description.containsIgnoreCase(keyword))
                .or(study.topic.name.containsIgnoreCase(keyword));  // 주제명도 검색
    }

    /**
     * 주제 ID 필터 (소분류)
     */
    private BooleanExpression topicIdEq(Long topicId) {
        return topicId != null ? study.topic.id.eq(topicId) : null;
    }

    /**
     * 대분류 ID 필터 (해당 대분류의 모든 소분류 포함)
     */
    private BooleanExpression parentTopicIdEq(Long parentTopicId) {
        if (parentTopicId == null) {
            return null;
        }
        // 대분류 자체이거나, parent가 해당 대분류인 경우
        return study.topic.id.eq(parentTopicId)
                .or(study.topic.parent.id.eq(parentTopicId));
    }

    /**
     * 형식 ID 필터
     */
    private BooleanExpression formatIdEq(Long formatId) {
        return formatId != null ? study.format.id.eq(formatId) : null;
    }

    /**
     * 스터디 타입 필터 (계획/번개)
     */
    private BooleanExpression studyTypeEq(StudyType studyType) {
        return studyType != null ? study.studyType.eq(studyType) : null;
    }

    /**
     * 진행 방식 필터 (온라인/오프라인/혼합)
     */
    private BooleanExpression meetingTypeEq(MeetingType meetingType) {
        return meetingType != null ? study.meetingType.eq(meetingType) : null;
    }

    /**
     * 상태 필터
     */
    private BooleanExpression statusEq(Status status) {
        return status != null ? study.status.eq(status) : null;
    }

    /**
     * 지역 ID 필터
     */
    private BooleanExpression regionIdEq(Long regionId) {
        return regionId != null ? study.regionId.eq(regionId) : null;
    }

    /**
     * 난이도 필터
     */
    private BooleanExpression difficultyEq(Difficulty difficulty) {
        return difficulty != null ? study.difficulty.eq(difficulty) : null;
    }

    /**
     * 대상 소속 타입 필터
     */
    private BooleanExpression targetOrgTypeEq(String targetOrgType) {
        return StringUtils.hasText(targetOrgType) ? study.targetOrgType.eq(targetOrgType) : null;
    }

    /**
     * 요일 검색 (scheduleDays에 포함된 경우)
     */
    private BooleanExpression scheduleDaysContains(String scheduleDays) {
        return StringUtils.hasText(scheduleDays) ? study.scheduleDays.contains(scheduleDays) : null;
    }

    /**
     * 최대 인원 이하 필터
     */
    private BooleanExpression maxMembersLessThan(Integer maxMembers) {
        return maxMembers != null ? study.maxMembers.loe(maxMembers) : null;
    }
}