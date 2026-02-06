package com.ssafy.domain.daily.repository;

import com.ssafy.domain.daily.entity.DailyReport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyReportRepositoryTest {

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Autowired
    private EntityManager entityManager;

    // DailyReport는 studyId만 FK로 가지는데,
    // ON DELETE CASCADE이므로 테스트에서는 Study 없이 studyId만 사용
    // 실제 환경에서는 Study가 존재해야 함
    private Long studyId;
    private DailyReport report1;
    private DailyReport report2;
    private DailyReport report3;

    @BeforeEach
    void setUp() {
        // 테스트용 studyId (실제 환경에서는 Study 엔티티 생성 필요)
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
    @DisplayName("스터디별 데일리 리포트 목록 조회")
    void findByStudyId_Success() {
        // when
        List<DailyReport> reports = dailyReportRepository.findByStudyId(studyId);

        // then
        assertThat(reports).hasSize(3);
        assertThat(reports).extracting(DailyReport::getStudyId)
                .containsOnly(studyId);
    }

    @Test
    @DisplayName("존재하지 않는 스터디 ID로 조회 시 빈 목록 반환")
    void findByStudyId_NotFound() {
        // when
        List<DailyReport> reports = dailyReportRepository.findByStudyId(999L);

        // then
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("스터디별 특정 날짜 리포트 조회")
    void findByStudyIdAndReportDate_Success() {
        // when
        Optional<DailyReport> result = dailyReportRepository
                .findByStudyIdAndReportDate(studyId, LocalDate.of(2025, 1, 21));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSummary()).isEqualTo("1월 21일 데일리 리포트");
    }

    @Test
    @DisplayName("존재하지 않는 날짜로 조회 시 빈 Optional 반환")
    void findByStudyIdAndReportDate_NotFound() {
        // when
        Optional<DailyReport> result = dailyReportRepository
                .findByStudyIdAndReportDate(studyId, LocalDate.of(2025, 1, 30));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스터디별 기간 내 리포트 조회")
    void findByStudyIdAndReportDateBetween_Success() {
        // when
        List<DailyReport> reports = dailyReportRepository
                .findByStudyIdAndReportDateBetween(
                        studyId,
                        LocalDate.of(2025, 1, 20),
                        LocalDate.of(2025, 1, 22)
                );

        // then
        assertThat(reports).hasSize(2);
        assertThat(reports).extracting(DailyReport::getReportDate)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2025, 1, 20),
                        LocalDate.of(2025, 1, 21)
                );
    }

    @Test
    @DisplayName("기간 내 리포트가 없으면 빈 목록 반환")
    void findByStudyIdAndReportDateBetween_NotFound() {
        // when
        List<DailyReport> reports = dailyReportRepository
                .findByStudyIdAndReportDateBetween(
                        studyId,
                        LocalDate.of(2025, 2, 1),
                        LocalDate.of(2025, 2, 28)
                );

        // then
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("스터디별 리포트 전체 삭제")
    void deleteByStudyId_Success() {
        // when
        dailyReportRepository.deleteByStudyId(studyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<DailyReport> reports = dailyReportRepository.findByStudyId(studyId);
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("다른 스터디 리포트는 삭제되지 않음")
    void deleteByStudyId_OnlyTargetStudy() {
        // given
        Long otherStudyId = 2L;
        dailyReportRepository.save(DailyReport.builder()
                .studyId(otherStudyId)
                .reportDate(LocalDate.of(2025, 1, 20))
                .summary("다른 스터디 리포트")
                .build());
        dailyReportRepository.flush();

        // when
        dailyReportRepository.deleteByStudyId(studyId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<DailyReport> remainingReports = dailyReportRepository.findByStudyId(otherStudyId);
        assertThat(remainingReports).hasSize(1);
    }

    @Test
    @DisplayName("리포트 단건 조회")
    void findById_Success() {
        // when
        Optional<DailyReport> result = dailyReportRepository.findById(report1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getReportDate()).isEqualTo(LocalDate.of(2025, 1, 20));
    }

    @Test
    @DisplayName("리포트 단건 삭제")
    void deleteById_Success() {
        // given
        Long reportId = report1.getId();

        // when
        dailyReportRepository.deleteById(reportId);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<DailyReport> result = dailyReportRepository.findById(reportId);
        assertThat(result).isEmpty();
    }
}
