package com.ssafy.domain.daily.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.domain.daily.dto.response.DailyReportResponse;
import com.ssafy.domain.daily.entity.DailyReport;
import com.ssafy.domain.daily.repository.DailyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;

    // 스터디별 데일리 리포트 목록 조회 (최신순)
    public List<DailyReportResponse> getReportsByStudyId(Long studyId) {
        return dailyReportRepository.findByStudyId(studyId).stream()
                .sorted(Comparator.comparing(DailyReport::getReportDate).reversed())
                .map(DailyReportResponse::from)
                .toList();
    }

    // 리포트 단건 조회
    public DailyReportResponse getReportById(Long reportId) {
        DailyReport report = dailyReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("DAILY_REPORT_NOT_FOUND", "데일리 리포트를 찾을 수 없습니다."));
        return DailyReportResponse.from(report);
    }

    // 스터디별 특정 날짜 리포트 조회
    public DailyReportResponse getReportByStudyIdAndDate(Long studyId, LocalDate reportDate) {
        DailyReport report = dailyReportRepository.findByStudyIdAndReportDate(studyId, reportDate)
                .orElseThrow(() -> new NotFoundException("DAILY_REPORT_NOT_FOUND", "해당 날짜의 데일리 리포트를 찾을 수 없습니다."));
        return DailyReportResponse.from(report);
    }

    // 스터디별 기간 내 리포트 조회 (최신순)
    public List<DailyReportResponse> getReportsByStudyIdAndDateRange(Long studyId, LocalDate startDate, LocalDate endDate) {
        return dailyReportRepository.findByStudyIdAndReportDateBetween(studyId, startDate, endDate).stream()
                .sorted(Comparator.comparing(DailyReport::getReportDate).reversed())
                .map(DailyReportResponse::from)
                .toList();
    }

    // 리포트 단건 삭제
    @Transactional
    public void deleteReport(Long reportId) {
        if (!dailyReportRepository.existsById(reportId)) {
            throw new NotFoundException("DAILY_REPORT_NOT_FOUND", "데일리 리포트를 찾을 수 없습니다.");
        }
        dailyReportRepository.deleteById(reportId);
    }

    // 스터디별 리포트 전체 삭제
    @Transactional
    public void deleteReportsByStudyId(Long studyId) {
        dailyReportRepository.deleteByStudyId(studyId);
    }
}