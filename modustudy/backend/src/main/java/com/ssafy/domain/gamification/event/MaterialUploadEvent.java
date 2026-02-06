package com.ssafy.domain.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 자료 업로드 이벤트
 */
 @Getter
 @AllArgsConstructor
 public class MaterialUploadEvent {
    private final Long userId;
    private final Long studyId;
    private final String studyName;
    private final Long materialId;
    private final String materialName;
    private final LocalDate uploadDate;
}
