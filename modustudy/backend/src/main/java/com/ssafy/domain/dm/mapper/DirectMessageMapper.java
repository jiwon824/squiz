package com.ssafy.domain.dm.mapper;

import com.ssafy.domain.dm.entity.DirectMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DirectMessageMapper {

    /**
     * 메시지 생성
     */
    void insert(DirectMessage message);

    /**
     * ID로 메시지 조회
     */
    DirectMessage findById(@Param("id") Long id);

    /**
     * 대화방의 메시지 목록 조회 (페이징)
     */
    List<DirectMessage> findByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 대화방의 최신 메시지 조회
     */
    DirectMessage findLatestByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 특정 메시지 이후의 메시지 개수 (안 읽은 메시지 수)
     */
    int countUnreadMessages(
            @Param("conversationId") Long conversationId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );

    /**
     * 메시지 삭제 처리 (soft delete)
     */
    void markDeleted(@Param("id") Long id);

    /**
     * 대화방의 총 메시지 개수
     */
    int countByConversationId(@Param("conversationId") Long conversationId);
}
