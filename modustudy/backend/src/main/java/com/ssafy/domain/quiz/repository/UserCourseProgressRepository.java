package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserCourseProgress;
import com.ssafy.domain.quiz.entity.UserCourseProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 코스 진행 상황 레포지토리.
 *
 * 호출자: {@link com.ssafy.domain.quiz.service.QuizCourseService}
 */
 public interface UserCourseProgressRepository extends JpaRepository<UserCourseProgress, UserCourseProgressId> {

    /**
     * 사용자 ID와 코스 ID로 진행 상황을 조회한다.
     *
     * @param userId 사용자 ID
     * @param courseId 코스 ID
     * @return 진행 상황
     */
    Optional<UserCourseProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * 사용자 ID로 모든 진행 상황을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 진행 상황 목록
     */
    List<UserCourseProgress> findByUserId(Long userId);

    /**
     * 사용자 ID로 진행 중인 코스 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 진행 중인 코스 진행 상황 목록
     */
    @Query("SELECT p FROM UserCourseProgress p " +
           "WHERE p.userId = :userId AND p.isCompleted = false")
    List<UserCourseProgress> findInProgressByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID로 완료한 코스 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 완료한 코스 진행 상황 목록
     */
    @Query("SELECT p FROM UserCourseProgress p " +
           "WHERE p.userId = :userId AND p.isCompleted = true")
    List<UserCourseProgress> findCompletedByUserId(@Param("userId") Long userId);

    List<UserCourseProgress> findByCourseId(Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
