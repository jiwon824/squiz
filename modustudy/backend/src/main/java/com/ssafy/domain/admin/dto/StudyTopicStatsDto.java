package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 토픽별 스터디 분포 DTO
 */
 @Getter
 @Setter
 public class StudyTopicStatsDto {
    private String topicName;
    private int count;
}
