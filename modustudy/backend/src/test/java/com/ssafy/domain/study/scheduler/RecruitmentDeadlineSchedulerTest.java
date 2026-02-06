package com.ssafy.domain.study.scheduler;

import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * RecruitmentDeadlineScheduler 단위 테스트
 */
 @ExtendWith(MockitoExtension.class)
 class RecruitmentDeadlineSchedulerTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RecruitmentDeadlineScheduler scheduler;

    private Topic testTopic;
    private Study recruitingStudy;

    @BeforeEach
    void setUp() {
        testTopic = Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(testTopic, "id", 1L);

        recruitingStudy = Study.builder()
                .id(1L)
                .leaderId(100L)
                .name("알고리즘 스터디")
                .topic(testTopic)
                .status(Status.RECRUITING)
                .maxMembers(5)
                .recruitEndDate(LocalDate.now().minusDays(1))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("모집 기간 종료 스케줄러 테스트")
    class CheckRecruitmentDeadlinesTest {

        @Test
        @DisplayName("모집 기간 종료 + 인원 충족 시 RECRUIT_CLOSED로 상태 변경")
        void checkRecruitmentDeadlines_MembersFull_ChangeToRecruitClosed() {
            // given
            given(studyRepository.findByStatusAndRecruitEndDateBefore(eq(Status.RECRUITING), any(LocalDate.class)))
                    .willReturn(List.of(recruitingStudy));
            given(studyMemberRepository.countByStudyIdAndStatus(recruitingStudy.getId(), MemberStatus.APPROVED))
                    .willReturn(5); // maxMembers와 동일

            // when
            scheduler.checkRecruitmentDeadlines();

            // then
            assertThat(recruitingStudy.getStatus()).isEqualTo(Status.RECRUIT_CLOSED);

            verify(notificationService, times(1)).createNotification(
                    eq(100L), // leaderId
                    eq(NotificationType.STUDY_RECRUITMENT_COMPLETE),
                    any(),
                    any(),
                    eq("STUDY"),
                    eq(1L)
            );
        }

        @Test
        @DisplayName("모집 기간 종료 + 최소 인원(2명) 충족 시 RECRUIT_CLOSED로 상태 변경")
        void checkRecruitmentDeadlines_MinimumMembersMet_ChangeToRecruitClosed() {
            // given
            given(studyRepository.findByStatusAndRecruitEndDateBefore(eq(Status.RECRUITING), any(LocalDate.class)))
                    .willReturn(List.of(recruitingStudy));
            given(studyMemberRepository.countByStudyIdAndStatus(recruitingStudy.getId(), MemberStatus.APPROVED))
                    .willReturn(2); // 최소 인원 2명 충족

            // when
            scheduler.checkRecruitmentDeadlines();

            // then
            assertThat(recruitingStudy.getStatus()).isEqualTo(Status.RECRUIT_CLOSED);

            verify(notificationService, times(1)).createNotification(
                    eq(100L),
                    eq(NotificationType.STUDY_RECRUITMENT_COMPLETE),
                    any(),
                    any(),
                    eq("STUDY"),
                    eq(1L)
            );
        }

        @Test
        @DisplayName("모집 기간 종료 + 인원 미충족 시 PENDING으로 상태 변경")
        void checkRecruitmentDeadlines_MembersNotEnough_ChangeToPending() {
            // given
            given(studyRepository.findByStatusAndRecruitEndDateBefore(eq(Status.RECRUITING), any(LocalDate.class)))
                    .willReturn(List.of(recruitingStudy));
            given(studyMemberRepository.countByStudyIdAndStatus(recruitingStudy.getId(), MemberStatus.APPROVED))
                    .willReturn(1); // 1명만 있음 (인원 미충족)

            // when
            scheduler.checkRecruitmentDeadlines();

            // then
            assertThat(recruitingStudy.getStatus()).isEqualTo(Status.PENDING);

            verify(notificationService, times(1)).createNotification(
                    eq(100L),
                    eq(NotificationType.STUDY_UPDATE),
                    any(),
                    any(),
                    eq("STUDY"),
                    eq(1L)
            );
        }

        @Test
        @DisplayName("모집 기간이 종료된 스터디가 없는 경우")
        void checkRecruitmentDeadlines_NoExpiredStudies() {
            // given
            given(studyRepository.findByStatusAndRecruitEndDateBefore(eq(Status.RECRUITING), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());

            // when
            scheduler.checkRecruitmentDeadlines();

            // then
            verify(studyMemberRepository, never()).countByStudyIdAndStatus(any(), any());
            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("여러 스터디 동시 처리 - 각 스터디별 조건에 따라 상태 변경")
        void checkRecruitmentDeadlines_MultipleStudies() {
            // given
            Study studyWithFullMembers = Study.builder()
                    .id(2L)
                    .leaderId(200L)
                    .name("풀 인원 스터디")
                    .topic(testTopic)
                    .status(Status.RECRUITING)
                    .maxMembers(3)
                    .recruitEndDate(LocalDate.now().minusDays(1))
                    .createdAt(LocalDateTime.now())
                    .build();

            Study studyWithFewMembers = Study.builder()
                    .id(3L)
                    .leaderId(300L)
                    .name("인원 부족 스터디")
                    .topic(testTopic)
                    .status(Status.RECRUITING)
                    .maxMembers(5)
                    .recruitEndDate(LocalDate.now().minusDays(1))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(studyRepository.findByStatusAndRecruitEndDateBefore(eq(Status.RECRUITING), any(LocalDate.class)))
                    .willReturn(List.of(studyWithFullMembers, studyWithFewMembers));
            given(studyMemberRepository.countByStudyIdAndStatus(2L, MemberStatus.APPROVED))
                    .willReturn(3); // 풀 인원
            given(studyMemberRepository.countByStudyIdAndStatus(3L, MemberStatus.APPROVED))
                    .willReturn(1); // 인원 부족

            // when
            scheduler.checkRecruitmentDeadlines();

            // then
            assertThat(studyWithFullMembers.getStatus()).isEqualTo(Status.RECRUIT_CLOSED);
            assertThat(studyWithFewMembers.getStatus()).isEqualTo(Status.PENDING);

            verify(notificationService, times(1)).createNotification(
                    eq(200L),
                    eq(NotificationType.STUDY_RECRUITMENT_COMPLETE),
                    any(),
                    any(),
                    eq("STUDY"),
                    eq(2L)
            );

            verify(notificationService, times(1)).createNotification(
                    eq(300L),
                    eq(NotificationType.STUDY_UPDATE),
                    any(),
                    any(),
                    eq("STUDY"),
                    eq(3L)
            );
        }

        @Test
        @DisplayName("maxMembers가 null인 경우 최소 인원(2명) 기준으로 판단")
        void checkRecruitmentDeadlines_MaxMembersNull_UseMinimumCriteria() {
            // given
            Study studyWithNoMaxLimit = Study.builder()
                    .id(4L)
                    .leaderId(400L)
                    .name("인원 제한 없는 스터디")
                    .topic(testTopic)
                    .status(Status.RECRUITING)
                    .maxMembers(null) // 최대 인원 제한 없음
                    .recruitEndDate(LocalDate.now().minusDays(1))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(studyRepository.findByStatusAndRecruitEndDateBefore(eq(Status.RECRUITING), any(LocalDate.class)))
                    .willReturn(List.of(studyWithNoMaxLimit));
            given(studyMemberRepository.countByStudyIdAndStatus(4L, MemberStatus.APPROVED))
                    .willReturn(2); // 최소 인원 충족

            // when
            scheduler.checkRecruitmentDeadlines();

            // then
            assertThat(studyWithNoMaxLimit.getStatus()).isEqualTo(Status.RECRUIT_CLOSED);
        }
    }
}
