package com.ssafy.domain.dm.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DM 대화방 엔티티
 * - 두 사용자 간의 대화를 그룹화
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmConversation {

    private Long id;

    /**
     * 참여자 1 (ID가 작은 쪽)
     */
    private Long user1Id;

    /**
     * 참여자 2 (ID가 큰 쪽)
     */
    private Long user2Id;

    /**
     * user1의 마지막 읽은 메시지 ID
     */
    private Long user1LastReadMessageId;

    /**
     * user2의 마지막 읽은 메시지 ID
     */
    private Long user2LastReadMessageId;

    /**
     * user1이 대화를 삭제했는지
     */
    @Builder.Default
    private Boolean user1Deleted = false;

    /**
     * user2가 대화를 삭제했는지
     */
    @Builder.Default
    private Boolean user2Deleted = false;

    /**
     * 마지막 메시지 시간 (정렬용)
     */
    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN된 사용자 정보
    private String user1Nickname;
    private String user1ProfileImage;
    private Boolean user1IsOnline;
    private String user2Nickname;
    private String user2ProfileImage;
    private Boolean user2IsOnline;

    // 마지막 메시지 내용 (목록 조회용)
    private String lastMessageContent;
    private Long lastMessageSenderId;

    /**
     * 내가 user1인지 확인
     */
    public boolean isUser1(Long userId) {
        return user1Id.equals(userId);
    }

    /**
     * 상대방 ID 반환
     */
    public Long getPartnerId(Long myId) {
        return user1Id.equals(myId) ? user2Id : user1Id;
    }

    /**
     * 상대방 닉네임 반환
     */
    public String getPartnerNickname(Long myId) {
        return user1Id.equals(myId) ? user2Nickname : user1Nickname;
    }

    /**
     * 상대방 프로필 이미지 반환
     */
    public String getPartnerProfileImage(Long myId) {
        return user1Id.equals(myId) ? user2ProfileImage : user1ProfileImage;
    }

    /**
     * 상대방 온라인 상태 반환
     */
    public Boolean getPartnerIsOnline(Long myId) {
        return user1Id.equals(myId) ? user2IsOnline : user1IsOnline;
    }

    /**
     * 마지막 읽은 메시지 ID 조회
     */
    public Long getLastReadMessageId(Long userId) {
        return isUser1(userId) ? user1LastReadMessageId : user2LastReadMessageId;
    }

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted(Long userId) {
        return isUser1(userId) ? Boolean.TRUE.equals(user1Deleted) : Boolean.TRUE.equals(user2Deleted);
    }
}
