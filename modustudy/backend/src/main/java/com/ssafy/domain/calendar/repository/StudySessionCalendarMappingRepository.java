package com.ssafy.domain.calendar.repository;

import com.ssafy.domain.calendar.entity.StudySessionCalendarMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudySessionCalendarMappingRepository extends JpaRepository<StudySessionCalendarMapping, Long> {

    Optional<StudySessionCalendarMapping> findBySessionIdAndUserId(Long sessionId, Long userId);

    List<StudySessionCalendarMapping> findBySessionId(Long sessionId);

    List<StudySessionCalendarMapping> findByUserId(Long userId);

    void deleteBySessionId(Long sessionId);

    void deleteBySessionIdAndUserId(Long sessionId, Long userId);

    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
}
