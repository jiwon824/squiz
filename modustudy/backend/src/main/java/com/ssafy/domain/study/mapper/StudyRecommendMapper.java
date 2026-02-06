package com.ssafy.domain.study.mapper;

import com.ssafy.domain.study.dto.response.StudyRecommendDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyRecommendMapper {

    /**
     * 사용자 맞춤 스터디 참여 추천
     * - 모집중(RECRUITING) + 잔여석 있음
     * - 이미 참여중/지원중인 스터디 제외
     * - 기술스택, 일정, 토픽 계층(연관 기술) 매칭 점수 기반 정렬
     */
    List<StudyRecommendDto> findRecommendedStudies(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    /**
     * 특정 토픽 기반 추천 (토픽 필터 추가)
     */
    List<StudyRecommendDto> findRecommendedStudiesByTopic(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId,
            @Param("limit") int limit
    );
}
