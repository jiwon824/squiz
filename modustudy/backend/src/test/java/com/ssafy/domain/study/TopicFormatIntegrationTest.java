package com.ssafy.domain.study;

import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V9, V10 DB 마이그레이션 통합 테스트
 * - Topic/Format 테이블 생성 및 초기 데이터 검증
 * - study 테이블 스키마 변경 검증 (VARCHAR → FK)
 *
 * 주의: 이 테스트는 Flyway 마이그레이션이 적용된 실제 DB 환경에서 수동 실행해야 합니다.
 * 테스트 환경에서는 Flyway가 비활성화되어 순환 의존성 문제가 발생합니다.
 */
@SpringBootTest
@Transactional
@Disabled("Flyway 마이그레이션 테스트는 실제 DB 환경에서 수동 실행 필요 - 테스트 환경에서 순환 의존성 발생")
@DisplayName("Topic/Format 마이그레이션 통합 테스트")
public class TopicFormatIntegrationTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("V9: Topic 테이블이 생성되고 10개 대분류가 존재한다")
    void testTopicParentCategoriesExist() {
        // Given & When
        List<Topic> allTopics = topicRepository.findAll();

        // Then
        assertThat(allTopics).isNotEmpty();
        assertThat(allTopics.size()).isGreaterThanOrEqualTo(10); // 최소 10개 이상 (대분류 + 소분류)

        // 대분류 확인
        long parentCount = allTopics.stream()
            .filter(topic -> topic.getParent() == null)
            .count();

        assertThat(parentCount).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("V9: Format 테이블이 생성되고 8개 형식이 존재한다")
    void testFormatCategoriesExist() {
        // Given & When
        List<Format> allFormats = formatRepository.findAll();

        // Then
        assertThat(allFormats)
            .isNotEmpty()
            .hasSizeGreaterThanOrEqualTo(8);  // 최소 8개 이상
    }

    @Test
    @DisplayName("V9: '알고리즘/코딩테스트' 대분류 아래 소분류들이 존재한다")
    void testAlgorithmTopicHierarchy() {
        // Given & When
        List<Topic> allTopics = topicRepository.findAll();

        // '알고리즘/코딩테스트' 대분류 찾기
        Topic algorithmParent = allTopics.stream()
            .filter(topic -> topic.getParent() == null)
            .filter(topic -> topic.getName().contains("알고리즘"))
            .findFirst()
            .orElse(null);

        // Then
        assertThat(algorithmParent).isNotNull();
        assertThat(algorithmParent.getName()).contains("알고리즘");

        // 소분류 확인 (백준, 프로그래머스, SWEA 등)
        Long parentId = algorithmParent.getId();
        long childCount = allTopics.stream()
            .filter(topic -> topic.getParent() != null)
            .filter(topic -> topic.getParent().getId().equals(parentId))
            .count();

        assertThat(childCount).isGreaterThanOrEqualTo(4);  // 최소 4개 이상
    }

    @Test
    @DisplayName("V10: study 테이블에 topic_id 컬럼이 존재한다 (VARCHAR topic 제거됨)")
    void testStudyTableSchemaChanged() throws Exception {
        // Given & When
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESC study")) {

            boolean hasTopicId = false;
            boolean hasOldTopic = false;

            while (rs.next()) {
                String columnName = rs.getString("Field");
                if ("topic_id".equals(columnName)) {
                    hasTopicId = true;
                    String type = rs.getString("Type");
                    assertThat(type).containsIgnoringCase("bigint");
                }
                if ("topic".equals(columnName)) {
                    hasOldTopic = true;
                }
            }

            // Then
            assertThat(hasTopicId).isTrue()
                .withFailMessage("study 테이블에 topic_id 컬럼이 없습니다. V10 마이그레이션을 확인하세요.");

            assertThat(hasOldTopic).isFalse()
                .withFailMessage("study 테이블에 기존 topic VARCHAR 컬럼이 남아있습니다. V10 마이그레이션을 확인하세요.");
        }
    }

    @Test
    @DisplayName("V10: study 테이블에 format_id 컬럼이 존재한다 (VARCHAR format 제거됨)")
    void testStudyTableFormatIdExists() throws Exception {
        // Given & When
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESC study")) {

            boolean hasFormatId = false;
            boolean hasOldFormat = false;

            while (rs.next()) {
                String columnName = rs.getString("Field");
                if ("format_id".equals(columnName)) {
                    hasFormatId = true;
                    String type = rs.getString("Type");
                    assertThat(type).containsIgnoringCase("bigint");
                }
                if ("format".equals(columnName)) {
                    hasOldFormat = true;
                }
            }

            // Then
            assertThat(hasFormatId).isTrue()
                .withFailMessage("study 테이블에 format_id 컬럼이 없습니다. V10 마이그레이션을 확인하세요.");

            assertThat(hasOldFormat).isFalse()
                .withFailMessage("study 테이블에 기존 format VARCHAR 컬럼이 남아있습니다. V10 마이그레이션을 확인하세요.");
        }
    }

    @Test
    @DisplayName("V10: topic_id와 format_id에 외래 키가 설정되어 있다")
    void testForeignKeysExist() throws Exception {
        // Given & When
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME " +
                 "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                 "WHERE TABLE_SCHEMA = DATABASE() " +
                 "AND TABLE_NAME = 'study' " +
                 "AND REFERENCED_TABLE_NAME IN ('topic', 'format')"
             )) {

            boolean hasTopicFK = false;
            boolean hasFormatFK = false;

            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String refTable = rs.getString("REFERENCED_TABLE_NAME");

                if ("topic_id".equals(columnName) && "topic".equals(refTable)) {
                    hasTopicFK = true;
                }
                if ("format_id".equals(columnName) && "format".equals(refTable)) {
                    hasFormatFK = true;
                }
            }

            // Then
            assertThat(hasTopicFK).isTrue()
                .withFailMessage("topic_id에 외래 키가 설정되지 않았습니다.");

            assertThat(hasFormatFK).isTrue()
                .withFailMessage("format_id에 외래 키가 설정되지 않았습니다.");
        }
    }

    @Test
    @DisplayName("주제와 형식의 sortOrder가 정상적으로 설정되어 있다")
    void testSortOrderExists() {
        // Given & When
        List<Topic> topics = topicRepository.findAll();
        List<Format> formats = formatRepository.findAll();

        // Then
        topics.forEach(topic -> {
            assertThat(topic.getSortOrder()).isNotNull();
        });

        formats.forEach(format -> {
            assertThat(format.getSortOrder()).isNotNull();
        });
    }

    @Test
    @DisplayName("특정 주제와 형식이 데이터베이스에 존재한다")
    void testSpecificTopicsAndFormatsExist() {
        // Given & When
        List<Topic> allTopics = topicRepository.findAll();
        List<Format> allFormats = formatRepository.findAll();

        // Then - 주요 주제 존재 확인
        boolean hasSWEA = allTopics.stream()
            .anyMatch(topic -> "SWEA".equals(topic.getName()));

        boolean hasBaekjoon = allTopics.stream()
            .anyMatch(topic -> "백준".equals(topic.getName()));

        boolean hasProgrammers = allTopics.stream()
            .anyMatch(topic -> "프로그래머스".equals(topic.getName()));

        assertThat(hasSWEA).isTrue()
            .withFailMessage("SWEA 주제가 없습니다.");
        assertThat(hasBaekjoon).isTrue()
            .withFailMessage("백준 주제가 없습니다.");
        assertThat(hasProgrammers).isTrue()
            .withFailMessage("프로그래머스 주제가 없습니다.");

        // 주요 형식 존재 확인
        boolean hasProblemSolving = allFormats.stream()
            .anyMatch(format -> "문제 풀이".equals(format.getName()));

        boolean hasBookStudy = allFormats.stream()
            .anyMatch(format -> "독서/책 스터디".equals(format.getName()));

        assertThat(hasProblemSolving).isTrue()
            .withFailMessage("문제 풀이 형식이 없습니다.");
        assertThat(hasBookStudy).isTrue()
            .withFailMessage("독서/책 스터디 형식이 없습니다.");
    }
}