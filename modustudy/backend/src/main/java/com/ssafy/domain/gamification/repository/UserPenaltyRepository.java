package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.UserPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    /**
     * 사용자의 활성 패널티 조회 (패널티, 스터디 정보 포함)
     */
    @Query("SELECT up FROM UserPenalty up " +
            "JOIN FETCH up.penalty " +
            "LEFT JOIN FETCH up.study " +
            "WHERE up.user.id = :userId AND up.isActive = true")
    List<UserPenalty> findByUserIdAndIsActiveTrueWithDetails(Long userId);

    /**
     * 사용자의 해소된 패널티 조회
     */
    @Query("SELECT up FROM UserPenalty up " +
            "JOIN FETCH up.penalty " +
            "LEFT JOIN FETCH up.study " +
            "WHERE up.user.id = :userId AND up.isActive = false")
    List<UserPenalty> findByUserIdAndIsActiveFalseWithDetails(Long userId);

    /**
     * 특정 스터디의 활성 패널티 조회
     */
    @Query("SELECT up FROM UserPenalty up " +
            "JOIN FETCH up.penalty " +
            "WHERE up.user.id = :userId AND up.study.id = :studyId AND up.isActive = true")
    List<UserPenalty> findByUserIdAndStudyIdAndIsActiveTrueWithPenalty(Long userId, Long studyId);

    /**
     * 특정 코드의 활성 패널티 조회
     */
    @Query("SELECT up FROM UserPenalty up " +
            "JOIN FETCH up.penalty p " +
            "WHERE up.user.id = :userId AND p.code = :penaltyCode AND up.isActive = true")
    Optional<UserPenalty> findByUserIdAndPenaltyCodeAndIsActiveTrue(Long userId, String penaltyCode);
}
