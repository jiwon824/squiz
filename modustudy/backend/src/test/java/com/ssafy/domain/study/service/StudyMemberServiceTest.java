package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.response.StudyMemberResponse;
import com.ssafy.domain.study.entity.MemberRole;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StudyMemberServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyMemberService studyMemberService;

    private User user1;
    private User user2;
    private StudyMember leaderMember;
    private StudyMember normalMember;

    @BeforeEach
    void setUp() {
        // User 1 (스터디장)
        user1 = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user1, "id", 1L);

        // User 2 (일반 멤버)
        user2 = User.builder()
                .name("이싸피")
                .nickname("ssafy_lee")
                .email("lee@ssafy.com")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user2, "id", 2L);

        // 스터디장 멤버십
        leaderMember = StudyMember.builder()
                .id(1L)
                .studyId(1L)
                .userId(1L)
                .role(MemberRole.LEADER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();

        // 일반 멤버십
        normalMember = StudyMember.builder()
                .id(2L)
                .studyId(1L)
                .userId(2L)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.APPROVED)
                .isProbation(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // 스터디 멤버 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 멤버 목록 조회 테스트")
    class GetStudyMembersTest {

        @Test
        @DisplayName("멤버 목록 조회 성공 - 스터디장 + 일반 멤버")
        void getStudyMembers_Success() {
            // given
            Long studyId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED))
                    .willReturn(List.of(leaderMember, normalMember));
            given(userRepository.findAllById(List.of(1L, 2L)))
                    .willReturn(List.of(user1, user2));

            // when
            Page<StudyMemberResponse> result = studyMemberService.getStudyMembers(studyId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            // 스터디장 확인
            StudyMemberResponse leader = result.getContent().stream()
                    .filter(m -> m.getRole() == MemberRole.LEADER)
                    .findFirst()
                    .orElse(null);
            assertThat(leader).isNotNull();
            assertThat(leader.getUserName()).isEqualTo("김싸피");
            assertThat(leader.getIsProbation()).isFalse();

            // 일반 멤버 확인
            StudyMemberResponse member = result.getContent().stream()
                    .filter(m -> m.getRole() == MemberRole.MEMBER)
                    .findFirst()
                    .orElse(null);
            assertThat(member).isNotNull();
            assertThat(member.getUserName()).isEqualTo("이싸피");
            assertThat(member.getIsProbation()).isTrue();

            verify(studyRepository, times(1)).existsById(studyId);
            verify(studyMemberRepository, times(1)).findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
            verify(userRepository, times(1)).findAllById(List.of(1L, 2L));
        }

        @Test
        @DisplayName("멤버 목록 조회 성공 - 스터디장만 있는 경우")
        void getStudyMembers_OnlyLeader() {
            // given
            Long studyId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED))
                    .willReturn(List.of(leaderMember));
            given(userRepository.findAllById(List.of(1L)))
                    .willReturn(List.of(user1));

            // when
            Page<StudyMemberResponse> result = studyMemberService.getStudyMembers(studyId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getRole()).isEqualTo(MemberRole.LEADER);
            assertThat(result.getContent().get(0).getUserName()).isEqualTo("김싸피");
        }

        @Test
        @DisplayName("멤버 목록 조회 실패 - 존재하지 않는 스터디")
        void getStudyMembers_StudyNotFound() {
            // given
            Long studyId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            given(studyRepository.existsById(studyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> studyMemberService.getStudyMembers(studyId, pageable))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);

            verify(studyMemberRepository, never()).findByStudyIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("멤버 목록 조회 - 빈 목록")
        void getStudyMembers_EmptyList() {
            // given
            Long studyId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED))
                    .willReturn(List.of());

            // when
            Page<StudyMemberResponse> result = studyMemberService.getStudyMembers(studyId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ============================================================
    // 스터디 멤버 수 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 멤버 수 조회 테스트")
    class CountStudyMembersTest {

        @Test
        @DisplayName("멤버 수 조회 성공")
        void countStudyMembers_Success() {
            // given
            Long studyId = 1L;

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED))
                    .willReturn(2);

            // when
            int count = studyMemberService.countStudyMembers(studyId);

            // then
            assertThat(count).isEqualTo(2);

            verify(studyRepository, times(1)).existsById(studyId);
            verify(studyMemberRepository, times(1)).countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
        }

        @Test
        @DisplayName("멤버 수 조회 - 0명")
        void countStudyMembers_Zero() {
            // given
            Long studyId = 1L;

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED))
                    .willReturn(0);

            // when
            int count = studyMemberService.countStudyMembers(studyId);

            // then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("멤버 수 조회 실패 - 존재하지 않는 스터디")
        void countStudyMembers_StudyNotFound() {
            // given
            Long studyId = 999L;

            given(studyRepository.existsById(studyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> studyMemberService.countStudyMembers(studyId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);

            verify(studyMemberRepository, never()).countByStudyIdAndStatus(any(), any());
        }
    }

    // ============================================================
    // 멤버 여부 확인 테스트
    // ============================================================

    @Nested
    @DisplayName("멤버 여부 확인 테스트")
    class IsMemberTest {

        @Test
        @DisplayName("멤버 여부 확인 - true (멤버인 경우)")
        void isMember_True() {
            // given
            Long studyId = 1L;
            Long userId = 2L;

            given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, MemberStatus.APPROVED))
                    .willReturn(true);

            // when
            boolean result = studyMemberService.isMember(studyId, userId);

            // then
            assertThat(result).isTrue();

            verify(studyMemberRepository, times(1))
                    .existsByStudyIdAndUserIdAndStatus(studyId, userId, MemberStatus.APPROVED);
        }

        @Test
        @DisplayName("멤버 여부 확인 - false (멤버 아닌 경우)")
        void isMember_False() {
            // given
            Long studyId = 1L;
            Long userId = 999L;

            given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, userId, MemberStatus.APPROVED))
                    .willReturn(false);

            // when
            boolean result = studyMemberService.isMember(studyId, userId);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("멤버 여부 확인 - 스터디장도 멤버로 인정")
        void isMember_LeaderIsAlsoMember() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L;

            given(studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, leaderId, MemberStatus.APPROVED))
                    .willReturn(true);

            // when
            boolean result = studyMemberService.isMember(studyId, leaderId);

            // then
            assertThat(result).isTrue();
        }
    }

    // ============================================================
    // 스터디 탈퇴 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디 탈퇴 테스트")
    class LeaveStudyTest {

        private Study study;

        @BeforeEach
        void setUpStudy() {
            // 스터디 (스터디장 ID = 1L)
            study = Study.builder()
                    .leaderId(1L)
                    .name("테스트 스터디")
                    .build();
            ReflectionTestUtils.setField(study, "id", 1L);
        }

        @Test
        @DisplayName("스터디 탈퇴 성공 - 일반 멤버 탈퇴")
        void leaveStudy_Success() {
            // given
            Long studyId = 1L;
            Long userId = 2L; // 일반 멤버

            given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
            given(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                    .willReturn(Optional.of(normalMember));

            // when
            studyMemberService.leaveStudy(studyId, userId);

            // then
            assertThat(normalMember.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(normalMember.getLeftAt()).isNotNull();

            verify(studyRepository, times(1)).findById(studyId);
            verify(studyMemberRepository, times(1)).findByStudyIdAndUserId(studyId, userId);
            verify(studyMemberRepository, times(1)).save(normalMember);
        }

        @Test
        @DisplayName("스터디 탈퇴 실패 - 스터디장은 탈퇴 불가")
        void leaveStudy_LeaderCannotLeave() {
            // given
            Long studyId = 1L;
            Long leaderId = 1L; // 스터디장

            given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

            // when & then
            assertThatThrownBy(() -> studyMemberService.leaveStudy(studyId, leaderId))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("스터디장은 탈퇴할 수 없습니다");

            verify(studyMemberRepository, never()).findByStudyIdAndUserId(any(), any());
            verify(studyMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("스터디 탈퇴 실패 - 존재하지 않는 스터디")
        void leaveStudy_StudyNotFound() {
            // given
            Long studyId = 999L;
            Long userId = 2L;

            given(studyRepository.findById(studyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyMemberService.leaveStudy(studyId, userId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);

            verify(studyMemberRepository, never()).findByStudyIdAndUserId(any(), any());
            verify(studyMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("스터디 탈퇴 실패 - 멤버가 아닌 사용자")
        void leaveStudy_NotMember() {
            // given
            Long studyId = 1L;
            Long userId = 999L; // 멤버 아님

            given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
            given(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyMemberService.leaveStudy(studyId, userId))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("스터디 멤버가 아닙니다");

            verify(studyMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("스터디 탈퇴 실패 - 승인되지 않은 멤버 (PENDING 상태)")
        void leaveStudy_NotApprovedMember() {
            // given
            Long studyId = 1L;
            Long userId = 3L;

            StudyMember pendingMember = StudyMember.builder()
                    .id(3L)
                    .studyId(studyId)
                    .userId(userId)
                    .role(MemberRole.MEMBER)
                    .status(MemberStatus.PENDING)
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
            given(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                    .willReturn(Optional.of(pendingMember));

            // when & then
            assertThatThrownBy(() -> studyMemberService.leaveStudy(studyId, userId))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("승인된 멤버만 탈퇴할 수 있습니다");

            verify(studyMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("스터디 탈퇴 실패 - 이미 탈퇴한 멤버 (LEFT 상태)")
        void leaveStudy_AlreadyLeft() {
            // given
            Long studyId = 1L;
            Long userId = 4L;

            StudyMember leftMember = StudyMember.builder()
                    .id(4L)
                    .studyId(studyId)
                    .userId(userId)
                    .role(MemberRole.MEMBER)
                    .status(MemberStatus.LEFT)
                    .leftAt(LocalDateTime.now().minusDays(1))
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
            given(studyMemberRepository.findByStudyIdAndUserId(studyId, userId))
                    .willReturn(Optional.of(leftMember));

            // when & then
            assertThatThrownBy(() -> studyMemberService.leaveStudy(studyId, userId))
                    .isInstanceOf(StudyException.InvalidStudyRequestException.class)
                    .hasMessageContaining("승인된 멤버만 탈퇴할 수 있습니다");

            verify(studyMemberRepository, never()).save(any());
        }
    }
}
