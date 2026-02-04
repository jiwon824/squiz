package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.ApplicationCreateRequest;
import com.ssafy.domain.study.dto.response.ApplicationResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyApplicationRepository;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.notification.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.*;
import com.ssafy.domain.notification.entity.NotificationType;

/**
 * ApplicationService 단위 테스트
 *
 * 테스트 범위:
 * 1. 신청 생성 (createApplication)
 * 2. 스터디별 신청 목록 조회 (getApplicationByStudy)
 * 3. 사용자별 신청 내역 조회 (getApplicationByUser)
 * 4. 신청 상세 조회 (getApplication)
 * 5. 신청 승인 (approveApplication)
 * 6. 신청 거절 (rejectApplication)
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private StudyApplicationRepository applicationRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyMemberRepository studyMemberRepository;

    @Mock
    private StudyRecommendService studyRecommendService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private StudyService studyService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ApplicationService applicationService;

    // 테스트용 Topic과 Format 객체
    private Topic testTopic;
    private Format testFormat;

    @BeforeEach
    void setUp() {
        // Mock 테스트에서 사용할 Topic 객체 생성
        testTopic = Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(testTopic, "id", 1L);

        // Mock 테스트에서 사용할 Format 객체 생성
        testFormat = Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(testFormat, "id", 1L);
    }

    // ============================================================
    // 1. 신청 생성 테스트
    // ============================================================

    @Test
    @DisplayName("신청 생성 성공")
    void createApplication_Success() {
        // given
        Long studyId = 1L;
        Long userId = 10L;
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("열심히 하겠습니다!")
                .build();

        Study study = Study.builder()
                .leaderId(2L)
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        User user = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .role(Role.USER)
                .build();

        StudyApplication savedApplication = StudyApplication.builder()
                .studyId(studyId)
                .userId(userId)
                .message(request.getMessage())
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(savedApplication, "id", 1L);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(applicationRepository.existsByStudyIdAndUserId(studyId, userId)).willReturn(false);
        given(applicationRepository.save(any(StudyApplication.class))).willReturn(savedApplication);

        // when
        ApplicationResponse response = applicationService.createApplication(studyId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getApplicationId()).isEqualTo(1L);
        assertThat(response.getStudyId()).isEqualTo(studyId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getMessage()).isEqualTo("열심히 하겠습니다!");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(response.getStudyName()).isEqualTo("알고리즘 스터디");
        assertThat(response.getUserName()).isEqualTo("김싸피");

        verify(studyRepository, times(1)).findById(studyId);
        verify(userRepository, times(1)).findById(userId);
        verify(applicationRepository, times(1)).existsByStudyIdAndUserId(studyId, userId);
        verify(applicationRepository, times(1)).save(any(StudyApplication.class));
    }

    @Test
    @DisplayName("신청 생성 실패 - 존재하지 않는 스터디")
    void createApplication_StudyNotFound() {
        // given
        Long studyId = 999L;
        Long userId = 10L;
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("열심히 하겠습니다!")
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.createApplication(studyId, request, userId))
                .isInstanceOf(StudyException.StudyNotFoundException.class);

        verify(studyRepository, times(1)).findById(studyId);
        verify(userRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 생성 실패 - 존재하지 않는 사용자")
    void createApplication_UserNotFound() {
        // given
        Long studyId = 1L;
        Long userId = 999L;
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("열심히 하겠습니다!")
                .build();

        Study study = Study.builder()
                .leaderId(2L)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.createApplication(studyId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자");

        verify(studyRepository, times(1)).findById(studyId);
        verify(userRepository, times(1)).findById(userId);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 생성 실패 - 중복 신청")
    void createApplication_DuplicateApplication() {
        // given
        Long studyId = 1L;
        Long userId = 10L;
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("열심히 하겠습니다!")
                .build();

        Study study = Study.builder()
                .leaderId(2L)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        User user = User.builder().build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(applicationRepository.existsByStudyIdAndUserId(studyId, userId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> applicationService.createApplication(studyId, request, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 신청한 스터디");

        verify(applicationRepository, times(1)).existsByStudyIdAndUserId(studyId, userId);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 생성 실패 - 본인 스터디 신청")
    void createApplication_OwnStudy() {
        // given
        Long studyId = 1L;
        Long userId = 10L;
        ApplicationCreateRequest request = ApplicationCreateRequest.builder()
                .message("열심히 하겠습니다!")
                .build();

        Study study = Study.builder()
                .leaderId(userId)  // 본인이 스터디장!
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        User user = User.builder().build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(applicationRepository.existsByStudyIdAndUserId(studyId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> applicationService.createApplication(studyId, request, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인이 만든 스터디");

        verify(applicationRepository, never()).save(any());
    }

    // ============================================================
    // 2. 스터디별 신청 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 신청 목록 조회 성공 - 전체 조회")
    void getApplicationByStudy_Success() {
        // given
        Long studyId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Study study = Study.builder()
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        User user1 = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .build();

        User user2 = User.builder()
                .name("이싸피")
                .nickname("ssafy_lee")
                .email("lee@ssafy.com")
                .build();

        StudyApplication app1 = StudyApplication.builder()
                .studyId(studyId)
                .userId(10L)
                .message("신청합니다!")
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(app1, "id", 1L);

        StudyApplication app2 = StudyApplication.builder()
                .studyId(studyId)
                .userId(11L)
                .message("참여하고 싶습니다!")
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(app2, "id", 2L);

        Page<StudyApplication> applicationPage = new PageImpl<>(List.of(app1, app2));

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findByStudyId(studyId, pageable)).willReturn(applicationPage);
        given(userRepository.findById(10L)).willReturn(Optional.of(user1));
        given(userRepository.findById(11L)).willReturn(Optional.of(user2));

        // when
        Page<ApplicationResponse> response = applicationService.getApplicationByStudy(studyId, null, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getStudyName()).isEqualTo("알고리즘 스터디");
        assertThat(response.getContent().get(0).getUserName()).isEqualTo("김싸피");
        assertThat(response.getContent().get(1).getUserName()).isEqualTo("이싸피");

        verify(studyRepository, times(1)).findById(studyId);
        verify(applicationRepository, times(1)).findByStudyId(studyId, pageable);
        verify(userRepository, times(2)).findById(any());
    }

    @Test
    @DisplayName("스터디별 신청 목록 조회 성공 - 상태별 필터링")
    void getApplicationByStudy_WithStatus() {
        // given
        Long studyId = 1L;
        ApplicationStatus status = ApplicationStatus.PENDING;
        Pageable pageable = PageRequest.of(0, 10);

        Study study = Study.builder()
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication app1 = StudyApplication.builder()
                .studyId(studyId)
                .userId(10L)
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(app1, "id", 1L);

        Page<StudyApplication> applicationPage = new PageImpl<>(List.of(app1));

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findByStudyIdAndStatus(studyId, status, pageable))
                .willReturn(applicationPage);
        given(userRepository.findById(10L)).willReturn(Optional.of(User.builder().build()));

        // when
        Page<ApplicationResponse> response = applicationService.getApplicationByStudy(studyId, status, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING);

        verify(applicationRepository, times(1)).findByStudyIdAndStatus(studyId, status, pageable);
    }

    @Test
    @DisplayName("스터디별 신청 목록 조회 실패 - 존재하지 않는 스터디")
    void getApplicationByStudy_StudyNotFound() {
        // given
        Long studyId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        given(studyRepository.findById(studyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.getApplicationByStudy(studyId, null, pageable))
                .isInstanceOf(StudyException.StudyNotFoundException.class);

        verify(applicationRepository, never()).findByStudyId(any(), any());
    }

    // ============================================================
    // 3. 사용자별 신청 내역 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자별 신청 내역 조회 성공 - 전체 조회")
    void getApplicationByUser_Success() {
        // given
        Long userId = 10L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .build();

        Topic csTopic = Topic.builder()
                .name("CS")
                .sortOrder(2)
                .build();
        ReflectionTestUtils.setField(csTopic, "id", 2L);

        Study study1 = Study.builder()
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study1, "id", 1L);

        Study study2 = Study.builder()
                .name("CS 스터디")
                .topic(csTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study2, "id", 2L);

        StudyApplication app1 = StudyApplication.builder()
                .studyId(1L)
                .userId(userId)
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(app1, "id", 1L);

        StudyApplication app2 = StudyApplication.builder()
                .studyId(2L)
                .userId(userId)
                .status(ApplicationStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(app2, "id", 2L);

        Page<StudyApplication> applicationPage = new PageImpl<>(List.of(app1, app2));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(applicationRepository.findByUserId(userId, pageable)).willReturn(applicationPage);
        given(studyRepository.findById(1L)).willReturn(Optional.of(study1));
        given(studyRepository.findById(2L)).willReturn(Optional.of(study2));

        // when
        Page<ApplicationResponse> response = applicationService.getApplicationByUser(userId, null, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getUserName()).isEqualTo("김싸피");
        assertThat(response.getContent().get(0).getStudyName()).isEqualTo("알고리즘 스터디");
        assertThat(response.getContent().get(1).getStudyName()).isEqualTo("CS 스터디");

        verify(userRepository, times(1)).findById(userId);
        verify(applicationRepository, times(1)).findByUserId(userId, pageable);
        verify(studyRepository, times(2)).findById(any());
    }

    @Test
    @DisplayName("사용자별 신청 내역 조회 성공 - 상태별 필터링")
    void getApplicationByUser_WithStatus() {
        // given
        Long userId = 10L;
        ApplicationStatus status = ApplicationStatus.APPROVED;
        Pageable pageable = PageRequest.of(0, 10);

        User user = User.builder()
                .name("김싸피")
                .build();

        StudyApplication app1 = StudyApplication.builder()
                .studyId(1L)
                .userId(userId)
                .status(ApplicationStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(app1, "id", 1L);

        Page<StudyApplication> applicationPage = new PageImpl<>(List.of(app1));

        Study study = Study.builder()
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", 1L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(applicationRepository.findByUserIdAndStatus(userId, status, pageable))
                .willReturn(applicationPage);
        given(studyRepository.findById(1L)).willReturn(Optional.of(study));

        // when
        Page<ApplicationResponse> response = applicationService.getApplicationByUser(userId, status, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        verify(applicationRepository, times(1)).findByUserIdAndStatus(userId, status, pageable);
    }

    @Test
    @DisplayName("사용자별 신청 내역 조회 실패 - 존재하지 않는 사용자")
    void getApplicationByUser_UserNotFound() {
        // given
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.getApplicationByUser(userId, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자");

        verify(applicationRepository, never()).findByUserId(any(), any());
    }

    // ============================================================
    // 4. 신청 상세 조회 테스트
    // ============================================================

    @Test
    @DisplayName("신청 상세 조회 성공")
    void getApplication_Success() {
        // given
        Long applicationId = 1L;

        Study study = Study.builder()
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", 1L);

        User user = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .build();

        StudyApplication application = StudyApplication.builder()
                .studyId(1L)
                .userId(10L)
                .message("열심히 하겠습니다!")
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));
        given(studyRepository.findById(1L)).willReturn(Optional.of(study));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));

        // when
        ApplicationResponse response = applicationService.getApplication(applicationId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getApplicationId()).isEqualTo(applicationId);
        assertThat(response.getStudyName()).isEqualTo("알고리즘 스터디");
        assertThat(response.getUserName()).isEqualTo("김싸피");
        assertThat(response.getMessage()).isEqualTo("열심히 하겠습니다!");

        verify(applicationRepository, times(1)).findById(applicationId);
        verify(studyRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("신청 상세 조회 실패 - 존재하지 않는 신청")
    void getApplication_NotFound() {
        // given
        Long applicationId = 999L;

        given(applicationRepository.findById(applicationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.getApplication(applicationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 신청");

        verify(applicationRepository, times(1)).findById(applicationId);
    }

    // ============================================================
    // 5. 신청 승인 테스트
    // ============================================================

    @Test
    @DisplayName("신청 승인 성공")
    void approveApplication_Success() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;
        Long applicantUserId = 10L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication application = StudyApplication.builder()
                .studyId(studyId)
                .userId(applicantUserId)
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        User user = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));
        given(applicationRepository.save(any(StudyApplication.class))).willReturn(application);
        given(userRepository.findById(applicantUserId)).willReturn(Optional.of(user));
        given(studyMemberRepository.save(any(StudyMember.class))).willReturn(StudyMember.builder().build());

        // when
        ApplicationResponse response = applicationService.approveApplication(studyId, applicationId, leaderId);

        // then
        assertThat(response).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getProcessedAt()).isNotNull();

        verify(studyRepository, times(1)).findById(studyId);
        verify(applicationRepository, times(1)).findById(applicationId);
        verify(applicationRepository, times(1)).save(application);
        verify(studyMemberRepository, times(1)).save(any(StudyMember.class));

        // 신청자에게 승인 알림 전송 검증
        verify(notificationService, times(1)).createNotification(
                eq(applicantUserId),
                eq(NotificationType.STUDY_APPLICATION),
                eq("스터디 신청 승인"),
                contains("알고리즘 스터디"),
                eq("STUDY"),
                eq(studyId)
        );
    }

    @Test
    @DisplayName("신청 승인 실패 - 권한 없음 (스터디장 아님)")
    void approveApplication_NotLeader() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;
        Long notLeaderId = 999L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> applicationService.approveApplication(studyId, applicationId, notLeaderId))
                .isInstanceOf(StudyException.NotStudyLeaderException.class);

        verify(applicationRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any());
        verify(studyMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 승인 실패 - 존재하지 않는 신청")
    void approveApplication_ApplicationNotFound() {
        // given
        Long studyId = 1L;
        Long applicationId = 999L;
        Long leaderId = 2L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.approveApplication(studyId, applicationId, leaderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 신청");

        verify(applicationRepository, never()).save(any());
        verify(studyMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 승인 실패 - 다른 스터디의 신청")
    void approveApplication_WrongStudy() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication application = StudyApplication.builder()
                .studyId(999L)  // 다른 스터디의 신청!
                .userId(10L)
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> applicationService.approveApplication(studyId, applicationId, leaderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 스터디의 신청이 아닙니다");

        verify(applicationRepository, never()).save(any());
        verify(studyMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 승인 실패 - 이미 처리된 신청")
    void approveApplication_AlreadyProcessed() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication application = StudyApplication.builder()
                .studyId(studyId)
                .userId(10L)
                .status(ApplicationStatus.APPROVED)  // 이미 승인됨!
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> applicationService.approveApplication(studyId, applicationId, leaderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리된 신청");

        verify(applicationRepository, never()).save(any());
        verify(studyMemberRepository, never()).save(any());
    }

    // ============================================================
    // 6. 신청 거절 테스트
    // ============================================================

    @Test
    @DisplayName("신청 거절 성공")
    void rejectApplication_Success() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;
        Long applicantUserId = 10L;
        String reason = "정원 초과";

        Study study = Study.builder()
                .leaderId(leaderId)
                .name("알고리즘 스터디")
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication application = StudyApplication.builder()
                .studyId(studyId)
                .userId(applicantUserId)
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        User user = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));
        given(applicationRepository.save(any(StudyApplication.class))).willReturn(application);
        given(userRepository.findById(applicantUserId)).willReturn(Optional.of(user));

        // when
        ApplicationResponse response = applicationService.rejectApplication(studyId, applicationId, leaderId, reason);

        // then
        assertThat(response).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getRejectedReason()).isEqualTo(reason);
        assertThat(application.getProcessedAt()).isNotNull();

        verify(studyRepository, times(1)).findById(studyId);
        verify(applicationRepository, times(1)).findById(applicationId);
        verify(applicationRepository, times(1)).save(application);

        // 신청자에게 거절 알림 전송 검증
        verify(notificationService, times(1)).createNotification(
                eq(applicantUserId),
                eq(NotificationType.STUDY_APPLICATION),
                eq("스터디 신청 결과"),
                contains("알고리즘 스터디"),
                eq("STUDY"),
                eq(studyId)
        );
    }

    @Test
    @DisplayName("신청 거절 실패 - 권한 없음 (스터디장 아님)")
    void rejectApplication_NotLeader() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;
        Long notLeaderId = 999L;
        String reason = "정원 초과";

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));

        // when & then
        assertThatThrownBy(() -> applicationService.rejectApplication(studyId, applicationId, notLeaderId, reason))
                .isInstanceOf(StudyException.NotStudyLeaderException.class);

        verify(applicationRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 거절 실패 - 존재하지 않는 신청")
    void rejectApplication_ApplicationNotFound() {
        // given
        Long studyId = 1L;
        Long applicationId = 999L;
        Long leaderId = 2L;
        String reason = "정원 초과";

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> applicationService.rejectApplication(studyId, applicationId, leaderId, reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 신청");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 거절 실패 - 다른 스터디의 신청")
    void rejectApplication_WrongStudy() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;
        String reason = "정원 초과";

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication application = StudyApplication.builder()
                .studyId(999L)  // 다른 스터디의 신청!
                .userId(10L)
                .status(ApplicationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> applicationService.rejectApplication(studyId, applicationId, leaderId, reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 스터디의 신청이 아닙니다");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("신청 거절 실패 - 이미 처리된 신청")
    void rejectApplication_AlreadyProcessed() {
        // given
        Long studyId = 1L;
        Long applicationId = 1L;
        Long leaderId = 2L;
        String reason = "정원 초과";

        Study study = Study.builder()
                .leaderId(leaderId)
                .topic(testTopic)
                .format(testFormat)
                .build();
        ReflectionTestUtils.setField(study, "id", studyId);

        StudyApplication application = StudyApplication.builder()
                .studyId(studyId)
                .userId(10L)
                .status(ApplicationStatus.REJECTED)  // 이미 거절됨!
                .build();
        ReflectionTestUtils.setField(application, "id", applicationId);

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> applicationService.rejectApplication(studyId, applicationId, leaderId, reason))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리된 신청");

        verify(applicationRepository, never()).save(any());
    }
}