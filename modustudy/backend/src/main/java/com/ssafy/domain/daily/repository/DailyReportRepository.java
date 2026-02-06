package com.ssafy.domain.daily.repository;

import com.ssafy.domain.daily.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    // 스터디별 데일리 리포트 목록 조회
    List<DailyReport> findByStudyId(Long studyId);

    // 스터디별 특정 날짜 리포트 조회
    Optional<DailyReport> findByStudyIdAndReportDate(Long studyId, LocalDate reportDate);

    // 스터디별 기간 내 리포트 조회
    List<DailyReport> findByStudyIdAndReportDateBetween(Long studyId, LocalDate startDate, LocalDate endDate);

    // 스터디별 리포트 전체 삭제
    void deleteByStudyId(Long studyId);
}
