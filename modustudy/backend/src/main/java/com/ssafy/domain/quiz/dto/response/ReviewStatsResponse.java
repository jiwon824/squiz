package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.service.FsrsConstants;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 복습 통계 응답 DTO.
 *
 * 사용자의 전체 학습 진행 상황을 요약한다.
 *
 * @param totalItems            전체 복습 항목 수
 * @param dueItems              오늘 복습 예정 항목 수
 * @param newItems              신규 항목 수 (state=0)
 * @param learningItems         학습 중 항목 수 (state=1)
 * @param reviewItems           복습 중 항목 수 (state=2)
 * @param relearningItems       재학습 중 항목 수 (state=3)
 * @param averageStability      평균 안정성
 * @param totalReps             총 복습 횟수
 * @param totalLapses           총 오답 횟수
 * @param proficiency           숙련도 (복습 상태 항목 비율, 0.0~1.0)
 * @param averageRetrievability 평균 기억도 (0.0~1.0)
 * @param matureCards           장기 기억 전환 문제 수
 * @param dailyMaxCombo         오늘의 최고 콤보
 */
@Schema(description = "복습 통계 응답")
public record ReviewStatsResponse(

        @Schema(description = "전체 복습 항목 수", example = "50") int totalItems,

        @Schema(description = "오늘 복습 예정 항목 수", example = "5") int dueItems,

        @Schema(description = "신규 항목 수", example = "10") int newItems,

        @Schema(description = "학습 중 항목 수", example = "3") int learningItems,

        @Schema(description = "복습 중 항목 수", example = "30") int reviewItems,

        @Schema(description = "재학습 중 항목 수", example = "7") int relearningItems,

        @Schema(description = "평균 안정성", example = "4.52") double averageStability,

        @Schema(description = "총 복습 횟수", example = "120") int totalReps,

        @Schema(description = "총 오답 횟수", example = "15") int totalLapses,

        @Schema(description = "숙련도 (0.0~1.0)", example = "0.60") double proficiency,

        @Schema(description = "평균 기억도 (0.0~1.0)", example = "0.85") double averageRetrievability,

        @Schema(description = "장기 기억 전환 문제 수", example = "150") int matureCards,

        @Schema(description = "오늘의 최고 콤보", example = "12") int dailyMaxCombo) {
    /**
     * UserReviewItem 목록과 복습 예정 항목 수로부터 통계 응답 DTO를 생성한다.
     *
     * @param allItems 전체 복습 항목 목록
     * @param dueCount 복습 예정 항목 수
     */
    public static ReviewStatsResponse from(List<UserReviewItem> allItems, int dueCount) {
        int total = allItems.size();
        if (total == 0) {
            return new ReviewStatsResponse(0, 0, 0, 0, 0, 0, 0.0, 0, 0, 0.0, 0.0, 0, 0);
        }

        int newCount = 0;
        int learningCount = 0;
        int reviewCount = 0;
        int relearningCount = 0;
        double stabilitySum = 0.0;
        int repsSum = 0;
        int lapsesSum = 0;

        for (UserReviewItem item : allItems) {
            switch (item.getState()) {
                case FsrsConstants.STATE_NEW -> newCount++;
                case FsrsConstants.STATE_LEARNING -> learningCount++;
                case FsrsConstants.STATE_REVIEW -> reviewCount++;
                case FsrsConstants.STATE_RELEARNING -> relearningCount++;
            }
            stabilitySum += item.getStability();
            repsSum += item.getReps();
            lapsesSum += item.getLapses();
        }

        double avgStability = stabilitySum / total;
        double proficiency = (double) reviewCount / total;

        return new ReviewStatsResponse(
                total,
                dueCount,
                newCount,
                learningCount,
                reviewCount,
                relearningCount,
                Math.round(avgStability * 100.0) / 100.0,
                repsSum,
                lapsesSum,
                Math.round(proficiency * 100.0) / 100.0,
                0.0, // averageRetrievability (placeholder)
                0, // matureCards (placeholder)
                0 // dailyMaxCombo (placeholder)
        );
    }

    public ReviewStatsResponse withExtraStats(double avgRetrievability, int matureCards, int maxCombo) {
        return new ReviewStatsResponse(
                totalItems(), dueItems(), newItems(), learningItems(), reviewItems(), relearningItems(),
                averageStability(), totalReps(), totalLapses(), proficiency(),
                avgRetrievability, matureCards, maxCombo);
    }
}
