package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.ssafy.domain.quiz.entity.enums.QuestionType;

/**
 * 사용자 섹션 시도별 문제 엔티티.
 *
 * 각 시도(Attempt)에 할당된 문제와 셔플된 순서를 저장한다.
 * 사용자의 답안과 정답 여부도 이 테이블에 기록된다.
 *
 * 핵심 역할:
 * - 시도별 문제 순서 고정 (order_index)
 * - 임시 저장 지원 (user_answer)
 * - 채점 결과 저장 (is_correct)
 *
 * DDL 참조: docs/sql/ERD.sql - user_section_attempt_question
 */
@Entity
@Table(name = "user_section_attempt_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attempt_question", columnNames = { "attempt_id", "question_id" })
}, indexes = {
        @Index(name = "idx_attempt_question_order", columnList = "attempt_id, order_index")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSectionAttemptQuestion extends BaseEntity {

    /**
     * 낙관적 잠금 버전 (Optimistic Locking).
     * 동시 답안 저장 충돌 방지를 위해 Hibernate가 자동 관리.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * 소속 시도.
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private UserSectionAttempt attempt;

    /**
     * 할당된 문제.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizCourseQuestion question;

    /**
     * 셔플된 순서 (1부터 시작).
     * 동일 attempt 내에서 유일한 순서를 가진다.
     */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /**
     * 사용자 답안.
     * 객관식: "A", "B" 등
     * 단답형: 사용자 입력 텍스트
     * null: 아직 답하지 않음
     */
    @Column(name = "user_answer", length = 500)
    private String userAnswer;

    /**
     * 정답 여부.
     * null: 아직 채점되지 않음 (임시 저장 상태)
     * true/false: 채점 완료
     */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    /**
     * 답변 시각.
     * 최초 답변 또는 마지막 수정 시각.
     */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /**
     * 답변하는 데에 걸린 시간
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs = 0L; // 기본값 0 설정

    @Builder
    public UserSectionAttemptQuestion(
            UserSectionAttempt attempt,
            QuizCourseQuestion question,
            Integer orderIndex) {
        this.attempt = attempt;
        this.question = question;
        this.orderIndex = orderIndex;
    }

    /**
     * 사용자 답안을 저장한다 (임시 저장).
     *
     * @param answer 사용자 답안
     */
    public void saveAnswer(String answer) {
        this.userAnswer = answer;
        this.answeredAt = LocalDateTime.now();
    }

    /**
     * 사용자 답안과 응답 시간을 함께 저장한다.
     *
     * @param answer         사용자 답안
     * @param responseTimeMs 응답 시간(ms)
     */
    public void saveAnswer(String answer, Long responseTimeMs) {
        this.userAnswer = answer;
        this.responseTimeMs = responseTimeMs;
        this.answeredAt = LocalDateTime.now();
    }

    /**
     * 답안을 채점한다.
     *
     * @param correctAnswer 정답
     */
    public void grade(String correctAnswer) {
        // 미답변 문제는 오답 처리
        if (this.userAnswer == null) {
            this.isCorrect = false;
            return;
        }

        // 문제가 있으면서, 문제 유형이 다중 정답 문제인 경우 flag true로 변경
        boolean isMultiValue = false;
        if (this.question != null
                && this.question.getQuestionType() == QuestionType.MULTIPLE_CHOICE_MULTIPLE) {
            isMultiValue = true;
        }

        if (isMultiValue) {
            // 사용자의 답안을 파싱하여 Set에 넣기
            Set<String> userSet = parseAnswerToSet(this.userAnswer);
            // 매개변수로 들어온 correctAnswer를 파싱하여 Set에 넣기
            Set<String> correctSet = parseAnswerToSet(correctAnswer);
            // 사용자의 답안과 매개변수로 들어온 correctAnswer의 구성요소가 같으면 true 정답
            // 다르면 false 오답
            this.isCorrect = userSet.equals(correctSet);
        } else {
            // 대소문자 무시, 앞뒤 공백 제거 후 비교
            this.isCorrect = this.userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
        }
    }

    /**
     * answer를 Set으로 반환
     *
     * @param answer
     * @return
     */
    private Set<String> parseAnswerToSet(String answer) {
        if (answer == null || answer.isBlank()) {
            return Collections.emptySet();
        }

        // Remove JSON brackets and quotes to handle ["A", "B"] or "A, B"
        String cleaned = answer.replaceAll("[\\[\\]\"]", "");
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    /**
     * 답변 완료 여부를 확인한다.
     */
    public boolean isAnswered() {
        return this.userAnswer != null && !this.userAnswer.isBlank();
    }
}
