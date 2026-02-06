package com.ssafy.domain.friend.mapper;

import com.ssafy.domain.friend.entity.UserBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBlockMapper {

    /**
     * 차단 저장
     */
    void insert(UserBlock userBlock);

    /**
     * 차단 삭제
     */
    void delete(@Param("id") Long id);

    /**
     * 차단 관계 조회
     */
    UserBlock findByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /**
     * 차단 여부 확인
     */
    boolean existsByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /**
     * 양방향 차단 확인 (둘 중 하나라도 차단했는지)
     */
    boolean existsAnyBlock(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 내가 차단한 사용자 목록
     */
    List<UserBlock> findByBlockerId(@Param("blockerId") Long blockerId);

    /**
     * 나를 차단한 사용자 ID 목록
     */
    List<Long> findBlockerIdsByBlockedId(@Param("blockedId") Long blockedId);
}
