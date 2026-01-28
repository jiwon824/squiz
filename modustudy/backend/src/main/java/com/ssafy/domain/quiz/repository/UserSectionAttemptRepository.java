package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserSectionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 섹션 시도 기록 레포지토리.
 *
 * QuizCourseSection을 참조하기 위해 복합 FK (quiz_course_id, section_number)를 사용
 */
public interface UserSectionAttemptRepository extends JpaRepository<UserSectionAttempt, Long> {

       /**
        * Find all in-progress attempts for user and section with questions.
        * Used to detect and handle duplicates.
        * [DISTINCT 키워드 추가]
        * - 이유: LEFT JOIN FETCH를 사용하여 컬렉션(문제 목록)을 함께 조회할 때,
        * SQL Join의 특성상 하나의 시도(Attempt) 데이터가 여러 개의 문항 수만큼 중복되어 조회되는
        * 카테시안 곱(Cartesian Product) 현상이 발생합니다.
        * - 효과: DISTINCT를 추가함으로써 DB에서 가져온 결과물 중 중복된 시도 객체를 제거하여
        * 정확히 1개의 시도 객체만 리스트에 담기도록 보장합니다.
        * 이는 푼 문제 수가 비정상적으로 합산되거나 500 에러가 발생하는 원인을 차단합니다.
        */
       @Query("SELECT DISTINCT a FROM UserSectionAttempt a " +
                     "LEFT JOIN FETCH a.attemptQuestions aq " +
                     "LEFT JOIN FETCH aq.question " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber " +
                     "AND a.status = 'IN_PROGRESS' " +
                     "ORDER BY a.createdAt DESC")
       List<UserSectionAttempt> findAllInProgressAttemptsWithQuestions(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Find in-progress attempt for user and section with questions.
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "LEFT JOIN FETCH a.attemptQuestions aq " +
                     "LEFT JOIN FETCH aq.question " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber " +
                     "AND a.status = 'IN_PROGRESS'")
       Optional<UserSectionAttempt> findInProgressAttemptWithQuestions(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Check if in-progress attempt exists.
        */
       @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
                     "FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber " +
                     "AND a.status = 'IN_PROGRESS'")
       boolean existsInProgressAttempt(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Find attempts for user and section ordered by created date.
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber " +
                     "ORDER BY a.createdAt DESC")
       List<UserSectionAttempt> findByUserAndSectionOrderByCreatedAtDesc(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Find best completed attempt for user and section.
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber " +
                     "AND a.status = 'COMPLETED' " +
                     "ORDER BY a.score DESC, a.createdAt DESC " +
                     "LIMIT 1")
       Optional<UserSectionAttempt> findBestAttempt(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Check if user passed the section.
        */
       @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
                     "FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber " +
                     "AND a.isPassed = true")
       boolean existsPassedAttempt(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Count attempts for user and section.
        */
       @Query("SELECT COUNT(a) FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.section.sectionNumber = :sectionNumber")
       int countAttempts(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * Find all attempts for user in a course.
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId")
       List<UserSectionAttempt> findByUserIdAndCourseId(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);

       /**
        * Count passed sections for user in a course.
        */
       @Query("SELECT COUNT(DISTINCT a.section.sectionNumber) FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.isPassed = true")
       int countPassedSections(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);

       /**
        * Find completed attempts for user in a course.
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.status = com.ssafy.domain.quiz.entity.enums.AttemptStatus.SUBMITTED " +
                     "ORDER BY a.section.sectionNumber, a.completedAt DESC")
       List<UserSectionAttempt> findCompletedAttemptsByUserIdAndCourseId(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);

       /**
        * Find in-progress attempts for user in a course (for section list display).
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.quizCourseId = :quizCourseId " +
                     "AND a.status = 'IN_PROGRESS' " +
                     "ORDER BY a.createdAt DESC")
       List<UserSectionAttempt> findInProgressAttemptsByUserIdAndCourseId(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);

       /**
        * Find a specific attempt by ID with questions (for resume endpoint).
        */
       @Query("SELECT DISTINCT a FROM UserSectionAttempt a " +
                     "LEFT JOIN FETCH a.attemptQuestions aq " +
                     "LEFT JOIN FETCH aq.question " +
                     "WHERE a.id = :attemptId")
       Optional<UserSectionAttempt> findByIdWithQuestions(@Param("attemptId") Long attemptId);
}
