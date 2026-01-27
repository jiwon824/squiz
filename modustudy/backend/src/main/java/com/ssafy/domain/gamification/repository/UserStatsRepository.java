package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    Optional<UserStats> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}