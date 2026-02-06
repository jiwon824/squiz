package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Continuous Learning 모드를 위한 가중치 기반 문제 선택 Repository.
 *
 * <p>
 * 섹션 클릭 시 가중치 기반 랜덤으로 문제를 출제한다.
 * </p>
 *
 * <h3>가중치 로직</h3>
 * <ul>
 * <li>풀지 않은 문제 (reps=0): 가중치 1.0 (가장 높음)</li>
 * <li>1번 푼 문제: 가중치 0.5</li>
 * <li>2번 푼 문제: 가중치 0.33</li>
 * <li>n번 푼 문제: 가중치 1/(n+1)</li>
 * </ul>
 *
 * <p>
 * 가중치가 높을수록 선택될 확률이 높지만, 낮은 문제도 선택될 수 있음 (랜덤성 유지)
 * </p>
 */
public interface ContinuousQuizRepository extends JpaRepository<QuizCourseQuestion, Long> {

    /**
     * 특정 섹션에서 가중치 기반 랜덤으로 다음 문제를 선택한다.
     *
     * <p>
     * 가중치 공식: weight = 1 / (reps + 1)
     * </p>
     * <p>
     * 랜덤 선택: ORDER BY -LOG(1 - RAND()) / weight (지수 분포 활용)
     * </p>
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param userId        사용자 ID
     * @return 선택된 문제 (Optional)
     */
    @Query(value = """
            SELECT q.*
            FROM quiz_course_question q
            LEFT JOIN user_review_items uri
                ON uri.content_type = 'COURSE_QUESTION'
                AND uri.content_id = q.id
                AND uri.user_id = :userId
            WHERE q.quiz_course_id = :courseId
              AND q.section_number = :sectionNumber
              AND q.question_type != 'SHORT_ANSWER'
            ORDER BY -LN(1 - RAND()) / (1.0 / (COALESCE(uri.reps, 0) + 1))
            LIMIT 1
            """, nativeQuery = true)
    Optional<QuizCourseQuestion> findNextQuestionWeightedRandom(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber,
            @Param("userId") Long userId);

    /**
     * 특정 섹션에서 가중치 기반 랜덤으로 여러 문제를 선택한다.
     *
     * <p>
     * Pageable을 통해 LIMIT을 처리한다.
     * LIMIT :param은 MySQL 5.x / 특정 JDBC 드라이버에서 MySQLSyntaxErrorException을 유발할 수
     * 있으므로
     * Spring이 Pageable로 LIMIT 절을 주입하도록 위임한다.
     * </p>
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param userId        사용자 ID
     * @param pageable      페이지 정보 (PageRequest.of(0, limit)로 호출)
     * @return 선택된 문제 목록
     */
    @Query(value = """
            SELECT q.*
            FROM quiz_course_question q
            LEFT JOIN user_review_items uri
                ON uri.content_type = 'COURSE_QUESTION'
                AND uri.content_id = q.id
                AND uri.user_id = :userId
            WHERE q.quiz_course_id = :courseId
              AND q.section_number = :sectionNumber
              AND q.question_type != 'SHORT_ANSWER'
            ORDER BY -LN(1 - RAND()) / (1.0 / (COALESCE(uri.reps, 0) + 1))
            """, nativeQuery = true)
    List<QuizCourseQuestion> findQuestionsWeightedRandom(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber,
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * 특정 코스 전체에서 가중치 기반 랜덤으로 다음 문제를 선택한다.
     * (섹션 무관)
     *
     * @param courseId 코스 ID
     * @param userId   사용자 ID
     * @return 선택된 문제 (Optional)
     */
    @Query(value = """
            SELECT q.*
            FROM quiz_course_question q
            LEFT JOIN user_review_items uri
                ON uri.content_type = 'COURSE_QUESTION'
                AND uri.content_id = q.id
                AND uri.user_id = :userId
            WHERE q.quiz_course_id = :courseId
              AND q.question_type != 'SHORT_ANSWER'
            ORDER BY -LN(1 - RAND()) / (1.0 / (COALESCE(uri.reps, 0) + 1))
            LIMIT 1
            """, nativeQuery = true)
    Optional<QuizCourseQuestion> findNextQuestionWeightedRandomByCourse(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId);

    // ────────────────────────────────────────────────────────────────────────────
    // 통계 조회
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * 섹션 내 전체 문제 수 (객관식만)
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM quiz_course_question q
            WHERE q.quiz_course_id = :courseId
              AND q.section_number = :sectionNumber
              AND q.question_type != 'SHORT_ANSWER'
            """, nativeQuery = true)
    long countTotalQuestions(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber);

    /**
     * 섹션 내 한 번도 풀지 않은 문제 수 (객관식만)
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM quiz_course_question q
            WHERE q.quiz_course_id = :courseId
              AND q.section_number = :sectionNumber
              AND q.question_type != 'SHORT_ANSWER'
              AND NOT EXISTS (
                  SELECT 1 FROM user_review_items uri
                  WHERE uri.user_id = :userId
                    AND uri.content_type = 'COURSE_QUESTION'
                    AND uri.content_id = q.id
              )
            """, nativeQuery = true)
    long countUnstudiedQuestions(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber,
            @Param("userId") Long userId);

