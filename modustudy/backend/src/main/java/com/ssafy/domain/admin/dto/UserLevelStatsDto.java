package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 레벨별 사용자 분포 DTO
 */
 @Getter
 @Setter
 public class UserLevelStatsDto {
    private int level;
    private String levelName;
    private int count;
}
