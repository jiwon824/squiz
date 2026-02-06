package com.ssafy.domain.retrospect.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.domain.retrospect.entity.Category;
import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveItem;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class RetrospectiveDetailResponse {

    private Long id;
    private String title;
    private RetrospectiveType retrospectiveType;
    private SessionInfo session;
    private ItemsByCategory items;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class SessionInfo {
        private Long id;
        private Integer sessionNumber;
        private String title;

        public static SessionInfo from(StudySession session) {
            if (session == null) return null;
            return SessionInfo.builder()
                    .id(session.getId())
                    .sessionNumber(session.getSessionNumber())
                    .title(session.getTitle())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ItemsByCategory {
        @JsonProperty("KEEP")
        private List<ItemResponse> KEEP;

        @JsonProperty("PROBLEM")
        private List<ItemResponse> PROBLEM;

        @JsonProperty("TRY")
        private List<ItemResponse> TRY;

        public static ItemsByCategory from(Map<Category, List<ItemResponse>> itemsMap) {
            return ItemsByCategory.builder()
                    .KEEP(itemsMap.getOrDefault(Category.KEEP, List.of()))
                    .PROBLEM(itemsMap.getOrDefault(Category.PROBLEM, List.of()))
                    .TRY(itemsMap.getOrDefault(Category.TRY, List.of()))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private Long id;
        private UserInfo user;
        private String content;
        private LocalDateTime createdAt;

        public static ItemResponse of(RetrospectiveItem item, User user) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .user(UserInfo.from(user))
                    .content(item.getContent())
                    .createdAt(item.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UserInfo {
        private Long id;
        private String nickname;
        private String profileImage;

        public static UserInfo from(User user) {
            if (user == null) return null;
            return UserInfo.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .build();
        }
    }

    public static RetrospectiveDetailResponse of(
            Retrospective retrospective,
            StudySession session,
            Map<Category, List<ItemResponse>> itemsMap) {
        return RetrospectiveDetailResponse.builder()
                .id(retrospective.getId())
                .title(retrospective.getTitle())
                .retrospectiveType(retrospective.getRetrospectiveType())
                .session(SessionInfo.from(session))
                .items(ItemsByCategory.from(itemsMap))
                .createdAt(retrospective.getCreatedAt())
                .build();
    }
}
