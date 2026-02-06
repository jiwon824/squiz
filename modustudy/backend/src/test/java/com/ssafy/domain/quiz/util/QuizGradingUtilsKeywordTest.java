package com.ssafy.domain.quiz.util;

import com.ssafy.domain.quiz.entity.enums.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 서술형 키워드 기반 채점 테스트
 */
 class QuizGradingUtilsKeywordTest {

    @Test
    @DisplayName("키워드 1개 - 정확히 일치")
    void singleKeyword_exactMatch() {
        String keywords = "[\"DDL\"]";
        boolean result = QuizGradingUtils.grade(
                "DDL", "DDL", QuestionType.SHORT_ANSWER, null, keywords);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키워드 1개 - 대소문자 무시")
    void singleKeyword_caseInsensitive() {
        String keywords = "[\"DDL\"]";
        boolean result = QuizGradingUtils.grade(
                "ddl", "DDL", QuestionType.SHORT_ANSWER, null, keywords);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키워드 1개 - 문장에 포함")
    void singleKeyword_containedInSentence() {
        String keywords = "[\"DDL\"]";
        boolean result = QuizGradingUtils.grade(
                "DDL은 데이터 정의 언어입니다", "DDL", QuestionType.SHORT_ANSWER, null, keywords);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키워드 1개 - 미포함 오답")
    void singleKeyword_notContained() {
        String keywords = "[\"DDL\"]";
        boolean result = QuizGradingUtils.grade(
                "DML", "DDL", QuestionType.SHORT_ANSWER, null, keywords);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("키워드 2개 - 모두 포함 정답")
    void multipleKeywords_allContained() {
        String keywords = "[\"엔티티\", \"관계\"]";
        boolean result = QuizGradingUtils.grade(
                "엔티티와 관계를 나타냅니다", "엔티티와 관계", QuestionType.SHORT_ANSWER, null, keywords);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키워드 2개 - 일부만 포함 오답")
    void multipleKeywords_partialMatch() {
        String keywords = "[\"엔티티\", \"관계\"]";
        boolean result = QuizGradingUtils.grade(
                "엔티티만 알아요", "엔티티와 관계", QuestionType.SHORT_ANSWER, null, keywords);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("키워드 없으면 기존 로직 (정확히 일치)")
    void noKeywords_fallbackToExactMatch() {
        boolean result = QuizGradingUtils.grade(
                "INSERT", "INSERT", QuestionType.SHORT_ANSWER, null, null);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키워드 없으면 기존 로직 (대소문자 무시)")
    void noKeywords_fallbackCaseInsensitive() {
        boolean result = QuizGradingUtils.grade(
                "insert", "INSERT", QuestionType.SHORT_ANSWER, null, null);
        assertThat(result).isTrue();
    }
}