    /**
     * 섹션 내 학습한 문제 수 (1번 이상 푼 객관식 문제)
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM quiz_course_question q
            INNER JOIN user_review_items uri
                ON uri.content_type = 'COURSE_QUESTION'
                AND uri.content_id = q.id
                AND uri.user_id = :userId
            WHERE q.quiz_course_id = :courseId
              AND q.section_number = :sectionNumber
              AND q.question_type != 'SHORT_ANSWER'
            """, nativeQuery = true)
    long countStudiedQuestions(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber,
            @Param("userId") Long userId);

    // ────────────────────────────────────────────────────────────────────────────
    // 확률적 가중치 기반 선택 (Probabilistic Weighted Selection)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * 확률적 가중치 기반 다음 문제 선택.
     *
     * <p>
     * 가중치 로직 (확률적 선택, 엄격한 우선순위 아님):
     * </p>
     * <ul>
     * <li>신규 문제 (uri.id IS NULL): 가중치 10.0 (가장 높음)</li>
     * <li>복습 필요 (next_review_at <= NOW): 가중치 5.0</li>
     * <li>학습 완료: 가중치 1.0 / (reps + 1) (반복할수록 감소)</li>
     * </ul>
     *
     * <p>
     * 지수 분포 랜덤 공식: ORDER BY -LOG(1 - RAND()) / weight
     * </p>
     * <p>
     * 가중치가 높을수록 선택 확률이 높지만, 모든 문제가 선택될 수 있음
     * </p>
     *
     * <p>
     * <b>무한 루프 지원:</b> 섹션 완료 개념 없음, 문제가 있으면 항상 반환
     * </p>
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param userId        사용자 ID
     * @param excludeId     제외할 문제 ID (방금 푼 문제, null 허용)
     * @return 선택된 문제 (Optional - 섹션에 문제가 없을 때만 empty)
     */
    @Query(value = """
            SELECT q.*
            FROM quiz_course_question q
            LEFT JOIN user_review_items uri
                ON uri.content_type = 'COURSE_QUESTION'
                AND uri.content_id = q.id
                AND uri.user_id = :userId
            WHERE q.quiz_course_id = :courseId
              AND q.section_number = :sectionNumber
              AND q.question_type != 'SHORT_ANSWER'
              AND (:excludeId IS NULL OR q.id != :excludeId)
            ORDER BY -LN(1 - RAND()) / (
                CASE
                    WHEN uri.id IS NULL THEN 10.0
                    WHEN uri.next_review_at <= NOW() THEN 5.0
                    ELSE 1.0 / (COALESCE(uri.reps, 0) + 1)
                END
            )
            LIMIT 1
            """, nativeQuery = true)
    Optional<QuizCourseQuestion> findNextQuestionProbabilistic(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber,
            @Param("userId") Long userId,
            @Param("excludeId") Long excludeId);

