package com.ssafy.domain.friend.mapper;

import com.ssafy.domain.friend.dto.response.UserSearchResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSearchMapper {

    /**
     * 닉네임으로 사용자 검색 (검색 허용 + 본인 제외)
     */
    List<UserSearchResponse> searchByNickname(
            @Param("keyword") String keyword,
            @Param("excludeId") Long excludeId,
            @Param("excludeBlockerIds") List<Long> excludeBlockerIds
    );
}
