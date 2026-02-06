package com.ssafy.domain.material.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import com.ssafy.domain.material.entity.QMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Material QueryDSL 구현체
 */
 @Repository
 @RequiredArgsConstructor
 public class MaterialRepositoryImpl implements MaterialRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QMaterial material = QMaterial.material;

    @Override
    public Page<Material> searchMaterials(Long studyId,
                                          Integer weekNumber,
                                          MaterialType materialType,
                                          String keyword,
                                          Pageable pageable) {

        List<Material> content = queryFactory
                .selectFrom(material)
                .where(
                        studyIdEq(studyId),
                        weekNumberEq(weekNumber),
                        materialTypeEq(materialType),
                        keywordContains(keyword)
                )
                .orderBy(material.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(material.count())
                .from(material)
                .where(
                        studyIdEq(studyId),
                        weekNumberEq(weekNumber),
                        materialTypeEq(materialType),
                        keywordContains(keyword)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 스터디 ID 조건
     */
    private BooleanExpression studyIdEq(Long studyId) {
        return studyId != null ? material.studyId.eq(studyId) : null;
    }

    /**
     * 주차 조건
     */
    private BooleanExpression weekNumberEq(Integer weekNumber) {
        return weekNumber != null ? material.weekNumber.eq(weekNumber) : null;
    }

    /**
     * 자료 타입 조건
     */
    private BooleanExpression materialTypeEq(MaterialType materialType) {
        return materialType != null ? material.materialType.eq(materialType) : null;
    }

    /**
     * 키워드 검색 조건 (제목 OR 설명)
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return material.title.containsIgnoreCase(keyword)
                .or(material.description.containsIgnoreCase(keyword));
    }
}
