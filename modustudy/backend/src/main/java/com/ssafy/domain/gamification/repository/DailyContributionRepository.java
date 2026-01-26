package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.DailyContribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyContributionRepository extends JpaRepository<DailyContribution, Long> {

}
