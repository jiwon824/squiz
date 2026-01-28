package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.UserSectionAttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 섹션 시도 문제 레포지토리.
 */
public interface UserSectionAttemptQuestionRepository extends JpaRepository<UserSectionAttemptQuestion, Long> {

       /**
        * 시도 ID로 문제 목록을 order_index 순서로 조회한다.
        * 문제 정보도 함께 fetch join으로 조회한다.
        *
        * @param attemptId 시도 ID
        * @return 문제 목록 (순서대로)
        */
       @Query("SELECT aq FROM UserSectionAttemptQuestion aq " +
                     "JOIN FETCH aq.question q " +
                     "WHERE aq.attempt.id = :attemptId " +
                     "ORDER BY aq.orderIndex ASC")
       List<UserSectionAttemptQuestion> findByAttemptIdWithQuestionOrderByOrderIndex(
                     @Param("attemptId") Long attemptId);

       /**
        * 시도 ID와 문제 ID로 특정 문제를 조회한다.
        * Attempt와 User도 함께 조회하여 권한 체크 및 Optimistic Lock 처리 시 N+1 방지.
        *
        * @param attemptId  시도 ID
        * @param questionId 문제 ID
        * @return 시도 문제
        */
       @Query("SELECT aq FROM UserSectionAttemptQuestion aq " +
                     "JOIN FETCH aq.attempt a " +
                     "JOIN FETCH a.user " +
                     "WHERE aq.attempt.id = :attemptId AND aq.question.id = :questionId")
       Optional<UserSectionAttemptQuestion> findByAttemptIdAndQuestionId(
                     @Param("attemptId") Long attemptId,
                     @Param("questionId") Long questionId);

       /**
        * 시도 ID와 order_index로 특정 문제를 조회한다.
        *
        * @param attemptId  시도 ID
        * @param orderIndex 문제 순서
        * @return 시도 문제
        */
       @Query("SELECT aq FROM UserSectionAttemptQuestion aq " +
                     "JOIN FETCH aq.question " +
                     "WHERE aq.attempt.id = :attemptId AND aq.orderIndex = :orderIndex")
       Optional<UserSectionAttemptQuestion> findByAttemptIdAndOrderIndex(
                     @Param("attemptId") Long attemptId,
                     @Param("orderIndex") Integer orderIndex);

       /**
        * 시도의 답변 완료 개수를 조회한다.
        *
        * @param attemptId 시도 ID
        * @return 답변 완료 개수
        */
       @Query("SELECT COUNT(aq) FROM UserSectionAttemptQuestion aq " +
                     "WHERE aq.attempt.id = :attemptId AND aq.userAnswer IS NOT NULL")
       int countAnsweredByAttemptId(@Param("attemptId") Long attemptId);
}
