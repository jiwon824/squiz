package com.ssafy.domain.study.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.*;
import com.ssafy.domain.study.workspace.service.WorkspaceService;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.gamification.event.StudyCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyService {

    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final TopicRepository topicRepository;
    private final FormatRepository formatRepository;
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // ============================================================
    // 스터디 조회 API
    // ============================================================

    /**
     * 전체 스터디 목록 조회 (공개된 스터디만, DRAFT 제외)
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public Page<StudyResponse> getAllStudies(Pageable pageable) {
        log.info("전체 스터디 목록 조회 - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Study> studies = studyRepository.findAllPublicStudies(Status.DRAFT, pageable);
        return studies.map(study -> {
            int currentMembers = studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED);
            StudyResponse response = StudyResponse.from(study);
            response.setCurrentMembers(currentMembers);
            return response;
        });
    }

    /**
     * 모집중인 스터디 목록 조회
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public Page<StudyResponse> getRecruitingStudies(Pageable pageable) {
        log.info("모집중인 스터디 목록 조회 - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Study> studies = studyRepository.findRecruitingStudies(pageable);
        return studies.map(study -> {
            int currentMembers = studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED);
            StudyResponse response = StudyResponse.from(study);
            response.setCurrentMembers(currentMembers);
            return response;
        });
    }

    /**
     * 스터디 검색/필터링 (동적 쿼리)
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public Page<StudyResponse> searchStudies(StudySearchCondition condition, Pageable pageable) {
        log.info("스터디 검색 - 조건: keyword={}, topicId={}, status={}, meetingType={}",
                condition.getKeyword(),
                condition.getTopicId(),
                condition.getStatus(),
                condition.getMeetingType());

        Page<Study> studies = studyRepository.searchStudies(condition, pageable);
        return studies.map(study -> {
            int currentMembers = studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED);
            StudyResponse response = StudyResponse.from(study);
            response.setCurrentMembers(currentMembers);
            return response;
        });
    }

    /**
     * 스터디장별 스터디 목록 조회
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public Page<StudyResponse> getStudiesByLeader(Long leaderId, Pageable pageable) {
        log.info("스터디장 {} 의 스터디 목록 조회", leaderId);

        Page<Study> studies = studyRepository.findByLeaderId(leaderId, pageable);
        return studies.map(study -> {
            int currentMembers = studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED);
            StudyResponse response = StudyResponse.from(study);
            response.setCurrentMembers(currentMembers);
            return response;
        });
    }

    /**
     * 스터디장의 특정 상태 스터디 목록 조회
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public Page<StudyResponse> getStudiesByLeaderAndStatus(Long leaderId, Status status, Pageable pageable) {
        log.info("스터디장 {} 의 {} 상태 스터디 목록 조회", leaderId, status);

        Page<Study> studies = studyRepository.findByLeaderIdAndStatus(leaderId, status, pageable);
        return studies.map(study -> {
            int currentMembers = studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED);
            StudyResponse response = StudyResponse.from(study);
            response.setCurrentMembers(currentMembers);
            return response;
        });
    }

    /**
     * 스터디 상세 조회
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public StudyResponse getStudyById(Long studyId) {
        log.info("스터디 상세 조회 - ID: {}", studyId);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 ID: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 스터디장 정보 조회
        User leader = null;
        if (study.getLeaderId() != null) {
            leader = userRepository.findById(study.getLeaderId()).orElse(null);
        }

        // 현재 참여 인원 조회 (스터디장도 StudyMember 테이블에 포함되어 있음)
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);

        return StudyResponse.from(study, leader, currentMembers);
    }

    /**
     * 특정 상태의 스터디 개수 조회
     */
    public Long countStudiesByStatus(Status status) {
        log.info("스터디 개수 조회 - 상태: {}", status);

        return studyRepository.countByStatus(status);
    }

    /**
     * 스터디 존재 여부 확인
     */
    public boolean existsStudy(Long studyId) {
        boolean exists = studyRepository.existsById(studyId);
        log.info("스터디 존재 확인 - ID: {}, 존재 여부: {}", studyId, exists);

        return exists;
    }

    // ============================================================
    // 스터디 CRUD API
    // ============================================================

    /**
     * 스터디 생성
     *
     * 성능 최적화:
     * - EXISTS 쿼리로 첫 스터디 여부 확인 (COUNT보다 빠름)
     * - 첫 스터디 체크를 INSERT 전에 수행하여 불필요한 쿼리 제거
     */
    @Transactional
    public StudyResponse createStudy(StudyCreateRequest request, Long leaderId) {
        log.info("스터디 생성 시작 - 스터디장: {}, 스터디명: {}", leaderId, request.getName());

        // 1. 첫 스터디 여부 확인 (INSERT 전에 체크 - EXISTS 쿼리 사용)
        boolean isFirstStudy = !studyRepository.existsByLeaderId(leaderId);

        // 2. 스터디장(User) 존재 확인
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자 - leaderId: {}", leaderId);
                    return NotFoundException.user();
                });

        // 3. Topic 조회 (필수)
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> {
                    log.error("존재하지 않는 주제 - topicId: {}", request.getTopicId());
                    return new StudyException.InvalidStudyRequestException("존재하지 않는 주제입니다: " + request.getTopicId());
                });

        // 4. Format 조회 (선택)
        Format format = null;
        if (request.getFormatId() != null) {
            format = formatRepository.findById(request.getFormatId())
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 형식 - formatId: {}", request.getFormatId());
                        return new StudyException.InvalidStudyRequestException("존재하지 않는 형식입니다: " + request.getFormatId());
                    });
        }

        // 5. 비즈니스 검증
        validateStudyCreate(request);

        // 6. DTO -> Entity 변환 (Topic, Format 전달)
        Study study = request.toEntity(leaderId, topic, format);

        // 7. Study 저장
        Study savedStudy = studyRepository.save(study);

        // 8. 스터디장을 StudyMember로 자동 추가
        StudyMember leaderMember = StudyMember.builder()
                .studyId(savedStudy.getId())
                .userId(leaderId)
                .role(MemberRole.LEADER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .joinedAt(LocalDateTime.now())
                .build();

        studyMemberRepository.save(leaderMember);

        log.info("스터디 생성 완료 - studyId: {}, topicId: {}, formatId: {}",
                savedStudy.getId(), topic.getId(), format != null ? format.getId() : null);

        // 9. 게이미피케이션 이벤트 발행 (비동기 처리됨)
        eventPublisher.publishEvent(new StudyCreateEvent(
                leaderId,
                savedStudy.getId(),
                savedStudy.getName(),
                LocalDate.now(),
                isFirstStudy
        ));

        return StudyResponse.from(savedStudy, leader);
    }

    /**
     * 스터디 수정
     */
    @Transactional
    public StudyResponse updateStudy(Long studyId, StudyUpdateRequest request, Long leaderId) {
        log.info("스터디 수정 시작 - studyId: {}, 요청자: {}", studyId, leaderId);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 권한 확인 (스터디장만 수정 가능)
        if (!study.getLeaderId().equals(leaderId)) {
            throw new StudyException.NotStudyLeaderException("스터디를 수정할 권한이 없습니다");
        }

        validateStudyUpdate(request);

        // Topic 조회 (요청에 있는 경우만)
        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new StudyException.InvalidStudyRequestException("존재하지 않는 주제입니다: " + request.getTopicId()));
        }

        // Format 조회 (요청에 있는 경우만)
        Format format = null;
        if (request.getFormatId() != null) {
            format = formatRepository.findById(request.getFormatId())
                    .orElseThrow(() -> new StudyException.InvalidStudyRequestException("존재하지 않는 형식입니다: " + request.getFormatId()));
        }

        request.updateEntity(study, topic, format);

        log.info("스터디 수정 완료 - studyId: {}", studyId);

        // 스터디장 정보 조회
        User leader = userRepository.findById(leaderId).orElse(null);

        return StudyResponse.from(study, leader);
    }

    /**
     * 스터디 삭제
     */
    @Transactional
    public void deleteStudy(Long studyId, Long leaderId) {
        log.info("스터디 삭제 시작 - studyId: {}, 요청자: {}", studyId, leaderId);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 권한 확인 (스터디장만 삭제 가능)
        if (!study.getLeaderId().equals(leaderId)) {
            throw new StudyException.NotStudyLeaderException("스터디를 삭제할 권한이 없습니다");
        }

        // 진행 중이거나 완료된 스터디는 삭제 불가
        if (study.getStatus() == Status.IN_PROGRESS || study.getStatus() == Status.COMPLETED) {
            throw new StudyException.CannotDeleteStudyException();
        }

        studyRepository.delete(study);

        log.info("스터디 삭제 완료 - studyId: {}", studyId);
    }

    /**
     * 스터디 상태 변경
     */
    @Transactional
    public StudyResponse updateStudyStatus(Long studyId, Status newStatus, Long leaderId) {
        log.info("스터디 상태 변경 - studyId: {}, 새 상태: {}", studyId, newStatus);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 권한 확인
        if (!study.getLeaderId().equals(leaderId)) {
            throw new StudyException.NotStudyLeaderException("스터디 상태를 변경할 권한이 없습니다");
        }

        // 상태 변환 검증
        validateStatusTransition(study.getStatus(), newStatus);

        // 상태 변경
        study.updateStatus(newStatus);

        log.info("스터디 상태 변경 완료 - studyId: {}, 이전: {} -> 새: {}",
                studyId, study.getStatus(), newStatus);

        // 스터디장 정보 조회
        User leader = userRepository.findById(leaderId).orElse(null);

        return StudyResponse.from(study, leader);
    }

    /**
     * 모집 기간 연장
     * - 모집중 또는 확정대기 상태에서만 가능
     * - 최대 1회 연장 가능
     * - 확정대기 상태에서 연장 시 모집중으로 상태 변경 + 팀원들에게 알림
     */
    @Transactional
    public StudyResponse extendRecruitment(Long studyId, LocalDate newEndDate, Long leaderId) {
        log.info("모집 기간 연장 - studyId: {}, 새 종료일: {}", studyId, newEndDate);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 권한 확인
        if (!study.getLeaderId().equals(leaderId)) {
            throw new StudyException.NotStudyLeaderException("모집 기간을 연장할 권한이 없습니다");
        }

        // 모집 연장 가능 여부 확인 (모집중 또는 확정대기 상태)
        if (!study.canExtendRecruitment()) {
            throw new StudyException.NotRecruitingException();
        }

        Status previousStatus = study.getStatus();

        // 연장
        study.extendRecruitment(newEndDate);

        // 확정대기 상태였다면 모집중으로 변경하고 팀원들에게 알림
        if (previousStatus == Status.PENDING) {
            study.updateStatus(Status.RECRUITING);

            // 팀원들에게 연장 알림 발송
            List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
            for (StudyMember member : members) {
                if (!member.getUserId().equals(leaderId)) {
                    notificationService.createNotification(
                            member.getUserId(),
                            NotificationType.STUDY_EXTENSION,
                            "스터디 모집 기간 연장",
                            String.format("'%s' 스터디의 모집 기간이 %s까지 연장되었습니다. 계속 참여하시겠습니까?",
                                    study.getName(), newEndDate),
                            "STUDY",
                            studyId
                    );
                }
            }
        }

        log.info("모집 기간 연장 완료 - studyId: {}, 연장 횟수: {}, 상태: {} -> {}",
                studyId, study.getExtensionCount(), previousStatus, study.getStatus());

        // 스터디장 정보 조회
        User leader = userRepository.findById(leaderId).orElse(null);

        return StudyResponse.from(study, leader);
    }

    /**
     * 스터디 시작하기
     * - 스터디장만 호출 가능
     * - 모집완료/시작대기 또는 확정대기 상태에서만 가능
     * - 워크스페이스 생성 + 상태를 진행중으로 변경
     * - 모든 팀원에게 스터디 시작 알림 발송
     */
    @Transactional
    public StudyResponse startStudy(Long studyId, Long leaderId) {
        log.info("스터디 시작 - studyId: {}, leaderId: {}", studyId, leaderId);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 권한 확인 (스터디장만 시작 가능)
        if (!study.getLeaderId().equals(leaderId)) {
            throw new StudyException.NotStudyLeaderException("스터디를 시작할 권한이 없습니다");
        }

        // 시작 가능 상태 확인
        if (!study.canStart()) {
            throw new StudyException.CannotStartStudyException();
        }

        // 워크스페이스 생성 (이미 존재하면 예외 발생)
        if (workspaceService.existsWorkspace(studyId)) {
            throw new StudyException.WorkspaceAlreadyExistsException();
        }
        workspaceService.createWorkspace(studyId);

        // 상태 변경
        study.start();

        // 모든 팀원에게 스터디 시작 알림 발송
        List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
        for (StudyMember member : members) {
            notificationService.createNotification(
                    member.getUserId(),
                    NotificationType.STUDY_START,
                    "스터디가 시작되었습니다!",
                    String.format("'%s' 스터디가 시작되었습니다. 워크스페이스에서 팀원들과 소통해보세요!", study.getName()),
                    "STUDY",
                    studyId
            );
        }

        log.info("스터디 시작 완료 - studyId: {}, 새 상태: {}", studyId, study.getStatus());

        // 스터디장 정보 조회
        User leader = userRepository.findById(leaderId).orElse(null);
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);

        return StudyResponse.from(study, leader, currentMembers);
    }

    /**
     * 모집 인원 충족 시 상태 변경 및 스터디장 알림
     * - 신규 멤버 승인 시 호출
     * - 현재 인원이 maxMembers에 도달하면 RECRUIT_CLOSED로 변경 + 스터디장에게 알림
     */
    @Transactional
    public void checkAndUpdateRecruitmentStatus(Long studyId) {
        log.info("모집 인원 충족 여부 확인 - studyId: {}", studyId);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 모집중 상태가 아니면 스킵
        if (study.getStatus() != Status.RECRUITING) {
            return;
        }

        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(studyId, MemberStatus.APPROVED);
        Integer maxMembers = study.getMaxMembers();

        if (maxMembers != null && currentMembers >= maxMembers) {
            // 모집 완료로 상태 변경
            study.updateStatus(Status.RECRUIT_CLOSED);

            // 스터디장에게 모집 완료 알림
            notificationService.createNotification(
                    study.getLeaderId(),
                    NotificationType.STUDY_RECRUITMENT_COMPLETE,
                    "모집이 완료되었습니다!",
                    String.format("'%s' 스터디의 모집 인원이 모두 찼습니다. 스터디를 시작해주세요!", study.getName()),
                    "STUDY",
                    studyId
            );

            log.info("모집 완료로 상태 변경 - studyId: {}, 현재 인원: {}/{}", studyId, currentMembers, maxMembers);
        }
    }

    /**
     * 내가 참여 중인 스터디 목록 조회
     * - status 파라미터로 특정 상태의 스터디만 필터링 가능
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    public Page<StudyResponse> getMyStudies(Long userId, Status status, Pageable pageable) {
        log.info("내 스터디 목록 조회 - userId: {}, status: {}", userId, status);

        // 1. 내가 멤버인 스터디 ID 목록 조회
        List<StudyMember> myMemberships = studyMemberRepository.findByUserIdAndStatus(userId, MemberStatus.APPROVED);

        List<Long> memberStudyIds = myMemberships.stream()
                .map(StudyMember::getStudyId)
                .toList();

        // 2. 내가 스터디장인 스터디 조회
        Page<Study> leaderStudies = studyRepository.findByLeaderId(userId, pageable);

        // 3. 멤버로 참여한 스터디 조회
        List<Study> memberStudies = memberStudyIds.isEmpty()
                ? Collections.emptyList()
                : studyRepository.findAllById(memberStudyIds);

        // 4. 두 목록 합치기 (중복 제거)
        Set<Study> allStudies = new HashSet<>(leaderStudies.getContent());
        allStudies.addAll(memberStudies);

        // 5. 상태 필터링 (status 파라미터가 있는 경우)
        if (status != null) {
            allStudies = allStudies.stream()
                    .filter(study -> study.getStatus() == status)
                    .collect(java.util.stream.Collectors.toSet());
        }

        // 6. 스터디장 정보 일괄 조회 (N+1 방지)
        Set<Long> leaderIds = allStudies.stream()
                .map(Study::getLeaderId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, User> leaderMap = userRepository.findAllById(leaderIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        // 7. 정렬 및 페이징 후 DTO 변환 (스터디장 정보 + isLeader 포함)
        List<StudyResponse> sortedResponses = allStudies.stream()
                .sorted(Comparator.comparing(Study::getCreatedAt).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(study -> {
                    StudyResponse response = StudyResponse.from(study, leaderMap.get(study.getLeaderId()));
                    // 현재 사용자가 스터디장인지 여부 설정
                    response.setIsLeader(userId.equals(study.getLeaderId()));
                    return response;
                })
                .toList();

        log.info("내 스터디 목록 조회 완료 - userId: {}, status: {}, count: {}", userId, status, allStudies.size());

        return new PageImpl<>(sortedResponses, pageable, allStudies.size());
    }

    // ============================================================
    // Private 검증 메서드
    // ============================================================

    private void validateStudyCreate(StudyCreateRequest request) {
        if (!request.isValidLocation()) {
            throw new StudyException.InvalidStudyRequestException("오프라인/혼합 스터디는 지역 정보가 필수입니다");
        }

        if (!request.isValidDateRange()) {
            throw new StudyException.InvalidStudyRequestException("종료일은 시작일보다 늦어야 합니다");
        }

        if (!request.isValidRecruitmentPeriod()) {
            throw new StudyException.InvalidStudyRequestException("모집 종료일은 스터디 시작일보다 앞서야 합니다");
        }
    }

    private void validateStudyUpdate(StudyUpdateRequest request) {
        if (!request.isValidLocation()) {
            throw new StudyException.InvalidStudyRequestException("오프라인/혼합 스터디는 지역 정보가 필수입니다");
        }

        if (!request.isValidDateRange()) {
            throw new StudyException.InvalidStudyRequestException("종료일은 시작일보다 늦어야 합니다");
        }
    }

    private void validateStatusTransition(Status currentStatus, Status newStatus) {
        if (currentStatus == Status.COMPLETED) {
            throw new StudyException.InvalidStatusTransitionException("완료된 스터디는 상태를 변경할 수 없습니다");
        }

        if (currentStatus == Status.CANCELLED) {
            throw new StudyException.InvalidStatusTransitionException("취소된 스터디는 상태를 변경할 수 없습니다");
        }

        if (currentStatus == Status.IN_PROGRESS) {
            if (newStatus != Status.COMPLETED && newStatus != Status.CANCELLED) {
                throw new StudyException.InvalidStatusTransitionException("진행 중인 스터디는 완료나 취소만 가능합니다");
            }
        }
    }
}