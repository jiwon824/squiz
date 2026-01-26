package com.ssafy.domain.daily.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.domain.daily.dto.response.DailyReportResponse;
import com.ssafy.domain.daily.entity.DailyReport;
import com.ssafy.domain.daily.repository.DailyReportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DailyReportServiceTest {

    @Autowired
    private DailyReportService dailyReportService;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Autowired
    private EntityManager entityManager;

    private Long studyId;
    private DailyReport report1;
    private DailyReport report2;
    private DailyReport report3;

    @BeforeEach
    void setUp() {
        studyId = 1L;

        report1 = dailyReportRepository.save(DailyReport.builder()
                .studyId(studyId)
                .reportDate(LocalDate.of(2025, 1, 20))
                .summary("1월 20일 데일리 리포트")
                .build());

        report2 = dailyReportRepository.save(DailyReport.builder()
                .studyId(studyId)
                .reportDate(LocalDate.of(2025, 1, 21))
                .summary("1월 21일 데일리 리포트")
                .build());

        report3 = dailyReportRepository.save(DailyReport.builder()
                .studyId(studyId)
                .reportDate(LocalDate.of(2025, 1, 25))
                .summary("1월 25일 데일리 리포트")
                .build());

        dailyReportRepository.flush();
    }

    @Test
    @DisplayName("스터디별 데일리 리포트 목록 조회 - 최신순 정렬")
    void getReportsByStudyId_Success() {
        // when
        List<DailyReportResponse> reports = dailyReportService.getReportsByStudyId(studyId);

        // then
        assertThat(reports).hasSize(3);
        assertThat(reports.get(0).getReportDate()).isEqualTo(LocalDate.of(2025, 1, 25));
        assertThat(reports.get(1).getReportDate()).isEqualTo(LocalDate.of(2025, 1, 21));
        assertThat(reports.get(2).getReportDate()).isEqualTo(LocalDate.of(2025, 1, 20));
    }

    @Test
    @DisplayName("존재하지 않는 스터디 ID로 조회 시 빈 목록 반환")
    void getReportsByStudyId_EmptyList() {
        // when
        List<DailyReportResponse> reports = dailyReportService.getReportsByStudyId(999L);

        // then
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("리포트 단건 조회 성공")
    void getReportById_Success() {
        // when
        DailyReportResponse response = dailyReportService.getReportById(report1.getId());

        // then
        assertThat(response.getId()).isEqualTo(report1.getId());
        assertThat(response.getReportDate()).isEqualTo(LocalDate.of(2025, 1, 20));
        assertThat(response.getSummary()).isEqualTo("1월 20일 데일리 리포트");
    }

    @Test
    @DisplayName("존재하지 않는 리포트 ID로 조회 시 예외 발생")
    void getReportById_NotFound() {
        // when & then
        assertThatThrownBy(() -> dailyReportService.getReportById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("데일리 리포트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("스터디별 특정 날짜 리포트 조회 성공")
    void getReportByStudyIdAndDate_Success() {
        // when
        DailyReportResponse response = dailyReportService
                .getReportByStudyIdAndDate(studyId, LocalDate.of(2025, 1, 21));

        // then
        assertThat(response.getSummary()).isEqualTo("1월 21일 데일리 리포트");
    }

    @Test
    @DisplayName("존재하지 않는 날짜로 조회 시 예외 발생")
    void getReportByStudyIdAndDate_NotFound() {
        // when & then
        assertThatThrownBy(() -> dailyReportService
                .getReportByStudyIdAndDate(studyId, LocalDate.of(2025, 1, 30)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("해당 날짜의 데일리 리포트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("스터디별 기간 내 리포트 조회 - 최신순 정렬")
    void getReportsByStudyIdAndDateRange_Success() {
        // when
        List<DailyReportResponse> reports = dailyReportService
                .getReportsByStudyIdAndDateRange(studyId, LocalDate.of(2025, 1, 20), LocalDate.of(2025, 1, 22));

        // then
        assertThat(reports).hasSize(2);
        assertThat(reports.get(0).getReportDate()).isEqualTo(LocalDate.of(2025, 1, 21));
        assertThat(reports.get(1).getReportDate()).isEqualTo(LocalDate.of(2025, 1, 20));
    }

    @Test
    @DisplayName("기간 내 리포트가 없으면 빈 목록 반환")
    void getReportsByStudyIdAndDateRange_EmptyList() {
        // when
        List<DailyReportResponse> reports = dailyReportService
                .getReportsByStudyIdAndDateRange(studyId, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28));

        // then
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("리포트 단건 삭제 성공")
    void deleteReport_Success() {
        // given
        Long reportId = report1.getId();

        // when
        dailyReportService.deleteReport(reportId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThatThrownBy(() -> dailyReportService.getReportById(reportId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 리포트 삭제 시 예외 발생")
    void deleteReport_NotFound() {
        // when & then
        assertThatThrownBy(() -> dailyReportService.deleteReport(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("데일리 리포트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("스터디별 리포트 전체 삭제 성공")
    void deleteReportsByStudyId_Success() {
        // when
        dailyReportService.deleteReportsByStudyId(studyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<DailyReportResponse> reports = dailyReportService.getReportsByStudyId(studyId);
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("다른 스터디 리포트는 삭제되지 않음")
    void deleteReportsByStudyId_OnlyTargetStudy() {
        // given
        Long otherStudyId = 2L;
        dailyReportRepository.save(DailyReport.builder()
                .studyId(otherStudyId)
                .reportDate(LocalDate.of(2025, 1, 20))
                .summary("다른 스터디 리포트")
                .build());
        dailyReportRepository.flush();

        // when
        dailyReportService.deleteReportsByStudyId(studyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<DailyReportResponse> remainingReports = dailyReportService.getReportsByStudyId(otherStudyId);
        assertThat(remainingReports).hasSize(1);
    }
}