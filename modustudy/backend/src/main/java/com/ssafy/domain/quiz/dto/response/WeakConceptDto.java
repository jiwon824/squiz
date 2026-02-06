package com.ssafy.domain.quiz.dto.response;

import lombok.Builder;

@Builder
public record WeakConceptDto(
        Long courseId,
        String courseName,
        Integer sectionNumber,
        String sectionName,
        Double weaknessScore) {
}
