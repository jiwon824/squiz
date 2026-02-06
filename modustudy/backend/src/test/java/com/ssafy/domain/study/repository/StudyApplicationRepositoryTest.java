package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.ApplicationStatus;
import com.ssafy.domain.study.entity.StudyApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * StudyApplicationRepository 테스트
 */
 @SpringBootTest
 @Transactional
 class StudyApplicationRepositoryTest {

    @Autowired
    private StudyApplicationRepository applicationRepository;

    private StudyApplication pendingApp1;
    private StudyApplication pendingApp2;
    private StudyApplication approvedApp;
    private StudyApplication rejectedApp;

    @BeforeEach
    void setUp() {
        // 스터디 1의 신청들
        pendingApp1 = StudyApplication.builder()
                .studyId(1L)
                .userId(10L)
                .message("열심히 하겠습니다!")
                .matchingScore(new BigDecimal("85.5"))
                .status(ApplicationStatus.PENDING)
                .build();
        pendingApp1 = applicationRepository.save(pendingApp1);

        pendingApp2 = StudyApplication.builder()
                .studyId(1L)
                .userId(11L)
                .message("잘 부탁드립니다.")
                .matchingScore(new BigDecimal("90.0"))
                .status(ApplicationStatus.PENDING)
                .build();
        pendingApp2 = applicationRepository.save(pendingApp2);

        approvedApp = StudyApplication.builder()
                .studyId(1L)
                .userId(12L)
                .message("함께 성장하고 싶습니다.")
                .matchingScore(new BigDecimal("95.0"))
                .status(ApplicationStatus.APPROVED)
                .build();
        approvedApp.approve();
        approvedApp = applicationRepository.save(approvedApp);

        // 스터디 2의 신청
        rejectedApp = StudyApplication.builder()
                .studyId(2L)
                .userId(10L)
                .message("지원합니다.")
                .matchingScore(new BigDecimal("70.0"))
                .status(ApplicationStatus.REJECTED)
                .rejectedReason("정원 초과")
                .build();
        rejectedApp.reject("정원 초과");
        rejectedApp = applicationRepository.save(rejectedApp);
    }

    // ============================================================
    // 스터디별 신청 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 전체 신청 조회 - 페이징")
    void findByStudyId_Paging_Success() {
        // given
        Long studyId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyApplication> result = applicationRepository.findByStudyId(studyId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("스터디별 상태별 신청 조회 - PENDING")
    void findByStudyIdAndStatus_Pending_Success() {
        // given
        Long studyId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyApplication> result = applicationRepository.findByStudyIdAndStatus(
                studyId, ApplicationStatus.PENDING, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(app -> app.getStatus() == ApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("스터디별 상태별 신청 조회 - APPROVED")
    void findByStudyIdAndStatus_Approved_Success() {
        // given
        Long studyId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyApplication> result = applicationRepository.findByStudyIdAndStatus(
                studyId, ApplicationStatus.APPROVED, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("스터디별 신청 목록 조회 - 리스트")
    void findByStudyId_List_Success() {
        // given
        Long studyId = 1L;

        // when
        List<StudyApplication> result = applicationRepository.findByStudyId(studyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(app -> app.getStudyId().equals(studyId));
    }

    @Test
    @DisplayName("스터디별 상태별 신청 개수")
    void countByStudyIdAndStatus_Success() {
        // given
        Long studyId = 1L;

        // when
        Long pendingCount = applicationRepository.countByStudyIdAndStatus(studyId, ApplicationStatus.PENDING);
        Long approvedCount = applicationRepository.countByStudyIdAndStatus(studyId, ApplicationStatus.APPROVED);

        // then
        assertThat(pendingCount).isEqualTo(2);
        assertThat(approvedCount).isEqualTo(1);
    }

    // ============================================================
    // 사용자별 신청 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자별 전체 신청 조회 - 페이징")
    void findByUserId_Paging_Success() {
        // given
        Long userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyApplication> result = applicationRepository.findByUserId(userId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(app -> app.getUserId().equals(userId));
    }

    @Test
    @DisplayName("사용자별 상태별 신청 조회")
    void findByUserIdAndStatus_Success() {
        // given
        Long userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyApplication> pending = applicationRepository.findByUserIdAndStatus(
                userId, ApplicationStatus.PENDING, pageable);
        Page<StudyApplication> rejected = applicationRepository.findByUserIdAndStatus(
                userId, ApplicationStatus.REJECTED, pageable);

        // then
        assertThat(pending.getContent()).hasSize(1);
        assertThat(rejected.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("사용자별 신청 목록 조회 - 리스트")
    void findByUserId_List_Success() {
        // given
        Long userId = 10L;

        // when
        List<StudyApplication> result = applicationRepository.findByUserId(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(app -> app.getUserId().equals(userId));
    }

    // ============================================================
    // 개별 신청 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디 + 사용자로 신청 조회 - 존재함")
    void findByStudyIdAndUserId_Exists_Success() {
        // given
        Long studyId = 1L;
        Long userId = 10L;

        // when
        Optional<StudyApplication> result = applicationRepository.findByStudyIdAndUserId(studyId, userId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStudyId()).isEqualTo(studyId);
        assertThat(result.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("스터디 + 사용자로 신청 조회 - 존재하지 않음")
    void findByStudyIdAndUserId_NotExists() {
        // given
        Long studyId = 1L;
        Long userId = 999L;

        // when
        Optional<StudyApplication> result = applicationRepository.findByStudyIdAndUserId(studyId, userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("신청 존재 여부 확인 - 존재함")
    void existsByStudyIdAndUserId_Exists() {
        // given
        Long studyId = 1L;
        Long userId = 10L;

        // when
        boolean exists = applicationRepository.existsByStudyIdAndUserId(studyId, userId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("신청 존재 여부 확인 - 존재하지 않음")
    void existsByStudyIdAndUserId_NotExists() {
        // given
        Long studyId = 1L;
        Long userId = 999L;

        // when
        boolean exists = applicationRepository.existsByStudyIdAndUserId(studyId, userId);

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // 통계 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 전체 신청 개수")
    void countByStudyId_Success() {
        // given
        Long studyId = 1L;

        // when
        Long count = applicationRepository.countByStudyId(studyId);

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("스터디별 승인된 신청 개수")
    void countApprovedByStudyId_Success() {
        // given
        Long studyId = 1L;

        // when
        Long count = applicationRepository.countApprovedByStudyId(studyId);

        // then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자별 전체 신청 개수")
    void countByUserId_Success() {
        // given
        Long userId = 10L;

        // when
        Long count = applicationRepository.countByUserId(userId);

        // then
        assertThat(count).isEqualTo(2);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("신청 생성")
    void save_Success() {
        // given
        StudyApplication newApp = StudyApplication.builder()
                .studyId(3L)
                .userId(20L)
                .message("새로운 신청입니다.")
                .matchingScore(new BigDecimal("88.0"))
                .status(ApplicationStatus.PENDING)
                .build();

        // when
        StudyApplication saved = applicationRepository.save(newApp);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyId()).isEqualTo(3L);
        assertThat(saved.getUserId()).isEqualTo(20L);
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("신청 수정 - 승인 처리")
    void update_Approve_Success() {
        // given
        StudyApplication app = pendingApp1;

        // when
        app.approve();
        StudyApplication updated = applicationRepository.save(app);

        // then
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(updated.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("신청 수정 - 거절 처리")
    void update_Reject_Success() {
        // given
        StudyApplication app = pendingApp1;
        String reason = "자격 요건 미달";

        // when
        app.reject(reason);
        StudyApplication updated = applicationRepository.save(app);

        // then
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(updated.getRejectedReason()).isEqualTo(reason);
        assertThat(updated.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("신청 삭제")
    void delete_Success() {
        // given
        Long applicationId = pendingApp1.getId();

        // when
        applicationRepository.delete(pendingApp1);

        // then
        Optional<StudyApplication> result = applicationRepository.findById(applicationId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스터디별 신청 전체 삭제")
    void deleteByStudyId_Success() {
        // given
        Long studyId = 1L;

        // when
        applicationRepository.deleteByStudyId(studyId);
        applicationRepository.flush();

        // then
        List<StudyApplication> result = applicationRepository.findByStudyId(studyId);
        assertThat(result).isEmpty();
    }
}
