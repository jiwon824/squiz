package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.ApplicationCreateRequest;
import com.ssafy.domain.study.dto.response.ApplicationResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.entity.StudyRecommendAction;
import com.ssafy.domain.study.repository.StudyApplicationRepository;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.gamification.event.StudyJoinEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationService {

    private final StudyApplicationRepository applicationRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRecommendService studyRecommendService;
    private final NotificationService notificationService;
    private final StudyService studyService;
    private final ApplicationEventPublisher eventPublisher;

    // ============================================================
    // 신청 생성
    // ============================================================

    /**
     * 스터디 신청 생성
     */
    @Transactional
    public ApplicationResponse createApplication(Long studyId, ApplicationCreateRequest request, Long userId) {
        log.info("스터디 신청 생성 시작 - studyId: {}, userId: {}", studyId, userId);

        // 1. 스터디 존재 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 - studyId: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자 - userId: {}" , userId);
                    return new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
                });

        // 3. 중복 신청 확인
        if (applicationRepository.existsByStudyIdAndUserId(studyId, userId)) {
            log.warn("이미 신청한 스터디 - studyId: {}, userId: {}", studyId, userId);
            throw new IllegalStateException("이미 신청한 스터디입니다");
        }

        // 4. 본인 스터디 신청 방지
        if (study.getLeaderId().equals(userId)) {
            log.warn("본인 스터디 신청 시도 - studyId: {}, userId: {}", studyId, userId);
            throw new IllegalStateException("본인이 만든 스터디에는 신청할 수 없습니다");
        }

        // 5. 정원 초과 검증 (스터디장도 StudyMember 테이블에 포함되어 있음)
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
        if (study.getMaxMembers() != null && currentMembers >= study.getMaxMembers()) {
            log.warn("스터디 정원 초과 - studyId: {}, currentMembers: {}, maxMembers: {}",
                    studyId, currentMembers, study.getMaxMembers());
            throw new IllegalStateException("스터디 정원이 가득 찼습니다");
        }

        // 6. 신청 생성
        StudyApplication application = StudyApplication.builder()
                .studyId(studyId)
                .userId(userId)
                .message(request.getMessage())
                .status(ApplicationStatus.PENDING)
                .build();

        StudyApplication saved = applicationRepository.save(application);

        log.info("스터디 신청 생성 완료 - applicationId: {}", saved.getId());

        // 7. 추천 반응 자동 기록 (추천에서 온 지원인지 감지)
        studyRecommendService.tryLogAction(userId, studyId, StudyRecommendAction.ActionType.APPLY);

        // 8. 스터디장에게 알림 전송
        // referenceId에 studyId를 저장하여 프론트엔드에서 /study/manage/{studyId}로 이동 가능하게 함
        String notificationTitle = "새로운 스터디 신청";
        String notificationContent = String.format("%s님이 '%s' 스터디에 참가 신청을 했습니다.",
                user.getNickname() != null ? user.getNickname() : user.getName(),
                study.getName());
        notificationService.createNotification(
                study.getLeaderId(),
                NotificationType.STUDY_APPLICATION,
                notificationTitle,
                notificationContent,
                "STUDY_APPLICATION",
                studyId
        );
        log.info("스터디장에게 알림 전송 완료 - leaderId: {}", study.getLeaderId());

        // 9. DTO 변환 및 추가 정보 설정
        ApplicationResponse response = ApplicationResponse.from(saved);
        response.setStudyName(study.getName());
        response.setUserInfo(user.getName(), user.getNickname(), user.getEmail(), user.getProfileImage());

        return response;
    }

    // ============================================================
    // 신청 목록 조회
    // ============================================================

    /**
     * 스터디별 신청 목록 조회
     */
    public Page<ApplicationResponse> getApplicationByStudy(Long studyId, ApplicationStatus status, Pageable pageable) {
        log.info("스터디별 신청 목록 조회 - studyId: {}, status: {}, page: {}, size: {}",
                studyId, status, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 스터디 존재 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 - studyId: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 신청 목록 조회
        Page<StudyApplication> applications = (status != null)
                ? applicationRepository.findByStudyIdAndStatus(studyId, status, pageable)
                : applicationRepository.findByStudyId(studyId, pageable);

        log.info("신청 목록 조회 완료 - totalElements: {}", applications.getTotalElements());

        // 3. DTO 변환 및 추가 정보 설정
        return applications.map(app -> {
            ApplicationResponse response = ApplicationResponse.from(app);
            response.setStudyName(study.getName());

            // 신청자 정보 설정
            userRepository.findById(app.getUserId()).ifPresent(user ->
                    response.setUserInfo(user.getName(), user.getNickname(), user.getEmail(), user.getProfileImage())
            );

            return response;
        });
    }

    /**
     * 사용자별 신청 내역 조회
     */
    public Page<ApplicationResponse> getApplicationByUser(Long userId, ApplicationStatus status, Pageable pageable) {
        log.info("사용자별 신청 내역 조회 - userId: {}, status: {}, page: {}, size: {}",
                userId, status, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자 - userId: {}", userId);
                    return new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
                });

        // 2. 신청 내역 조회
        Page<StudyApplication> applications = (status != null)
                ? applicationRepository.findByUserIdAndStatus(userId, status, pageable)
                : applicationRepository.findByUserId(userId, pageable);

        log.info("신청 내역 조회 완료 - totalElements: {}", applications.getTotalElements());

        // 3. DTO 변환 및 추가 정보 설정
        return applications.map(app -> {
            ApplicationResponse response =  ApplicationResponse.from(app);
            response.setUserInfo(user.getName(), user.getNickname(), user.getEmail(), user.getProfileImage());

            // 스터디 정보 설정
            studyRepository.findById(app.getStudyId()).ifPresent(study ->
                    response.setStudyName(study.getName())
            );

            return response;
        });
    }

    /**
     * 신청 상세 조회
     */
    public ApplicationResponse getApplication(Long applicationId) {
        log.info("신청 상세 조회 - applicationId: {}", applicationId);

        StudyApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 신청 - applicationId: {}", applicationId);
                    return new IllegalArgumentException("존재하지 않는 신청입니다: " + applicationId);
                });

        ApplicationResponse response = ApplicationResponse.from(application);

        // 추가 정보 설정
        studyRepository.findById(application.getStudyId()).ifPresent(study ->
                response.setStudyName(study.getName())
        );

        userRepository.findById(application.getUserId()).ifPresent(user ->
                response.setUserInfo(user.getName(), user.getNickname(), user.getEmail(), user.getProfileImage())
        );

        log.info("신청 상세 조회 완료 - applicationId: {}", applicationId);

        return response;
    }

    // ============================================================
    // 신청 승인/거절
    // ============================================================

    /**
     * 신청 승인
     */
    @Transactional
    public ApplicationResponse approveApplication(Long studyId, Long applicationId, Long leaderId) {
        log.info("신청 승인 시작 - studyId: {}, applicationId: {}, leaderId: {}", studyId, applicationId, leaderId);

        // 1. 스터디 존재 및 권한 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        if (!study.getLeaderId().equals(leaderId)) {
            log.warn("권한 없음 - studyId: {}, leaderId: {}", studyId, leaderId);
            throw new StudyException.NotStudyLeaderException("스터디장만 신청을 승인할 수 있습니다");
        }

        // 2. 신청 조회
        StudyApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> {
                    log.error("존재하지 않은 신청 - applicationId: {}", applicationId);
                    return new IllegalArgumentException("존재하지 않는 신청입니다: " + applicationId);
                });

        // 3. 신청이 해당 스터디의 것인지 확인
        if (!application.getStudyId().equals(studyId)) {
            log.warn("잘못된 신청 - applicationId: {}, studyId: {}", applicationId, studyId);
            throw new IllegalArgumentException("해당 스터디의 신청이 아닙니다");
        }

        // 4. 이미 처리된 신청인지 확인
        if (!application.isPending()) {
            log.warn("이미 처리된 신청 - applicationId: {}, status: {}", applicationId, application.getStatus());
            throw new IllegalStateException("이미 처리된 신청입니다");
        }

        // 5. 정원 초과 검증 (승인 시점 double-check, 스터디장도 StudyMember에 포함됨)
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
        if (study.getMaxMembers() != null && currentMembers >= study.getMaxMembers()) {
            log.warn("스터디 정원 초과로 승인 불가 - studyId: {}, currentMembers: {}, maxMembers: {}",
                    studyId, currentMembers, study.getMaxMembers());
            throw new IllegalStateException("스터디 정원이 가득 차서 승인할 수 없습니다");
        }

        // 6. 승인 처리
        application.approve();
        StudyApplication updated = applicationRepository.save(application);

        // 7. ⭐ StudyMember 추가 (핵심 로직)
        StudyMember member = StudyMember.builder()
                .studyId(studyId)
                .userId(application.getUserId())
                .role(MemberRole.MEMBER)
                .status(MemberStatus.APPROVED)
                .joinedAt(LocalDateTime.now())
                .isProbation(true)
                .build();

        studyMemberRepository.save(member);

        log.info("신청 승인 완료 - applicationId: {}, userId: {} 스터디 멤버로 추가됨",
                applicationId, application.getUserId());

        // 8. 게이미피케이션 이벤트 발행 - 스터디 가입
        // 첫 스터디 여부 확인 (현재 가입한 스터디 제외하고 다른 승인된 멤버십이 있는지)
        int otherMemberships = studyMemberRepository.findByUserIdAndStatus(application.getUserId(), MemberStatus.APPROVED).size();
        boolean isFirstStudy = otherMemberships <= 1; // 방금 가입한 것만 있으면 첫 스터디

        eventPublisher.publishEvent(new StudyJoinEvent(
                application.getUserId(),
                studyId,
                study.getName(),
                LocalDate.now(),
                isFirstStudy
        ));

        // 9. 모집 인원 충족 여부 확인 및 상태 변경
        studyService.checkAndUpdateRecruitmentStatus(studyId);

        // 10. DTO 변환
        ApplicationResponse response = ApplicationResponse.from(updated);
        response.setStudyName(study.getName());

        userRepository.findById(application.getUserId()).ifPresent(user ->
                response.setUserInfo(user.getName(), user.getNickname(), user.getEmail(), user.getProfileImage())
        );

        return response;
    }

    /**
     * 신청 거절
     */
    @Transactional
    public ApplicationResponse rejectApplication(Long studyId, Long applicationId, Long leaderId, String rejectedReason) {
        log.info("신청 거절 시작 - studyId: {}, applicationId: {}, leaderId: {}", studyId, applicationId, leaderId);

        // 1. 스터디 존재 및 권한 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        if (!study.getLeaderId().equals(leaderId)) {
            log.warn("권한 없음 - studyId: {}, leaderId: {}", studyId, leaderId);
            throw new StudyException.NotStudyLeaderException("스터디장만 신청을 거절할 수 있습니다");
        }

        // 2. 신청 조회
        StudyApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 신청 - applicationId: {}", applicationId);
                    return new IllegalArgumentException("존재하지 않는 신청입니다: " + applicationId);
                });

        // 3. 신청이 해당 스터디의 것인지 확인
        if (!application.getStudyId().equals(studyId)) {
            log.warn("잘못된 신청 - applicationId: {}, studyId: {}", applicationId, studyId);
            throw new IllegalArgumentException("해당 스터디의 신청이 아닙니다");
        }

        // 4. 이미 처리된 신청인지 확인
        if (!application.isPending()) {
            log.warn("이미 처리된 신청 - applicationId: {}, status: {}", applicationId, application.getStatus());
            throw new IllegalStateException("이미 처리된 신청입니다");
        }

        // 5. 거절 처리
        application.reject(rejectedReason);
        StudyApplication updated = applicationRepository.save(application);

        log.info("신청 거절 완료 - applicationId: {}, reason: {}", applicationId, rejectedReason);

        // 6. DTO 변환
        ApplicationResponse response = ApplicationResponse.from(updated);
        response.setStudyName(study.getName());

        userRepository.findById(application.getUserId()).ifPresent(user ->
                response.setUserInfo(user.getName(), user.getNickname(), user.getEmail(), user.getProfileImage())
        );

        return response;
    }
}
