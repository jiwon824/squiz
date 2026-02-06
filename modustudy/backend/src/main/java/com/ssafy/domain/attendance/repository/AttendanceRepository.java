package com.ssafy.domain.attendance.repository;

import com.ssafy.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    @Query("SELECT a FROM Attendance a WHERE a.session.id = :sessionId AND a.user.id = :userId")
    Optional<Attendance> findBySessionIdAndUserId(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId
    );

    @Query("SELECT a FROM Attendance a WHERE a.session.id = :sessionId")
    List<Attendance> findBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT a FROM Attendance a " +
            "JOIN a.session s " +
            "WHERE a.user.id = :userId " +
            "AND s.scheduledAt BETWEEN :start AND :end")
    List<Attendance> findByUserIdAndSessionScheduledAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
