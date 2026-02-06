package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.QuizCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 퀴즈 코스 조회 전용 레포지토리.
 *
 * 호출자: {@link com.ssafy.domain.quiz.service.QuizCourseService}
 *
 */
 public interface QuizCourseRepository extends JpaRepository<QuizCourse, Long> {
    /**
     * 활성 코스를 정렬 기준에 맞춰 조회한다.
     *
     * @return 활성 코스 목록 (sortOrder ASC, id ASC)
     */
    List<QuizCourse> findAllByIsActiveTrueOrderBySortOrderAscIdAsc();

    /**
     * 코스 ID로 코스와 섹션 목록을 함께 조회한다.
     * N+1 문제 방지를 위해 fetch join 사용.
     *
     * @param courseId 코스 ID
     * @return 코스 (섹션 포함)
     */
    @Query("SELECT c FROM QuizCourse c LEFT JOIN FETCH c.sections WHERE c.id = :courseId")
    Optional<QuizCourse> findByIdWithSections(@Param("courseId") Long courseId);

    /**
     * 사용자별 코스 통계 조회.
     * <p>
     * - Attempted: 풀이 이력이 있는 고유 문제 수
     * - Correct: 마지막 풀이가 정답인 고유 문제 수
     * </p>
     */
    @Query(value = """
            SELECT
                c.name as courseName,
                c.code as courseCode,
                COUNT(DISTINCT q.id) as attemptedCount,
                COUNT(DISTINCT CASE WHEN sub.is_correct = true THEN q.id ELSE NULL END) as correctCount
            FROM quiz_course c
            INNER JOIN quiz_course_section s ON s.quiz_course_id = c.id
            INNER JOIN quiz_course_question q ON q.quiz_course_id = s.quiz_course_id AND q.section_number = s.section_number
            INNER JOIN user_review_items uri ON uri.content_id = q.id AND uri.content_type = 'COURSE_QUESTION'
            INNER JOIN (
                 SELECT review_item_id, is_correct
                 FROM (
                     SELECT review_item_id, is_correct,
                            ROW_NUMBER() OVER (PARTITION BY review_item_id ORDER BY reviewed_at DESC) as rn
                     FROM user_review_log
                 ) t WHERE rn = 1
            ) sub ON sub.review_item_id = uri.id
            WHERE uri.user_id = :userId
            GROUP BY c.id, c.name, c.code
            """, nativeQuery = true)
    List<CourseQuizStatProjection> findCourseStats(@Param("userId") Long userId);
}
