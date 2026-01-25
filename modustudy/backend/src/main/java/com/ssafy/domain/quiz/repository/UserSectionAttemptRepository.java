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
 * 호출자: {@link com.ssafy.domain.quiz.service.QuizCourseService}
 *
 * 주의: QuizCourseSection은 복합키(quiz_course_id, section_number)를 사용하므로
 * JPQL에서 section.id.quizCourseId, section.id.sectionNumber로 참조해야 한다.
 */
public interface UserSectionAttemptRepository extends JpaRepository<UserSectionAttempt, Long> {

       /**
        * 사용자의 특정 섹션에 대한 진행 중인 시도를 조회한다.
        * 문제 목록도 함께 fetch join으로 조회한다.
        *
        * @param userId        사용자 ID
        * @param quizCourseId  코스 ID
        * @param sectionNumber 섹션 번호
        * @return 진행 중인 시도 (있으면)
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "LEFT JOIN FETCH a.attemptQuestions aq " +
                     "LEFT JOIN FETCH aq.question " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.section.id.sectionNumber = :sectionNumber " +
                     "AND a.status = 'IN_PROGRESS'")
       Optional<UserSectionAttempt> findInProgressAttemptWithQuestions(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * 사용자의 특정 섹션에 대한 진행 중인 시도가 있는지 확인한다.
        *
        * @param userId        사용자 ID
        * @param quizCourseId  코스 ID
        * @param sectionNumber 섹션 번호
        * @return 진행 중인 시도 존재 여부
        */
       @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
                     "FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.section.id.sectionNumber = :sectionNumber " +
                     "AND a.status = 'IN_PROGRESS'")
       boolean existsInProgressAttempt(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * 사용자의 특정 섹션에 대한 시도 기록 목록을 조회한다.
        * 최신 시도 순으로 정렬한다.
        *
        * @param userId        사용자 ID
        * @param quizCourseId  코스 ID
        * @param sectionNumber 섹션 번호
        * @return 시도 기록 목록
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.section.id.sectionNumber = :sectionNumber " +
                     "ORDER BY a.createdAt DESC")
       List<UserSectionAttempt> findByUserAndSectionOrderByCreatedAtDesc(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * 사용자의 특정 섹션에 대한 최고 점수 시도 기록을 조회한다.
        *
        * @param userId        사용자 ID
        * @param quizCourseId  코스 ID
        * @param sectionNumber 섹션 번호
        * @return 최고 점수 시도 기록
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.section.id.sectionNumber = :sectionNumber " +
                     "AND a.status = 'COMPLETED' " +
                     "ORDER BY a.score DESC, a.createdAt DESC " +
                     "LIMIT 1")
       Optional<UserSectionAttempt> findBestAttempt(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * 사용자의 특정 섹션에 대한 통과 여부를 확인한다.
        *
        * @param userId        사용자 ID
        * @param quizCourseId  코스 ID
        * @param sectionNumber 섹션 번호
        * @return 통과 여부
        */
       @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
                     "FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.section.id.sectionNumber = :sectionNumber " +
                     "AND a.isPassed = true")
       boolean existsPassedAttempt(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * 사용자의 특정 섹션에 대한 시도 횟수를 조회한다.
        *
        * @param userId        사용자 ID
        * @param quizCourseId  코스 ID
        * @param sectionNumber 섹션 번호
        * @return 시도 횟수
        */
       @Query("SELECT COUNT(a) FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.section.id.sectionNumber = :sectionNumber")
       int countAttempts(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId,
                     @Param("sectionNumber") Integer sectionNumber);

       /**
        * 사용자의 코스 내 모든 섹션의 시도 기록을 조회한다.
        *
        * @param userId       사용자 ID
        * @param quizCourseId 코스 ID
        * @return 시도 기록 목록
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId")
       List<UserSectionAttempt> findByUserIdAndCourseId(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);

       /**
        * 사용자의 코스 내 통과한 섹션 수를 조회한다.
        *
        * @param userId       사용자 ID
        * @param quizCourseId 코스 ID
        * @return 통과한 섹션 수
        */
       @Query("SELECT COUNT(DISTINCT a.section.id.sectionNumber) FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.isPassed = true")
       int countPassedSections(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);

       /**
        * 사용자의 특정 코스에 대한 제출된(COMPLETED) 시도 목록을 조회한다.
        * 섹션별 진행 상황 계산에 사용된다.
        *
        * @param userId       사용자 ID
        * @param quizCourseId 코스 ID
        * @return 제출된 시도 목록
        */
       @Query("SELECT a FROM UserSectionAttempt a " +
                     "WHERE a.user.id = :userId " +
                     "AND a.section.id.quizCourseId = :quizCourseId " +
                     "AND a.status = com.ssafy.domain.quiz.entity.enums.AttemptStatus.COMPLETED " +
                     "ORDER BY a.section.id.sectionNumber, a.completedAt DESC")
       List<UserSectionAttempt> findCompletedAttemptsByUserIdAndCourseId(
                     @Param("userId") Long userId,
                     @Param("quizCourseId") Long quizCourseId);
}
