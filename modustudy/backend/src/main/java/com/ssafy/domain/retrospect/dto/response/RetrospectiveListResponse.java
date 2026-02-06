package com.ssafy.domain.retrospect.dto.response;

import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import com.ssafy.domain.study.entity.StudySession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RetrospectiveListResponse {

    private Long id;
    private String title;
    private RetrospectiveType retrospectiveType;
    private SessionInfo session;
    private Integer itemCount;
    private Integer participantCount;
    private Boolean hasMyItem;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class SessionInfo {
        private Long id;
        private Integer sessionNumber;

        public static SessionInfo from(StudySession session) {
            if (session == null) {
                return null;
            }
            return SessionInfo.builder()
                    .id(session.getId())
                    .sessionNumber(session.getSessionNumber())
                    .build();
        }
    }

    public static RetrospectiveListResponse of(
            Retrospective retrospective,
            StudySession session,
            Integer itemCount,
            Integer participantCount,
            Boolean hasMyItem) {

        return RetrospectiveListResponse.builder()
                .id(retrospective.getId())
                .title(retrospective.getTitle())
                .retrospectiveType(retrospective.getRetrospectiveType())
                .session(SessionInfo.from(session))
                .itemCount(itemCount)
                .participantCount(participantCount)
                .hasMyItem(hasMyItem)
                .createdAt(retrospective.getCreatedAt())
                .build();
    }
}
