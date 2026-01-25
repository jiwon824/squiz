package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * StudyService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyService studyService;

    private User testUser;
    private Study testStudy;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .role(Role.USER)
                .build();

        testStudy = Study.builder()
                .id(1L)
                .leaderId(1L)
                .name("알고리즘 마스터")
                .description("백준 골드 문제 집중 풀이")
                .topic("알고리즘")
                .format("문제풀이")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .isPublic(true)
                .maxMembers(6)
                .difficulty(Difficulty.INTERMEDIATE)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .createdAt(LocalDateTime.now())  // ⭐ NPE 방지
                .build();
    }

    // ============================================================
    // 스터디 생성 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 생성 테스트")
    class CreateStudyTest {

        @Test
        @DisplayName("스터디 생성 성공 - 스터디장이 StudyMember에 자동 추가됨")
        void createStudy_Success_LeaderAddedToStudyMember() {
            // given
            Long leaderId = 1L;

            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("알고리즘 마스터")
                    .description("백준 골드 문제 집중 풀이")
                    .topic("알고리즘")
                    .format("문제풀이")
                    .studyType(StudyType.PLANNED)
                    .meetingType(MeetingType.ONLINE)
                    .isPublic(true)
                    .maxMembers(6)
                    .difficulty(Difficulty.INTERMEDIATE)
                    .startDate(LocalDate.of(2025, 2, 1))
                    .endDate(LocalDate.of(2025, 5, 1))
                    .recruitStartDate(LocalDate.of(2025, 1, 15))
                    .recruitEndDate(LocalDate.of(2025, 1, 31))
                    .build();

            given(userRepository.findById(leaderId)).willReturn(Optional.of(testUser));
            given(studyRepository.save(any(Study.class))).willReturn(testStudy);
            given(studyMemberRepository.save(any(StudyMember.class))).willReturn(StudyMember.builder().build());

            // when
            StudyResponse response = studyService.createStudy(request, leaderId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("알고리즘 마스터");
            assertThat(response.getLeaderName()).isEqualTo("김싸피");

            // ⭐ 핵심: StudyMember 저장 검증
            ArgumentCaptor<StudyMember> memberCaptor = ArgumentCaptor.forClass(StudyMember.class);
            verify(studyMemberRepository, times(1)).save(memberCaptor.capture());

            StudyMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getStudyId()).isEqualTo(1L);
            assertThat(savedMember.getUserId()).isEqualTo(leaderId);
            assertThat(savedMember.getRole()).isEqualTo(MemberRole.LEADER);
            assertThat(savedMember.getStatus()).isEqualTo(MemberStatus.APPROVED);
            assertThat(savedMember.getIsProbation()).isFalse();  // 스터디장은 수습 아님!
            assertThat(savedMember.getJoinedAt()).isNotNull();

            verify(userRepository, times(1)).findById(leaderId);
            verify(studyRepository, times(1)).save(any(Study.class));
        }

        @Test
        @DisplayName("스터디 생성 실패 - 존재하지 않는 사용자")
        void createStudy_UserNotFound() {
            // given
            Long leaderId = 999L;

            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("알고리즘 마스터")
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.of(2025, 2, 1))
                    .endDate(LocalDate.of(2025, 5, 1))
                    .build();

            given(userRepository.findById(leaderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyService.createStudy(request, leaderId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 사용자");

            verify(studyRepository, never()).save(any());
            verify(studyMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("스터디 생성 실패 - 오프라인 스터디인데 지역 정보 없음")
        void createStudy_OfflineWithoutRegion() {
            // given
            Long leaderId = 1L;

            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("오프라인 스터디")
                    .meetingType(MeetingType.OFFLINE)  // 오프라인인데
                    .regionId(null)  // 지역 정보 없음!
                    .startDate(LocalDate.of(2025, 2, 1))
                    .endDate(LocalDate.of(2025, 5, 1))
                    .build();

            given(userRepository.findById(leaderId)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> studyService.createStudy(request, leaderId))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("오프라인/혼합 스터디는 지역 정보가 필수");

            verify(studyRepository, never()).save(any());
            verify(studyMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("스터디 생성 실패 - 종료일이 시작일보다 앞섬")
        void createStudy_InvalidDateRange() {
            // given
            Long leaderId = 1L;

            StudyCreateRequest request = StudyCreateRequest.builder()
                    .name("알고리즘 마스터")
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.of(2025, 5, 1))  // 시작일이 더 늦음
                    .endDate(LocalDate.of(2025, 2, 1))    // 종료일이 더 빠름
                    .build();

            given(userRepository.findById(leaderId)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> studyService.createStudy(request, leaderId))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("종료일은 시작일보다 늦어야");

            verify(studyRepository, never()).save(any());
            verify(studyMemberRepository, never()).save(any());
        }
    }

    // ============================================================
    // 스터디 수정 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 수정 테스트")
    class UpdateStudyTest {

        @Test
        @DisplayName("스터디 수정 성공")
        void updateStudy_Success() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L;

            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .name("알고리즘 마스터 시즌2")
                    .description("백준 플래티넘 도전!")
                    .meetingType(MeetingType.ONLINE)
                    .startDate(LocalDate.of(2025, 2, 1))
                    .endDate(LocalDate.of(2025, 6, 1))
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));

            // when
            StudyResponse response = studyService.updateStudy(studyId, request, leaderId);

            // then
            assertThat(response).isNotNull();
            verify(studyRepository, times(1)).findById(studyId);
        }

        @Test
        @DisplayName("스터디 수정 실패 - 권한 없음 (스터디장 아님)")
        void updateStudy_NotLeader() {
            // given
            Long studyId = 1L;
            Long notLeaderId = 999L;

            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .name("알고리즘 마스터 시즌2")
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));

            // when & then
            assertThatThrownBy(() -> studyService.updateStudy(studyId, request, notLeaderId))
                    .isInstanceOf(StudyException.NotStudyLeaderException.class);
        }

        @Test
        @DisplayName("스터디 수정 실패 - 존재하지 않는 스터디")
        void updateStudy_StudyNotFound() {
            // given
            Long studyId = 999L;
            Long leaderId = 1L;

            StudyUpdateRequest request = StudyUpdateRequest.builder()
                    .name("알고리즘 마스터 시즌2")
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyService.updateStudy(studyId, request, leaderId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }
    }

    // ============================================================
    // 스터디 삭제 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 삭제 테스트")
    class DeleteStudyTest {

        @Test
        @DisplayName("스터디 삭제 성공")
        void deleteStudy_Success() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L;

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));

            // when
            studyService.deleteStudy(studyId, leaderId);

            // then
            verify(studyRepository, times(1)).delete(testStudy);
        }

        @Test
        @DisplayName("스터디 삭제 실패 - 진행 중인 스터디")
        void deleteStudy_InProgress() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L;

            Study inProgressStudy = Study.builder()
                    .id(studyId)
                    .leaderId(leaderId)
                    .status(Status.IN_PROGRESS)  // 진행 중!
                    .createdAt(LocalDateTime.now())  // ⭐ NPE 방지
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(inProgressStudy));

            // when & then
            assertThatThrownBy(() -> studyService.deleteStudy(studyId, leaderId))
                    .isInstanceOf(StudyException.CannotDeleteStudyException.class);

            verify(studyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("스터디 삭제 실패 - 권한 없음")
        void deleteStudy_NotLeader() {
            // given
            Long studyId = 1L;
            Long notLeaderId = 999L;

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));

            // when & then
            assertThatThrownBy(() -> studyService.deleteStudy(studyId, notLeaderId))
                    .isInstanceOf(StudyException.NotStudyLeaderException.class);

            verify(studyRepository, never()).delete(any());
        }
    }

    // ============================================================
    // 내 스터디 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("내 스터디 목록 조회 테스트")
    class GetMyStudiesTest {

        @Test
        @DisplayName("내 스터디 목록 조회 성공 - 스터디장 + 멤버 스터디 모두 조회")
        void getMyStudies_Success() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            // 스터디장으로 만든 스터디 (⭐ createdAt 추가!)
            Study leaderStudy = Study.builder()
                    .id(1L)
                    .leaderId(userId)
                    .name("내가 만든 스터디")
                    .createdAt(LocalDateTime.now())  // ⭐ NPE 방지
                    .build();

            // 멤버로 참여한 스터디 (⭐ createdAt 추가!)
            Study memberStudy = Study.builder()
                    .id(2L)
                    .leaderId(999L)  // 다른 사람이 스터디장
                    .name("참여한 스터디")
                    .createdAt(LocalDateTime.now().minusDays(1))  // ⭐ NPE 방지
                    .build();

            StudyMember membership = StudyMember.builder()
                    .studyId(2L)
                    .userId(userId)
                    .role(MemberRole.MEMBER)
                    .status(MemberStatus.APPROVED)
                    .build();

            Page<Study> leaderStudies = new PageImpl<>(List.of(leaderStudy));

            given(studyMemberRepository.findByUserIdAndStatus(userId, MemberStatus.APPROVED))
                    .willReturn(List.of(membership));
            given(studyRepository.findByLeaderId(userId, pageable)).willReturn(leaderStudies);
            given(studyRepository.findAllById(List.of(2L))).willReturn(List.of(memberStudy));

            // when
            Page<Study> result = studyService.getMyStudies(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);  // 스터디장 1개 + 멤버 1개

            verify(studyMemberRepository, times(1)).findByUserIdAndStatus(userId, MemberStatus.APPROVED);
            verify(studyRepository, times(1)).findByLeaderId(userId, pageable);
            verify(studyRepository, times(1)).findAllById(List.of(2L));
        }

        @Test
        @DisplayName("내 스터디 목록 조회 - 참여한 스터디 없음")
        void getMyStudies_Empty() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            given(studyMemberRepository.findByUserIdAndStatus(userId, MemberStatus.APPROVED))
                    .willReturn(List.of());
            given(studyRepository.findByLeaderId(userId, pageable))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            Page<Study> result = studyService.getMyStudies(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // ============================================================
    // 스터디 상태 변경 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 상태 변경 테스트")
    class UpdateStatusTest {

        @Test
        @DisplayName("상태 변경 성공 - RECRUITING -> IN_PROGRESS")
        void updateStatus_Success() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L;
            Status newStatus = Status.IN_PROGRESS;

            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));

            // when
            StudyResponse response = studyService.updateStudyStatus(studyId, newStatus, leaderId);

            // then
            assertThat(response).isNotNull();
            verify(studyRepository, times(1)).findById(studyId);
        }

        @Test
        @DisplayName("상태 변경 실패 - 완료된 스터디")
        void updateStatus_CompletedStudy() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L;

            Study completedStudy = Study.builder()
                    .id(studyId)
                    .leaderId(leaderId)
                    .status(Status.COMPLETED)  // 이미 완료됨
                    .createdAt(LocalDateTime.now())  // ⭐ NPE 방지
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(completedStudy));

            // when & then
            assertThatThrownBy(() -> studyService.updateStudyStatus(studyId, Status.RECRUITING, leaderId))
                    .isInstanceOf(StudyException.InvalidStatusTransitionException.class);
        }
    }

    // ============================================================
    // 스터디 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 조회 테스트")
    class GetStudyTest {

        @Test
        @DisplayName("스터디 상세 조회 성공")
        void getStudyById_Success() {
            // given
            Long studyId = 1L;
            given(studyRepository.findById(studyId)).willReturn(Optional.of(testStudy));

            // when
            Study result = studyService.getStudyById(studyId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(studyId);
            assertThat(result.getName()).isEqualTo("알고리즘 마스터");
        }

        @Test
        @DisplayName("스터디 상세 조회 실패 - 존재하지 않는 스터디")
        void getStudyById_NotFound() {
            // given
            Long studyId = 999L;
            given(studyRepository.findById(studyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyService.getStudyById(studyId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }

        @Test
        @DisplayName("모집중인 스터디 목록 조회")
        void getRecruitingStudies_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Study> studies = new PageImpl<>(List.of(testStudy));

            given(studyRepository.findRecruitingStudies(pageable)).willReturn(studies);

            // when
            Page<Study> result = studyService.getRecruitingStudies(pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(Status.RECRUITING);
        }
    }
}