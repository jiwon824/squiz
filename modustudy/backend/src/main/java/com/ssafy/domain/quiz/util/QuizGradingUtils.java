package com.ssafy.domain.quiz.util;

import com.ssafy.domain.quiz.entity.enums.QuestionType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 퀴즈 채점 로직을 담당하는 유틸리티 클래스.
 * 
 * <p>
 * UserSectionAttemptQuestion(퀴즈 코스)와 FsrsService(복습) 등
 * 여러 도메인에서 동일한 채점 기준을 적용하기 위해 분리됨.
 * </p>
 */
public class QuizGradingUtils {

    /**
     * 답안을 채점한다.
     *
     * @param userAnswer    사용자가 제출한 답안
     * @param correctAnswer 정답
     * @param questionType  문제 유형
     * @return 정답 여부 (true/false)
     */
    public static boolean grade(String userAnswer, String correctAnswer, QuestionType questionType) {
        // 미답변 문제는 오답 처리
        if (userAnswer == null) {
            return false;
        }

        // 정답 데이터가 없는 경우 (예외적 상황)
        if (correctAnswer == null) {
            return false;
        }

        // 다중 선택(객관식 중복 정답) 처리
        if (questionType == QuestionType.MULTIPLE_CHOICE_MULTIPLE) {
            Set<String> userSet = parseAnswerToSet(userAnswer);
            Set<String> correctSet = parseAnswerToSet(correctAnswer);
            return userSet.equals(correctSet);
        }

        // 일반 객관식/단답형 처리
        // 대소문자 무시, 앞뒤 공백 제거 후 비교
        return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }

    /**
     * 답안 문자열을 Set으로 파싱한다.
     * JSON 배열 형태(["A", "B"])나 쉼표 구분 문자열("A, B")을 모두 처리한다.
     *
     * @param answer 답안 문자열
     * @return 파싱된 답안 Set (대문자 변환됨)
     */
    private static Set<String> parseAnswerToSet(String answer) {
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
}
