package com.ssafy.domain.calendar.repository;

import com.ssafy.domain.calendar.entity.CalendarWatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarWatchRepository extends JpaRepository<CalendarWatch, Long> {

    Optional<CalendarWatch> findByUserId(Long userId);

    Optional<CalendarWatch> findByChannelId(String channelId);

    @Query("SELECT cw FROM CalendarWatch cw WHERE cw.expiresAt < :threshold")
    List<CalendarWatch> findExpiringSoon(@Param("threshold") LocalDateTime threshold);

    void deleteByUserId(Long userId);
}
