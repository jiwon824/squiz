package com.ssafy.domain.material.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 업로더 정보 DTO
 */
@Getter
@Builder
public class UploaderInfo {

    private Long id;
    private String nickname;
    private String profileImage;

    public static UploaderInfo of(Long id, String nickname, String profileImage) {
        return UploaderInfo.builder()
                .id(id)
                .nickname(nickname)
                .profileImage(profileImage)
                .build();
    }
}