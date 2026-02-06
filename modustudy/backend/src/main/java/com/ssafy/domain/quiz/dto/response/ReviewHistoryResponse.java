package com.ssafy.domain.quiz.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.UserReviewLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 복습 항목 이력 응답 DTO.
 *
 * 복습 항목의 현재 FSRS 상태와 복습 이력 목록을 반환한다.
 *
 * @param reviewItemId 복습 항목 ID
 * @param contentType  콘텐츠 유형
 * @param contentId    콘텐츠 ID
 * @param stability    현재 안정성
 * @param difficulty   현재 난이도
 * @param state        현재 카드 상태
 * @param reps         총 복습 횟수
 * @param lapses       총 오답 횟수
 * @param nextReviewAt 다음 복습 예정일
 * @param logs         복습 이력 목록 (최신순)
 */
 @Schema(description = "복습 항목 이력 응답")
 public record ReviewHistoryResponse(

        @Schema(description = "복습 항목 ID", example = "1")
        Long reviewItemId,

        @Schema(description = "콘텐츠 유형", example = "COURSE_QUESTION")
        ReviewContentType contentType,

        @Schema(description = "콘텐츠 ID", example = "101")
        Long contentId,

        @Schema(description = "현재 안정성", example = "5.80")
        double stability,

        @Schema(description = "현재 난이도", example = "4.93")
        double difficulty,

        @Schema(description = "카드 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)", example = "2")
        int state,

        @Schema(description = "총 복습 횟수", example = "5")
        int reps,

        @Schema(description = "총 오답 횟수", example = "1")
        int lapses,

        @Schema(description = "다음 복습 예정일")
        LocalDateTime nextReviewAt,

        @Schema(description = "복습 이력 목록 (최신순)")
        List<ReviewLogDto> logs
        ) {
    /**
     * UserReviewItem과 UserReviewLog 목록으로부터 응답 DTO를 생성한다.
     */
    public static ReviewHistoryResponse from(UserReviewItem item, List<UserReviewLog> logs) {
        List<ReviewLogDto> logDtos = logs.stream()
                .map(ReviewLogDto::from)
                .toList();

        return new ReviewHistoryResponse(
                item.getId(),
                item.getContentType(),
                item.getContentId(),
                item.getStability(),
                item.getDifficulty(),
                item.getState(),
                item.getReps(),
                item.getLapses(),
                item.getNextReviewAt(),
                logDtos
        );
    }

    /**
     * 복습 이력 DTO.
     *
     * @param logId          이력 ID
     * @param isCorrect      정답 여부
     * @param responseTimeMs 응답 시간 (밀리초)
     * @param stability      복습 시점의 안정성 스냅샷
     * @param difficulty     복습 시점의 난이도 스냅샷
     * @param reviewedAt     복습 일시
     */
    @Schema(description = "복습 이력 항목")
    public record ReviewLogDto(

            @Schema(description = "이력 ID", example = "1")
            Long logId,

            @Schema(description = "정답 여부", example = "true")
            @JsonProperty("correct")
            boolean isCorrect,

            @Schema(description = "응답 시간 (밀리초)", example = "3200")
            long responseTimeMs,

            @Schema(description = "복습 시점 안정성", example = "2.40")
            double stability,

            @Schema(description = "복습 시점 난이도", example = "4.93")
            double difficulty,

            @Schema(description = "복습 일시")
            LocalDateTime reviewedAt
    ) {
        /**
         * UserReviewLog 엔티티로부터 DTO를 생성한다.
         */
        public static ReviewLogDto from(UserReviewLog log) {
            return new ReviewLogDto(
                    log.getId(),
                    log.getIsCorrect(),
                    log.getResponseTimeMs(),
                    log.getStability(),
                    log.getDifficulty(),
                    log.getReviewedAt()
            );
        }
    }
}
