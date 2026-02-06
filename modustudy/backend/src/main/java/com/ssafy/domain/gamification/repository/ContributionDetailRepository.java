package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.ContributionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ContributionDetailRepository extends JpaRepository<ContributionDetail, Long> {

    List<ContributionDetail> findByUserIdAndContributionDateOrderByCreatedAtDesc(Long userId, LocalDate contributionDate);
}
