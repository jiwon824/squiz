package com.ssafy.domain.board.dto.response;

import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardRecruitingStudyResponse {
    private Long id;
    private String name;
    private String topicName;
    private StudyType studyType;
    private MeetingType meetingType;
    private Integer maxMembers;
    private int currentMembers;
    private Status status;

    public static BoardRecruitingStudyResponse from(Study study, int currentMembers) {
        return BoardRecruitingStudyResponse.builder()
                .id(study.getId())
                .name(study.getName())
                .topicName(study.getTopicName())
                .studyType(study.getStudyType())
                .meetingType(study.getMeetingType())
                .maxMembers(study.getMaxMembers())
                .currentMembers(currentMembers)
                .status(study.getStatus())
                .build();
    }
}
