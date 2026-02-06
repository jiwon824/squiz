package com.ssafy.domain.quiz.dto.response;

/**
 * 배지 정보 DTO.
 *
 * 코스 상세 응답 내 badge 객체로 사용된다.
 */
 public record BadgeInfo(
        String code,
        String name,
        String description
        ) {
}
