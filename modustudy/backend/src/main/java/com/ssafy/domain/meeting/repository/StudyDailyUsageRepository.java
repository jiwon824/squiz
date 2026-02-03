package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.StudyDailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface StudyDailyUsageRepository extends JpaRepository<StudyDailyUsage, Long> {

    /**
     * 스터디의 특정 날짜 사용량 조회
     */
    Optional<StudyDailyUsage> findByStudyIdAndUsageDate(Long studyId, LocalDate usageDate);

    /**
     * 스터디의 오늘 사용량 조회 (없으면 생성용)
     */
    @Query("SELECT u FROM StudyDailyUsage u WHERE u.study.id = :studyId AND u.usageDate = CURRENT_DATE")
    Optional<StudyDailyUsage> findTodayUsage(@Param("studyId") Long studyId);

    /**
     * 오래된 사용량 기록 삭제 (30일 이전)
     */
    @Query("DELETE FROM StudyDailyUsage u WHERE u.usageDate < :cutoffDate")
    void deleteOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
}
