package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
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

        @Schema(description = "복습 예정 항목 목록") List<ReviewItemDto> items,

        @Schema(description = "총 항목 수", example = "5") int totalCount,

        @Schema(description = "총 페이지 수", example = "1") int totalPages,
        @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0") int number,
        @Schema(description = "페이지 크기", example = "5") int size) {

    /**
     * UserReviewItem 목록으로부터 응답 DTO를 생성한다 (문제 정보 없음).
     * 
     * @deprecated 문제 정보가 필요한 경우 서비스에서 직접 조립해야 함
     */
    @Deprecated
    public static TodayReviewResponse from(List<UserReviewItem> items) {
        List<ReviewItemDto> dtos = items.stream()
                .map(item -> ReviewItemDto.from(item, null))
                .toList();
        return new TodayReviewResponse(dtos, dtos.size(), 1, 0, Math.max(1, dtos.size()));
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
     * @param question     문제 상세 정보 (지문, 보기, 정답 등)
     */
    @Schema(description = "복습 예정 항목")
    public record ReviewItemDto(

            @Schema(description = "복습 항목 ID", example = "1") Long reviewItemId,

            @Schema(description = "콘텐츠 유형", example = "COURSE_QUESTION") ReviewContentType contentType,

            @Schema(description = "콘텐츠 ID", example = "101") Long contentId,

            @Schema(description = "안정성", example = "2.40") double stability,

            @Schema(description = "난이도", example = "4.93") double difficulty,

            @Schema(description = "카드 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)", example = "2") int state,

            @Schema(description = "복습 횟수", example = "3") int reps,

            @Schema(description = "오답 횟수", example = "1") int lapses,

            @Schema(description = "복습 예정일") LocalDateTime nextReviewAt,

            @Schema(description = "문제 상세 정보") QuestionDetail question) {
        /**
         * UserReviewItem 엔티티로부터 DTO를 생성한다 (문제 정보 포함).
         */
        public static ReviewItemDto from(UserReviewItem item, QuestionDetail question) {
            return new ReviewItemDto(
                    item.getId(),
                    item.getContentType(),
                    item.getContentId(),
                    item.getStability(),
                    item.getDifficulty(),
                    item.getState(),
                    item.getReps(),
                    item.getLapses(),
                    item.getNextReviewAt(),
                    question);
        }
    }

    /**
     * 문제 상세 정보 DTO.
     */
    @Schema(description = "문제 상세 정보")
    public record QuestionDetail(
            @Schema(description = "문제 순서 번호", example = "1") Integer questionNumber,

            @Schema(description = "문제 내용", example = "Java에서 정수형 변수를 선언할 때 사용하는 키워드는?") String questionText,

            @Schema(description = "문제 유형", example = "MULTIPLE_CHOICE") QuestionType questionType,

            @Schema(description = "객관식 보기 목록") List<OptionItem> options,

            @Schema(description = "정답", example = "A") String correctAnswer,

            @Schema(description = "해설", example = "설명...") String explanation,

            @Schema(description = "문제 태그/카테고리", example = "TypeScript") String category, // UI 상단의 카테고리 뱃지용

            // UI의 "마지막 오답" 표시용
            @Schema(description = "마지막 학습/오답 일자") LocalDateTime lastReviewAt) {
    }
}
