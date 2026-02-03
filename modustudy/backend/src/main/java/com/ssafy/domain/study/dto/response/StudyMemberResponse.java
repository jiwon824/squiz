package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.MemberRole;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyMemberResponse {

    private Long memberId;          // study_member PK
    private Long studyId;
    private Long userId;
    private String userName;        // User.name
    private String userNickname;    // User.nickname
    private String userEmail;       // User.email
    private String profileImage;    // User.profileImage
    private MemberRole role;        // LEADER, MEMBER
    private MemberStatus status;    // APPROVED, WITHDRAWN, KICKED
    private Boolean isProbation;    // 수습 여부
    private LocalDateTime joinedAt; // 가입일

    /**
     * Entity -> DTO 변환
     */
    public static StudyMemberResponse from(StudyMember member, User user) {
        return StudyMemberResponse.builder()
                .memberId(member.getId())
                .studyId(member.getStudyId())
                .userId(member.getUserId())
                .userName(user.getName())
                .userNickname(user.getNickname())
                .userEmail(user.getEmail())
                .profileImage(user.getProfileImage())
                .role(member.getRole())
                .status(member.getStatus())
                .isProbation(member.getIsProbation())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}