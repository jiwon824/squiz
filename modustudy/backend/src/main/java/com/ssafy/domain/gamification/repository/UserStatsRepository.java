package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
}
