package com.ssafy.domain.calendar.repository;

import com.ssafy.domain.calendar.entity.PersonalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 개인 일정 Repository
 */
 @Repository
 public interface PersonalScheduleRepository extends JpaRepository<PersonalSchedule, Long> {

    /**
     * 특정 사용자의 날짜 범위 내 일정 조회
     */
    @Query("SELECT ps FROM PersonalSchedule ps " +
           "WHERE ps.userId = :userId " +
           "AND ps.startDate >= :startDate " +
           "AND ps.startDate <= :endDate " +
           "ORDER BY ps.startDate, ps.startTime")
    List<PersonalSchedule> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 사용자의 특정 일정 조회
     */
    Optional<PersonalSchedule> findByIdAndUserId(Long id, Long userId);

    /**
     * 특정 사용자의 모든 일정 조회
     */
    List<PersonalSchedule> findByUserIdOrderByStartDateAscStartTimeAsc(Long userId);

    /**
     * Google Event ID로 조회
     */
    Optional<PersonalSchedule> findByGoogleEventId(String googleEventId);
}
