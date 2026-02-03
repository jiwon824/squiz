package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.UserReviewItem;
import lombok.Getter;

/**
 * 복습 처리 결과를 담는 내부 DTO.
 * FsrsService에서 Controller로 채점 결과와 함께 UserReviewItem을 전달할 때 사용.
 */
@Getter
public class ReviewResult {
    private final UserReviewItem item;
    private final boolean isCorrect;
    private final String correctAnswer;

    public ReviewResult(UserReviewItem item, boolean isCorrect, String correctAnswer) {
        this.item = item;
        this.isCorrect = isCorrect;
        this.correctAnswer = correctAnswer;
    }
}
