package com.ssafy.domain.dm.mapper;

import com.ssafy.domain.dm.entity.DmConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DmConversationMapper {

    /**
     * 대화방 생성
     */
    void insert(DmConversation conversation);

    /**
     * ID로 대화방 조회
     */
    DmConversation findById(@Param("id") Long id);

    /**
     * 두 사용자 간의 대화방 조회
     */
    DmConversation findByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    /**
     * 사용자의 대화방 목록 조회 (삭제되지 않은 것만)
     */
    List<DmConversation> findByUserId(@Param("userId") Long userId);

    /**
     * 마지막 메시지 시간 업데이트
     */
    void updateLastMessageAt(@Param("id") Long id, @Param("lastMessageAt") LocalDateTime lastMessageAt);

    /**
     * 마지막 읽은 메시지 ID 업데이트 (user1)
     */
    void updateUser1LastReadMessageId(@Param("id") Long id, @Param("messageId") Long messageId);

    /**
     * 마지막 읽은 메시지 ID 업데이트 (user2)
     */
    void updateUser2LastReadMessageId(@Param("id") Long id, @Param("messageId") Long messageId);

    /**
     * 대화방 삭제 처리 (user1)
     */
    void markUser1Deleted(@Param("id") Long id);

    /**
     * 대화방 삭제 처리 (user2)
     */
    void markUser2Deleted(@Param("id") Long id);

    /**
     * 대화방 복원 (user1)
     */
    void restoreUser1(@Param("id") Long id);

    /**
     * 대화방 복원 (user2)
     */
    void restoreUser2(@Param("id") Long id);

    /**
     * 사용자가 참여자인지 확인
     */
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
