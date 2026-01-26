package com.ssafy.domain.study.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TopicRepository topicRepository;      // 추가
    private final FormatRepository formatRepository;    // 추가

    // ============================================================
    // 스터디 조회 API
    // ============================================================

    /**
     * 전체 스터디 목록 조회 (공개된 스터디만, DRAFT 제외)
     */
    public Page<Study> getAllStudies(Pageable pageable) {
        log.info("전체 스터디 목록 조회 - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return studyRepository.findAllPublicStudies(Status.DRAFT, pageable);
    }

    /**
     * 모집중인 스터디 목록 조회
     */
    public Page<Study> getRecruitingStudies(Pageable pageable) {
        log.info("모집중인 스터디 목록 조회 - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return studyRepository.findRecruitingStudies(pageable);
    }

    /**
     * 스터디 검색/필터링 (동적 쿼리)
     */
    public Page<Study> searchStudies(StudySearchCondition condition, Pageable pageable) {
        log.info("스터디 검색 - 조건: keyword={}, topicId={}, status={}, meetingType={}",
                condition.getKeyword(),
                condition.getTopicId(),
                condition.getStatus(),
                condition.getMeetingType());

        return studyRepository.searchStudies(condition, pageable);
    }

    /**
     * 스터디장별 스터디 목록 조회
     */
    public Page<Study> getStudiesByLeader(Long leaderId, Pageable pageable) {
        log.info("스터디장 {} 의 스터디 목록 조회", leaderId);

        return studyRepository.findByLeaderId(leaderId, pageable);
    }

    /**
     * 스터디장의 특정 상태 스터디 목록 조회
     */
    public Page<Study> getStudiesByLeaderAndStatus(Long leaderId, Status status, Pageable pageable) {
        log.info("스터디장 {} 의 {} 상태 스터디 목록 조회", leaderId, status);

        return studyRepository.findByLeaderIdAndStatus(leaderId, status, pageable);
    }

    /**
     * 스터디 상세 조회
     */
    public Study getStudyById(Long studyId) {
        log.info("스터디 상세 조회 - ID: {}", studyId);

        return studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 ID: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });
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
     */
    @Transactional
    public StudyResponse createStudy(StudyCreateRequest request, Long leaderId) {
        log.info("스터디 생성 시작 - 스터디장: {}, 스터디명: {}", leaderId, request.getName());

        // 1. 스터디장(User) 존재 확인
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자 - leaderId: {}", leaderId);
                    return NotFoundException.user();
                });

        // 2. Topic 조회 (필수)
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> {
                    log.error("존재하지 않는 주제 - topicId: {}", request.getTopicId());
                    return new StudyException.InvalidStudyRequestException("존재하지 않는 주제입니다: " + request.getTopicId());
                });

        // 3. Format 조회 (선택)
        Format format = null;
        if (request.getFormatId() != null) {
            format = formatRepository.findById(request.getFormatId())
                    .orElseThrow(() -> {
                        log.error("존재하지 않는 형식 - formatId: {}", request.getFormatId());
                        return new StudyException.InvalidStudyRequestException("존재하지 않는 형식입니다: " + request.getFormatId());
                    });
        }

        // 4. 비즈니스 검증
        validateStudyCreate(request);

        // 5. DTO -> Entity 변환 (Topic, Format 전달)
        Study study = request.toEntity(leaderId, topic, format);

        // 6. Study 저장
        Study savedStudy = studyRepository.save(study);

        // 7. 스터디장을 StudyMember로 자동 추가
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

        return StudyResponse.from(study);
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

        return StudyResponse.from(study);
    }

    /**
     * 모집 기간 연장
     */
    @Transactional
    public StudyResponse extendRecruitment(Long studyId, java.time.LocalDate newEndDate, Long leaderId) {
        log.info("모집 기간 연장 - studyId: {}, 새 종료일: {}", studyId, newEndDate);

        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 권한 확인
        if (!study.getLeaderId().equals(leaderId)) {
            throw new StudyException.NotStudyLeaderException("모집 기간을 연장할 권한이 없습니다");
        }

        // 모집 중인 상태에서만 가능
        if (study.getStatus() != Status.RECRUITING) {
            throw new StudyException.NotRecruitingException();
        }

        // 연장
        study.extendRecruitment(newEndDate);

        log.info("모집 기간 연장 완료 - studyId: {}, 연장 횟수: {}", studyId, study.getExtensionCount());

        return StudyResponse.from(study);
    }

    /**
     * 내가 참여 중인 스터디 목록 조회
     */
    public Page<Study> getMyStudies(Long userId, Pageable pageable) {
        log.info("내 스터디 목록 조회 - userId: {}", userId);

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

        List<Study> sortedStudies = allStudies.stream()
                .sorted(Comparator.comparing(Study::getCreatedAt).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        log.info("내 스터디 목록 조회 완료 - userId: {}, count: {}", userId, allStudies.size());

        return new PageImpl<>(sortedStudies, pageable, allStudies.size());
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