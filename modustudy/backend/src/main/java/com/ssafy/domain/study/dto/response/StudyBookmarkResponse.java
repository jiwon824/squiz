package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyBookmark;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyBookmarkResponse {

    private Long id;
    private Long userId;
    private Long studyId;
    private LocalDateTime createdAt;

    // 스터디 정보 (목록 조회 시)
    private String studyName;
    private String studyTopic;
    private String studyDescription;
    private String studyStatus;
    private String meetingType;
    private Integer maxMembers;
    private String difficulty;

    // 북마크 통계
    private Long bookmarkCount;

    // 북마크 여부 (스터디 목록에서 사용)
    private Boolean isBookmarked;

    /**
     * 북마크만 (토글 응답용)
     */
    public static StudyBookmarkResponse from(StudyBookmark bookmark) {
        return StudyBookmarkResponse.builder()
                .id(bookmark.getId())
                .userId(bookmark.getUserId())
                .studyId(bookmark.getStudyId())
                .createdAt(bookmark.getCreatedAt())
                .isBookmarked(true)
                .build();
    }

    /**
     * 북마크 + 스터디 정보 (목록 조회용)
     */
    public static StudyBookmarkResponse from(StudyBookmark bookmark, Study study) {
        return StudyBookmarkResponse.builder()
                .id(bookmark.getId())
                .userId(bookmark.getUserId())
                .studyId(bookmark.getStudyId())
                .createdAt(bookmark.getCreatedAt())
                .studyName(study.getName())
                .studyTopic(study.getTopic())
                .studyDescription(study.getDescription())
                .studyStatus(study.getStatus().name())
                .meetingType(study.getMeetingType().name())
                .maxMembers(study.getMaxMembers())
                .difficulty(study.getDifficulty() != null ? study.getDifficulty().name() : null)
                .isBookmarked(true)
                .build();
    }

    /**
     * 북마크 + 스터디 정보 + 북마크 개수
     */
    public static StudyBookmarkResponse from(StudyBookmark bookmark, Study study, Long bookmarkCount) {
        return StudyBookmarkResponse.builder()
                .id(bookmark.getId())
                .userId(bookmark.getUserId())
                .studyId(bookmark.getStudyId())
                .createdAt(bookmark.getCreatedAt())
                .studyName(study.getName())
                .studyTopic(study.getTopic())
                .studyDescription(study.getDescription())
                .studyStatus(study.getStatus().name())
                .meetingType(study.getMeetingType().name())
                .maxMembers(study.getMaxMembers())
                .difficulty(study.getDifficulty() != null ? study.getDifficulty().name() : null)
                .bookmarkCount(bookmarkCount)
                .isBookmarked(true)
                .build();
    }

    /**
     * 북마크 취소 응답
     */
    public static StudyBookmarkResponse unbookmarked(Long studyId) {
        return StudyBookmarkResponse.builder()
                .studyId(studyId)
                .isBookmarked(false)
                .build();
    }
}