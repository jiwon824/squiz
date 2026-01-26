package com.ssafy.domain.daily.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.daily.dto.response.DailyReportResponse;
import com.ssafy.domain.daily.entity.DailyReport;
import com.ssafy.domain.daily.repository.DailyReportRepository;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
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
    private StudyRepository studyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private Study study;
    private Long studyId;
    private Long leaderId;
    private DailyReport report1;
    private DailyReport report2;
    private DailyReport report3;
    private Topic topic;
    private Topic topic2;
    private Format format;

    @BeforeEach
    void setUp() {
        leaderId = 100L;

        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("Java")
                .sortOrder(1)
                .build());
        topic2 = topicRepository.save(Topic.builder()
                .name("Python")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. 스터디 생성 (필수 필드 모두 포함)
        study = studyRepository.save(Study.builder()
                .leaderId(leaderId)
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        studyId = study.getId();

        // 데일리 리포트 생성
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
    @DisplayName("스터디장이 리포트 단건 삭제 성공")
    void deleteReport_Success() {
        // given
        Long reportId = report1.getId();

        // when
        dailyReportService.deleteReport(studyId, reportId, leaderId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThatThrownBy(() -> dailyReportService.getReportById(reportId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("스터디장이 아닌 사용자가 삭제 시 예외 발생")
    void deleteReport_NotLeader() {
        // given
        Long otherUserId = 999L;

        // when & then
        assertThatThrownBy(() -> dailyReportService.deleteReport(studyId, report1.getId(), otherUserId))
                .isInstanceOf(StudyException.NotStudyLeaderException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 리포트 삭제 시 예외 발생")
    void deleteReport_NotFound() {
        // when & then
        assertThatThrownBy(() -> dailyReportService.deleteReport(studyId, 999L, leaderId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("데일리 리포트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 스터디에서 삭제 시 예외 발생")
    void deleteReport_StudyNotFound() {
        // when & then
        assertThatThrownBy(() -> dailyReportService.deleteReport(999L, report1.getId(), leaderId))
                .isInstanceOf(StudyException.StudyNotFoundException.class);
    }

    @Test
    @DisplayName("스터디장이 리포트 전체 삭제 성공")
    void deleteReportsByStudyId_Success() {
        // when
        dailyReportService.deleteReportsByStudyId(studyId, leaderId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<DailyReportResponse> reports = dailyReportService.getReportsByStudyId(studyId);
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("스터디장이 아닌 사용자가 전체 삭제 시 예외 발생")
    void deleteReportsByStudyId_NotLeader() {
        // given
        Long otherUserId = 999L;

        // when & then
        assertThatThrownBy(() -> dailyReportService.deleteReportsByStudyId(studyId, otherUserId))
                .isInstanceOf(StudyException.NotStudyLeaderException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    @DisplayName("다른 스터디 리포트는 삭제되지 않음")
    void deleteReportsByStudyId_OnlyTargetStudy() {
        // given
        Study otherStudy = studyRepository.save(Study.builder()
                .leaderId(200L)
                .name("다른 스터디")
                .topic(topic2)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        dailyReportRepository.save(DailyReport.builder()
                .studyId(otherStudy.getId())
                .reportDate(LocalDate.of(2025, 1, 20))
                .summary("다른 스터디 리포트")
                .build());
        dailyReportRepository.flush();

        // when
        dailyReportService.deleteReportsByStudyId(studyId, leaderId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<DailyReportResponse> remainingReports = dailyReportService.getReportsByStudyId(otherStudy.getId());
        assertThat(remainingReports).hasSize(1);
    }
}