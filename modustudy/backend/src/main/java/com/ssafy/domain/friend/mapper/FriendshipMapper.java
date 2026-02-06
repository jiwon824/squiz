package com.ssafy.domain.friend.mapper;

import com.ssafy.domain.friend.entity.Friendship;
import com.ssafy.domain.friend.entity.FriendshipStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendshipMapper {

    /**
     * 친구 관계 저장
     */
    void insert(Friendship friendship);

    /**
     * 친구 관계 상태 업데이트
     */
    void updateStatus(@Param("id") Long id, @Param("status") FriendshipStatus status);

    /**
     * 친구 관계 삭제
     */
    void delete(@Param("id") Long id);

    /**
     * ID로 조회
     */
    Friendship findById(@Param("id") Long id);

    /**
     * 두 사용자 간의 친구 관계 조회 (방향 무관)
     */
    Friendship findByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 특정 사용자가 보낸 친구 요청 조회
     */
    Friendship findByRequesterAndAddressee(@Param("requesterId") Long requesterId, @Param("addresseeId") Long addresseeId);

    /**
     * 받은 친구 요청 목록 (PENDING 상태)
     */
    List<Friendship> findReceivedRequests(@Param("addresseeId") Long addresseeId);

    /**
     * 보낸 친구 요청 목록 (PENDING 상태)
     */
    List<Friendship> findSentRequests(@Param("requesterId") Long requesterId);

    /**
     * 친구 목록 조회 (ACCEPTED 상태, 사용자 정보 포함)
     */
    List<Friendship> findFriends(@Param("userId") Long userId);

    /**
     * 특정 친구 관계 조회 (본인이 포함된)
     */
    Friendship findByIdAndUserId(@Param("friendshipId") Long friendshipId, @Param("userId") Long userId);
}
