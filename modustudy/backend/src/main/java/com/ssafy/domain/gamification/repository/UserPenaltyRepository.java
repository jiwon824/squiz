package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.UserPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    List<UserPenalty> findByUserIdAndIsActiveTrue(Long userId);

    List<UserPenalty> findByUserIdAndIsActiveFalse(Long userId);

    @Query("SELECT up FROM UserPenalty up " +
            "JOIN FETCH up.penalty " +
            "LEFT JOIN FETCH up.study " +
            "WHERE up.user.id = :userId AND up.isActive = :isActive " +
            "ORDER BY up.grantedAt DESC")
    List<UserPenalty> findByUserIdAndIsActiveWithDetails(@Param("userId") Long userId, @Param("isActive") boolean isActive);
}