package com.ssafy.domain.daily.controller;

import com.ssafy.domain.daily.dto.response.DailyReportResponse;
import com.ssafy.domain.daily.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/studies/{studyId}/daily-reports")
@RequiredArgsConstructor
@Slf4j
public class DailyReportController {

    private final DailyReportService dailyReportService;

    /**
     * 스터디별 데일리 리포트 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<DailyReportResponse>> getReports(
            @PathVariable Long studyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("API 호출 - 데일리 리포트 목록 조회: studyId={}, startDate={}, endDate={}",
                studyId, startDate, endDate);

        List<DailyReportResponse> response;

        if (startDate != null && endDate != null) {
            response = dailyReportService.getReportsByStudyIdAndDateRange(studyId, startDate, endDate);
        } else {
            response = dailyReportService.getReportsByStudyId(studyId);
        }

        log.info("API 응답 - 데일리 리포트 목록: count={}", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 데일리 리포트 단건 조회
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<DailyReportResponse> getReport(
            @PathVariable Long studyId,
            @PathVariable Long reportId) {

        log.info("API 호출 - 데일리 리포트 단건 조회: studyId={}, reportId={}", studyId, reportId);

        DailyReportResponse response = dailyReportService.getReportById(reportId);

        log.info("API 응답 - 데일리 리포트: reportId={}, reportDate={}", response.getId(), response.getReportDate());

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디별 특정 날짜 리포트 조회
     */
    @GetMapping("/date/{reportDate}")
    public ResponseEntity<DailyReportResponse> getReportByDate(
            @PathVariable Long studyId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {

        log.info("API 호출 - 특정 날짜 리포트 조회: studyId={}, reportDate={}", studyId, reportDate);

        DailyReportResponse response = dailyReportService.getReportByStudyIdAndDate(studyId, reportDate);

        log.info("API 응답 - 데일리 리포트: reportId={}", response.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * 데일리 리포트 단건 삭제 (스터디장만 가능)
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long studyId,
            @PathVariable Long reportId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 데일리 리포트 삭제: studyId={}, reportId={}, userId={}", studyId, reportId, userId);

        dailyReportService.deleteReport(studyId, reportId, userId);

        log.info("API 응답 - 데일리 리포트 삭제 완료: reportId={}", reportId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 스터디별 데일리 리포트 전체 삭제 (스터디장만 가능)
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllReports(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 데일리 리포트 전체 삭제: studyId={}, userId={}", studyId, userId);

        dailyReportService.deleteReportsByStudyId(studyId, userId);

        log.info("API 응답 - 데일리 리포트 전체 삭제 완료: studyId={}", studyId);

        return ResponseEntity.noContent().build();
    }
}