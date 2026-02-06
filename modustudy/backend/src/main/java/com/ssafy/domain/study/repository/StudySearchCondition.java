package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Difficulty;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.StudyType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySearchCondition {

    private String keyword;           // 검색어 (스터디명, 설명, 주제명)

    // ========== 카테고리: ID 기반 ==========
    private Long topicId;             // 주제 ID (소분류 ID)
    private Long parentTopicId;       // 대분류 ID (대분류로 필터링 시 하위 소분류 모두 포함)
    private Long formatId;            // 형식 ID
    // =====================================

    private StudyType studyType;      // 스터디 타입 (계획/번개)
    private Status status;            // 스터디 상태
    private MeetingType meetingType;  // 진행 방식 (온라인/오프라인/혼합)
    private Long regionId;            // 지역
    private Boolean isPublic;         // 공개 여부
    private Difficulty difficulty;    // 난이도
    private String targetOrgType;     // 대상 소속 타입 (SSAFY, NBC 등)
    private String scheduleDays;      // 요일 필터 (MON, TUE 등)
    private Integer maxMembersLessThan; // 최대 인원 이하 필터
}
