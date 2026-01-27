package com.ssafy.domain.retrospect.service;

import com.ssafy.common.exception.RetrospectiveException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.retrospect.dto.request.RetrospectiveCreateRequest;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveDetailResponse;
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
import org.junit.jupiter.api.Nested;
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
    private User otherUser;
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

        // 다른 유저
        otherUser = userRepository.save(User.builder()
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
                .createdBy(userId)
                .title("1회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());

        retro2 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(session2.getId())
                .createdBy(userId)
                .title("2회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());

        retro3 = retrospectiveRepository.save(Retrospective.builder()
                .studyId(studyId)
                .sessionId(null)  // 세션 없는 자유 회고
                .createdBy(otherUser.getId())  // 다른 유저가 생성
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

        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retro1.getId())
                .userId(otherUser.getId())
                .category(Category.TRY)
                .content("시도해볼 점")
                .build());
        retrospectiveItemRepository.flush();
    }

    // ============================================================
    // 회고 목록 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 목록 조회")
    class GetRetrospectives {

        @Test
        @DisplayName("성공")
        void success() {
            // when
            Page<RetrospectiveListResponse> result = retrospectiveService
                    .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

            // then
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("최신순 정렬 확인")
        void orderByCreatedAtDesc() {
            // when
            Page<RetrospectiveListResponse> result = retrospectiveService
                    .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

            // then
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("중간 점검 회고");
        }

        @Test
        @DisplayName("세션 정보 포함")
        void withSessionInfo() {
            // when
            Page<RetrospectiveListResponse> result = retrospectiveService
                    .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

            // then
            RetrospectiveListResponse retro1Response = result.getContent().stream()
                    .filter(r -> r.getTitle().equals("1회차 회고"))
                    .findFirst()
                    .orElseThrow();

            assertThat(retro1Response.getSession()).isNotNull();
            assertThat(retro1Response.getSession().getSessionNumber()).isEqualTo(1);

            RetrospectiveListResponse retro3Response = result.getContent().stream()
                    .filter(r -> r.getTitle().equals("중간 점검 회고"))
                    .findFirst()
                    .orElseThrow();

            assertThat(retro3Response.getSession()).isNull();
        }

        @Test
        @DisplayName("itemCount, participantCount, hasMyItem 확인")
        void withCounts() {
            // when
            Page<RetrospectiveListResponse> result = retrospectiveService
                    .getRetrospectives(studyId, userId, PageRequest.of(0, 20));

            // then
            RetrospectiveListResponse retro1Response = result.getContent().stream()
                    .filter(r -> r.getTitle().equals("1회차 회고"))
                    .findFirst()
                    .orElseThrow();

            assertThat(retro1Response.getItemCount()).isEqualTo(3);
            assertThat(retro1Response.getParticipantCount()).isEqualTo(2);
            assertThat(retro1Response.getHasMyItem()).isTrue();
        }

        @Test
        @DisplayName("페이징 확인")
        void withPaging() {
            // when
            Page<RetrospectiveListResponse> result = retrospectiveService
                    .getRetrospectives(studyId, userId, PageRequest.of(0, 2));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 스터디")
        void studyNotFound() {
            assertThatThrownBy(() -> retrospectiveService
                    .getRetrospectives(999L, userId, PageRequest.of(0, 20)))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }

        @Test
        @DisplayName("회고가 없는 스터디")
        void emptyList() {
            // given
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

    // ============================================================
    // 회고 상세 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 상세 조회")
    class GetRetrospectiveDetail {

        @Test
        @DisplayName("성공")
        void success() {
            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .getRetrospectiveDetail(studyId, retro1.getId());

            // then
            assertThat(result.getId()).isEqualTo(retro1.getId());
            assertThat(result.getTitle()).isEqualTo("1회차 회고");
            assertThat(result.getRetrospectiveType()).isEqualTo(RetrospectiveType.KPT);
        }

        @Test
        @DisplayName("세션 정보 포함")
        void withSessionInfo() {
            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .getRetrospectiveDetail(studyId, retro1.getId());

            // then
            assertThat(result.getSession()).isNotNull();
            assertThat(result.getSession().getId()).isEqualTo(session1.getId());
            assertThat(result.getSession().getSessionNumber()).isEqualTo(1);
            assertThat(result.getSession().getTitle()).isEqualTo("1회차 세션");
        }

        @Test
        @DisplayName("세션 없는 회고")
        void withoutSession() {
            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .getRetrospectiveDetail(studyId, retro3.getId());

            // then
            assertThat(result.getSession()).isNull();
        }

        @Test
        @DisplayName("카테고리별 항목 그룹핑")
        void itemsGroupedByCategory() {
            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .getRetrospectiveDetail(studyId, retro1.getId());

            // then
            assertThat(result.getItems()).isNotNull();
            assertThat(result.getItems().getKEEP()).hasSize(1);
            assertThat(result.getItems().getPROBLEM()).hasSize(1);
            assertThat(result.getItems().getTRY()).hasSize(1);

            assertThat(result.getItems().getKEEP().get(0).getContent()).isEqualTo("좋았던 점");
            assertThat(result.getItems().getPROBLEM().get(0).getContent()).isEqualTo("아쉬웠던 점");
            assertThat(result.getItems().getTRY().get(0).getContent()).isEqualTo("시도해볼 점");
        }

        @Test
        @DisplayName("항목에 사용자 정보 포함")
        void itemsWithUserInfo() {
            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .getRetrospectiveDetail(studyId, retro1.getId());

            // then
            RetrospectiveDetailResponse.ItemResponse keepItem = result.getItems().getKEEP().get(0);
            assertThat(keepItem.getUser()).isNotNull();
            assertThat(keepItem.getUser().getId()).isEqualTo(userId);
            assertThat(keepItem.getUser().getNickname()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("항목이 없는 회고")
        void emptyItems() {
            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .getRetrospectiveDetail(studyId, retro2.getId());

            // then
            assertThat(result.getItems().getKEEP()).isEmpty();
            assertThat(result.getItems().getPROBLEM()).isEmpty();
            assertThat(result.getItems().getTRY()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 스터디")
        void studyNotFound() {
            assertThatThrownBy(() -> retrospectiveService
                    .getRetrospectiveDetail(999L, retro1.getId()))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 회고")
        void retrospectiveNotFound() {
            assertThatThrownBy(() -> retrospectiveService
                    .getRetrospectiveDetail(studyId, 999L))
                    .isInstanceOf(RetrospectiveException.RetrospectiveNotFoundException.class);
        }

        @Test
        @DisplayName("다른 스터디의 회고 조회 시 실패")
        void wrongStudyId() {
            // given
            Topic topic2 = topicRepository.save(Topic.builder()
                    .name("CS")
                    .sortOrder(3)
                    .build());
            topicRepository.flush();

            Study otherStudy = studyRepository.save(Study.builder()
                    .leaderId(userId)
                    .name("다른 스터디")
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

            // when & then
            assertThatThrownBy(() -> retrospectiveService
                    .getRetrospectiveDetail(otherStudy.getId(), retro1.getId()))
                    .isInstanceOf(RetrospectiveException.RetrospectiveNotFoundException.class);
        }
    }

    // ============================================================
    // 회고 생성 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 생성")
    class CreateRetrospective {

        @Test
        @DisplayName("성공 - 기본 생성")
        void success() {
            // given
            RetrospectiveCreateRequest request = RetrospectiveCreateRequest.builder()
                    .title("새 회고")
                    .retrospectiveType(RetrospectiveType.KPT)
                    .build();

            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .createRetrospective(studyId, request, userId);

            // then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo("새 회고");
            assertThat(result.getRetrospectiveType()).isEqualTo(RetrospectiveType.KPT);
            assertThat(result.getSession()).isNull();
        }

        @Test
        @DisplayName("성공 - 세션과 함께 생성")
        void successWithSession() {
            // given
            RetrospectiveCreateRequest request = RetrospectiveCreateRequest.builder()
                    .title("세션 회고")
                    .retrospectiveType(RetrospectiveType.KPT)
                    .sessionId(session1.getId())
                    .build();

            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .createRetrospective(studyId, request, userId);

            // then
            assertThat(result.getSession()).isNotNull();
            assertThat(result.getSession().getId()).isEqualTo(session1.getId());
        }

        @Test
        @DisplayName("성공 - 기본 타입 KPT")
        void defaultTypeIsKpt() {
            // given
            RetrospectiveCreateRequest request = RetrospectiveCreateRequest.builder()
                    .title("타입 미지정 회고")
                    .retrospectiveType(null)  // 타입 미지정
                    .build();

            // when
            RetrospectiveDetailResponse result = retrospectiveService
                    .createRetrospective(studyId, request, userId);

            // then
            assertThat(result.getRetrospectiveType()).isEqualTo(RetrospectiveType.KPT);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void studyNotFound() {
            // given
            RetrospectiveCreateRequest request = RetrospectiveCreateRequest.builder()
                    .title("새 회고")
                    .build();

            // when & then
            assertThatThrownBy(() -> retrospectiveService
                    .createRetrospective(999L, request, userId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 세션")
        void sessionNotFound() {
            // given
            RetrospectiveCreateRequest request = RetrospectiveCreateRequest.builder()
                    .title("새 회고")
                    .sessionId(999L)
                    .build();

            // when & then
            assertThatThrownBy(() -> retrospectiveService
                    .createRetrospective(studyId, request, userId))
                    .isInstanceOf(RetrospectiveException.InvalidRetrospectiveRequestException.class)
                    .hasMessageContaining("존재하지 않는 세션");
        }
    }

    // ============================================================
    // 회고 삭제 테스트
    // ============================================================

    @Nested
    @DisplayName("회고 삭제")
    class DeleteRetrospective {

        @Test
        @DisplayName("성공 - 생성자가 삭제")
        void successByCreator() {
            // when
            retrospectiveService.deleteRetrospective(studyId, retro1.getId(), userId);

            // then
            assertThat(retrospectiveRepository.findById(retro1.getId())).isEmpty();
        }

        @Test
        @DisplayName("성공 - 스터디장이 다른 사람의 회고 삭제")
        void successByLeader() {
            // retro3는 otherUser가 생성, study의 리더는 user
            // when
            retrospectiveService.deleteRetrospective(studyId, retro3.getId(), userId);

            // then
            assertThat(retrospectiveRepository.findById(retro3.getId())).isEmpty();
        }

        @Test
        @DisplayName("성공 - 회고 삭제 시 항목도 함께 삭제")
        void deleteWithItems() {
            // given
            Long retroId = retro1.getId();
            int itemCount = retrospectiveItemRepository.findByRetrospectiveId(retroId).size();
            assertThat(itemCount).isGreaterThan(0);

            // when
            retrospectiveService.deleteRetrospective(studyId, retroId, userId);

            // then
            assertThat(retrospectiveRepository.findById(retroId)).isEmpty();
            assertThat(retrospectiveItemRepository.findByRetrospectiveId(retroId)).isEmpty();
        }

        @Test
        @DisplayName("실패 - 권한 없음 (생성자도 아니고 스터디장도 아님)")
        void noPermission() {
            // retro1은 user가 생성, user가 스터디장
            // otherUser는 생성자도 아니고 스터디장도 아님
            assertThatThrownBy(() -> retrospectiveService
                    .deleteRetrospective(studyId, retro1.getId(), otherUser.getId()))
                    .isInstanceOf(RetrospectiveException.NotRetrospectiveOwnerException.class);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void studyNotFound() {
            assertThatThrownBy(() -> retrospectiveService
                    .deleteRetrospective(999L, retro1.getId(), userId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회고")
        void retrospectiveNotFound() {
            assertThatThrownBy(() -> retrospectiveService
                    .deleteRetrospective(studyId, 999L, userId))
                    .isInstanceOf(RetrospectiveException.RetrospectiveNotFoundException.class);
        }
    }
}