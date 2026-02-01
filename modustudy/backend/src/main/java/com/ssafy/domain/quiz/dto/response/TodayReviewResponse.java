package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오늘 복습 예정 항목 응답 DTO.
 *
 * @param items      복습 예정 항목 목록
 * @param totalCount 총 항목 수
 */
@Schema(description = "오늘 복습 예정 항목 응답")
public record TodayReviewResponse(

        @Schema(description = "복습 예정 항목 목록")
        List<ReviewItemDto> items,

        @Schema(description = "총 항목 수", example = "5")
        int totalCount
) {
    /**
     * UserReviewItem 목록으로부터 응답 DTO를 생성한다.
     */
    public static TodayReviewResponse from(List<UserReviewItem> items) {
        List<ReviewItemDto> dtos = items.stream()
                .map(ReviewItemDto::from)
                .toList();
        return new TodayReviewResponse(dtos, dtos.size());
    }

    /**
     * 복습 예정 항목 DTO.
     *
     * @param reviewItemId 복습 항목 ID
     * @param contentType  콘텐츠 유형
     * @param contentId    콘텐츠 ID
     * @param stability    안정성
     * @param difficulty   난이도
     * @param state        카드 상태
     * @param reps         복습 횟수
     * @param lapses       오답 횟수
     * @param nextReviewAt 복습 예정일
     */
    @Schema(description = "복습 예정 항목")
    public record ReviewItemDto(

            @Schema(description = "복습 항목 ID", example = "1")
            Long reviewItemId,

            @Schema(description = "콘텐츠 유형", example = "COURSE_QUESTION")
            ReviewContentType contentType,

            @Schema(description = "콘텐츠 ID", example = "101")
            Long contentId,

            @Schema(description = "안정성", example = "2.40")
            double stability,

            @Schema(description = "난이도", example = "4.93")
            double difficulty,

            @Schema(description = "카드 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)", example = "2")
            int state,

            @Schema(description = "복습 횟수", example = "3")
            int reps,

            @Schema(description = "오답 횟수", example = "1")
            int lapses,

            @Schema(description = "복습 예정일")
            LocalDateTime nextReviewAt
    ) {
        /**
         * UserReviewItem 엔티티로부터 DTO를 생성한다.
         */
        public static ReviewItemDto from(UserReviewItem item) {
            return new ReviewItemDto(
                    item.getId(),
                    item.getContentType(),
                    item.getContentId(),
                    item.getStability(),
                    item.getDifficulty(),
                    item.getState(),
                    item.getReps(),
                    item.getLapses(),
                    item.getNextReviewAt()
            );
        }
    }
}
