package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.DailyContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyContributionRepository extends JpaRepository<DailyContribution, Long> {

    Optional<DailyContribution> findByUserIdAndContributionDate(Long userId, LocalDate contributionDate);

    List<DailyContribution> findByUserIdAndContributionDateBetweenOrderByContributionDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate
    );

    @Query("SELECT dc FROM DailyContribution dc " +
            "WHERE dc.user.id = :userId " +
            "AND YEAR(dc.contributionDate) = :year " +
            "ORDER BY dc.contributionDate ASC")
    List<DailyContribution> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);
}