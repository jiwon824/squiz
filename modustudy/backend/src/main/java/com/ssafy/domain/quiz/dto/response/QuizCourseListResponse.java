package com.ssafy.domain.quiz.dto.response;

import java.util.List;

/**
 * 코스 목록 응답 DTO.
 *
 * 코스 목록 API의 data 영역에 매핑된다.
 *
 */
 public record QuizCourseListResponse(List<QuizCourseListItem> courses) {
}
