package com.ssafy.domain.daily.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class DailyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private Format format;

    @BeforeEach
    void setUp() {
        leaderId = 100L;

        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("Java")
                .sortOrder(1)
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
    @DisplayName("스터디별 데일리 리포트 목록 조회 성공")
    void getReports_Success() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports", studyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].reportDate").value("2025-01-25"))
                .andExpect(jsonPath("$[1].reportDate").value("2025-01-21"))
                .andExpect(jsonPath("$[2].reportDate").value("2025-01-20"));
    }

    @Test
    @DisplayName("기간별 데일리 리포트 목록 조회 성공")
    void getReports_WithDateRange_Success() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports", studyId)
                        .param("startDate", "2025-01-20")
                        .param("endDate", "2025-01-22"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reportDate").value("2025-01-21"))
                .andExpect(jsonPath("$[1].reportDate").value("2025-01-20"));
    }

    @Test
    @DisplayName("데일리 리포트 단건 조회 성공")
    void getReport_Success() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports/{reportId}", studyId, report1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report1.getId()))
                .andExpect(jsonPath("$.reportDate").value("2025-01-20"))
                .andExpect(jsonPath("$.summary").value("1월 20일 데일리 리포트"));
    }

    @Test
    @DisplayName("특정 날짜 리포트 조회 성공")
    void getReportByDate_Success() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports/date/{reportDate}", studyId, "2025-01-21"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportDate").value("2025-01-21"))
                .andExpect(jsonPath("$.summary").value("1월 21일 데일리 리포트"));
    }

    @Test
    @DisplayName("스터디장이 데일리 리포트 단건 삭제 성공")
    void deleteReport_Success() throws Exception {
        Long reportId = report1.getId();

        mockMvc.perform(delete("/api/v1/studies/{studyId}/daily-reports/{reportId}", studyId, reportId)
                        .header("User-Id", leaderId))
                .andDo(print())
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports/{reportId}", studyId, reportId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("스터디장이 아닌 사용자가 삭제 시 403 반환")
    void deleteReport_Forbidden() throws Exception {
        Long otherUserId = 999L;

        mockMvc.perform(delete("/api/v1/studies/{studyId}/daily-reports/{reportId}", studyId, report1.getId())
                        .header("User-Id", otherUserId))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("스터디장이 데일리 리포트 전체 삭제 성공")
    void deleteAllReports_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/studies/{studyId}/daily-reports", studyId)
                        .header("User-Id", leaderId))
                .andDo(print())
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports", studyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("스터디장이 아닌 사용자가 전체 삭제 시 403 반환")
    void deleteAllReports_Forbidden() throws Exception {
        Long otherUserId = 999L;

        mockMvc.perform(delete("/api/v1/studies/{studyId}/daily-reports", studyId)
                        .header("User-Id", otherUserId))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 리포트 조회 시 404 반환")
    void getReport_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports/{reportId}", studyId, 999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 날짜 리포트 조회 시 404 반환")
    void getReportByDate_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/studies/{studyId}/daily-reports/date/{reportDate}", studyId, "2025-01-30"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 스터디에서 삭제 시 404 반환")
    void deleteReport_StudyNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/studies/{studyId}/daily-reports/{reportId}", 999L, report1.getId())
                        .header("User-Id", leaderId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}