    /**
     * 안 푼 문제를 우선적으로 조회하고, 모든 문제를 풀었을 경우 확률적 가중치 기반으로 다음 복습 문제를 선택합니다.
     *
     * <p>
     * 1순위: 해당 섹션에서 사용자가 한 번도 풀지 않은 문제 중 랜덤 선택 <br>
     * 2순위: 모든 문제를 풀었다면, 복습 주기(next_review_at)와 학습 횟수(reps)를 고려한 가중치 기반 선택
     * </p>
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param userId        사용자 ID
     * @return 선택된 문제 (Optional - 섹션에 문제가 아예 없는 경우에만 empty)
     */
    @Query(value = """
            SELECT combined.* FROM (
                (
                    -- [1단계] 안 푼 문제 우선 조회: 학습 기록(user_review_items)이 없는 객관식 문제 중 랜덤 추출
                    SELECT q.*, 1 AS priority FROM quiz_course_question q
                    WHERE q.quiz_course_id = :courseId
                      AND q.section_number = :sectionNumber
                      AND q.question_type != 'SHORT_ANSWER'
                      AND NOT EXISTS (
                          SELECT 1 FROM user_review_items uri
                          WHERE uri.content_id = q.id
                            AND uri.user_id = :userId
                            AND uri.content_type = 'COURSE_QUESTION'
                      )
                    ORDER BY RAND()
                    LIMIT 1
                )
                UNION ALL
                (
                    -- [2단계] 복습 문제 조회: 모든 문제를 푼 경우, 가중치(복습 시점, 반복 횟수)에 따라 추출
                    SELECT q.*, 2 AS priority FROM quiz_course_question q
                    JOIN user_review_items uri ON uri.content_id = q.id
                    WHERE q.quiz_course_id = :courseId
                      AND q.section_number = :sectionNumber
                      AND q.question_type != 'SHORT_ANSWER'
                      AND uri.user_id = :userId
                      AND uri.content_type = 'COURSE_QUESTION'
                    ORDER BY -LN(1 - RAND()) / (
                        CASE
                            -- 복습 예정 시점이 지났으면 높은 가중치(5.0) 부여
                            WHEN uri.next_review_at <= NOW() THEN 5.0
                            -- 그 외에는 학습 횟수가 적을수록 높은 확률 부여
                            ELSE 1.0 / (uri.reps + 1)
                        END
                    )
                    LIMIT 1
                )
            ) AS combined
            -- 전체 결과 중 최상위 1개만 선택 (1단계 결과가 있으면 2단계는 무시됨)
            ORDER BY combined.priority
            LIMIT 1
            """, nativeQuery = true)
    Optional<QuizCourseQuestion> findNextQuestionProbabilisticNoExclude(
            @Param("courseId") Long courseId,
            @Param("sectionNumber") Integer sectionNumber,
            @Param("userId") Long userId);

    // ────────────────────────────────────────────────────────────────────────────
    // 코스별 정답 통계 (N+1 방지용 배치 쿼리)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * 모든 코스의 전체 문제 수를 코스별로 집계한다 (객관식만).
     */
    @Query(value = """
            SELECT q.quiz_course_id AS courseId, COUNT(*) AS questionCount
            FROM quiz_course_question q
            WHERE q.question_type != 'SHORT_ANSWER'
            GROUP BY q.quiz_course_id
            """, nativeQuery = true)
    List<CourseQuestionStatsProjection> countTotalQuestionsGroupByCourse();

  /**
   * 사용자가 코스별로 1회 이상 정답을 맞힌 고유 문제 수를 집계한다 (객관식만).
   * (중복 카운트 방지: 같은 문제를 여러 번 맞혀도 1개로 집계)
   */
  @Query(value = """
      SELECT q.quiz_course_id AS courseId, COUNT(DISTINCT q.id) AS questionCount
      FROM quiz_course_question q
      JOIN user_review_items uri
          ON uri.content_type = 'COURSE_QUESTION'
          AND uri.content_id = q.id
          AND uri.user_id = :userId
      JOIN user_review_log url
          ON url.review_item_id = uri.id
          AND url.is_correct = true
      WHERE q.question_type != 'SHORT_ANSWER'
      GROUP BY q.quiz_course_id
      """, nativeQuery = true)
  List<CourseQuestionStatsProjection> countSolvedQuestionsGroupByCourse(@Param("userId") Long userId);

    /**
     * 코스별 총 복습 횟수(reps)와 오답 횟수(lapses)를 집계한다 (객관식만).
     */
    @Query(value = """
            SELECT
                q.quiz_course_id AS courseId,
                SUM(uri.reps) AS sumReps,
                SUM(uri.lapses) AS sumLapses
            FROM quiz_course_question q
            JOIN user_review_items uri
                ON uri.content_type = 'COURSE_QUESTION'
                AND uri.content_id = q.id
                AND uri.user_id = :userId
            WHERE q.question_type != 'SHORT_ANSWER'
            GROUP BY q.quiz_course_id
            """, nativeQuery = true)
    List<CourseQuestionStatsProjection> countReviewStatsGroupByCourse(@Param("userId") Long userId);

    /**
     * 사용자가 전체 코스에서 1회 이상 정답을 맞힌 고유 문제 수를 조회한다 (객관식만).
     */
    @Query(value = """
            SELECT COUNT(DISTINCT uri.content_id)
            FROM user_review_items uri
            JOIN user_review_log url
                ON url.review_item_id = uri.id
                AND url.is_correct = true
            JOIN quiz_course_question q
                ON uri.content_id = q.id
            WHERE uri.user_id = :userId
                AND uri.content_type = 'COURSE_QUESTION'
                AND q.question_type != 'SHORT_ANSWER'
            """, nativeQuery = true)
    long countTotalSolvedQuestions(@Param("userId") Long userId);
}
