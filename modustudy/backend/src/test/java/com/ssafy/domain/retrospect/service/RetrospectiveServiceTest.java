package com.ssafy.domain.retrospect.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveListResponse;
import com.ssafy.domain.retrospect.entity.Category;
import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveItem;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import com.ssafy.domain.retrospect.repository.RetrospectiveItemRepository;
import com.ssafy.domain.retrospect.repository.RetrospectiveRepository;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RetrospectiveServiceTest {

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private RetrospectiveItemRepository retrospectiveItemRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private UserRepository userRepository;

    private Study study;
    private Long studyId;
    private User user;
    private Long userId;
    private StudySession session1;
    private StudySession session2;
    private Retrospective retro1;
    private Retrospective retro2;
    private Retrospective retro3;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        user = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스트유저")
                .name("테스트")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();
        userId = user.getId();

        // 2. Topic 생성
        Topic topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 3. Format 생성
        Format format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 4. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(userId)
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.IN_PROGRESS)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .extensionCount(0)
                .build());
        studyRepository.flush();
        studyId = study.getId();

        // 5. StudySession 생성
        session1 = studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(1)
                .title("1회차 세션")
                .scheduledAt(LocalDateTime.of(2025, 1, 10, 19, 0))
                .status(SessionStatus.COMPLETED)
                .build());

        session2 = studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(2)
                .title("2회차 세션")
                .scheduledAt(LocalDateTime.of(2025, 1, 17, 19, 0))
                .status(SessionStatus.COMPLETED)
                .build());
        studySessionRepository.flush();

        // 6. Retrospective 생성 (시간차를 두고 생성)
        retro1 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(session1.getId())
                .title("1회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());

        retro2 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(session2.getId())
                .title("2회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());

        retro3 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(null)  // 세션 없는 자유 회고
                .title("중간 점검 회고")
                .retrospectiveType(RetrospectiveType.FREE)
                .build());
        retrospectiveRepository.flush();

        // 7. RetrospectiveItem 생성 - retro1에만 항목 추가
        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(userId)
                .category(Category.KEEP)
                .content("좋았던 점")
                .build());

        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(userId)
                .category(Category.PROBLEM)
                .content("아쉬웠던 점")
                .build());

        // 다른 유저의 항목 추가
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser")
                .email("other@test.com")
                .nickname("다른유저")
                .name("다른")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(otherUser.getId())
                .category(Category.TRY)
                .content("시도해볼 점")
                .build());
        retrospectiveItemRepository.flush();
    }

    @Test
    @DisplayName("회고 목록 조회 성공")
    void getRetrospectives_Success() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("회고 목록 조회 - 최신순 정렬 확인")
    void getRetrospectives_OrderByCreatedAtDesc() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

        // then
        // 마지막에 생성된 retro3가 첫 번째로 와야 함
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("중간 점검 회고");
    }

    @Test
    @DisplayName("회고 목록 조회 - 세션 정보 포함")
    void getRetrospectives_WithSessionInfo() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

        // then
        // retro1 찾기 (1회차 회고)
        RetrospectiveListResponse retro1Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("1회차 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro1Response.getSession()).isNotNull();
        assertThat(retro1Response.getSession().getSessionNumber()).isEqualTo(1);

        // retro3은 세션 없음
        RetrospectiveListResponse retro3Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("중간 점검 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro3Response.getSession()).isNull();
    }

    @Test
    @DisplayName("회고 목록 조회 - itemCount 확인")
    void getRetrospectives_WithItemCount() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

        // then
        RetrospectiveListResponse retro1Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("1회차 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro1Response.getItemCount()).isEqualTo(3);  // 3개 항목

        RetrospectiveListResponse retro2Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("2회차 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro2Response.getItemCount()).isEqualTo(0);  // 항목 없음
    }

    @Test
    @DisplayName("회고 목록 조회 - participantCount 확인")
    void getRetrospectives_WithParticipantCount() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

        // then
        RetrospectiveListResponse retro1Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("1회차 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro1Response.getParticipantCount()).isEqualTo(2);  // 2명 참여
    }

    @Test
    @DisplayName("회고 목록 조회 - hasMyItem 확인")
    void getRetrospectives_WithHasMyItem() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

        // then
        RetrospectiveListResponse retro1Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("1회차 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro1Response.getHasMyItem()).isTrue();  // 내 항목 있음

        RetrospectiveListResponse retro2Response = result.getContent().stream()
                .filter(r -> r.getTitle().equals("2회차 회고"))
                .findFirst()
                .orElseThrow();

        assertThat(retro2Response.getHasMyItem()).isFalse();  // 내 항목 없음
    }

    @Test
    @DisplayName("회고 목록 조회 - 페이징 확인")
    void getRetrospectives_WithPaging() {
        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(studyId, userId, PageRequest.of(0, 2));

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("회고 목록 조회 - 존재하지 않는 스터디")
    void getRetrospectives_StudyNotFound() {
        // when & then
        assertThatThrownBy(() -> retrospectiveService
                .getRetrospectives(999L, userId, PageRequest.of(0, 20)))
                .isInstanceOf(StudyException.StudyNotFoundException.class);
    }

    @Test
    @DisplayName("회고 목록 조회 - 회고가 없는 스터디")
    void getRetrospectives_EmptyList() {
        // given - 새 스터디 생성
        Topic topic2 = topicRepository.save(Topic.builder()
                .name("백엔드")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        Study emptyStudy = studyRepository.save(Study.builder()
                .leaderId(userId)
                .name("빈 스터디")
                .topic(topic2)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.IN_PROGRESS)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        // when
        Page<RetrospectiveListResponse> result = retrospectiveService
                .getRetrospectives(emptyStudy.getId(), userId, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}