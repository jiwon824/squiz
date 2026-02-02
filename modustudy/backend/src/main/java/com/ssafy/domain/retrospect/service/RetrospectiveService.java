package com.ssafy.domain.retrospect.service;

import com.ssafy.common.exception.RetrospectiveException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.retrospect.dto.request.RetrospectiveCreateRequest;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveDetailResponse;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveListResponse;
import com.ssafy.domain.retrospect.entity.Category;
import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveItem;
import com.ssafy.domain.retrospect.repository.RetrospectiveItemRepository;
import com.ssafy.domain.retrospect.repository.RetrospectiveRepository;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.gamification.event.RetrospectiveWriteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final RetrospectiveItemRepository retrospectiveItemRepository;
    private final StudyRepository studyRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 스터디별 회고 목록 조회 (최신순)
     */
    public Page<RetrospectiveListResponse> getRetrospectives(Long studyId, Long userId, Pageable pageable) {
        log.info("회고 목록 조회 - studyId: {}, userId: {}", studyId, userId);

        // 스터디 존재 여부 확인
        if (!studyRepository.existsById(studyId)) {
            throw new StudyException.StudyNotFoundException(studyId);
        }

        // 회고 목록 조회
        List<Retrospective> retrospectives = retrospectiveRepository.findByStudyId(studyId);

        // 최신순 정렬 (createdAt 역순, 같으면 id 역순)
        List<Retrospective> sorted = retrospectives.stream()
                .sorted(Comparator
                        .comparing(Retrospective::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparing(Retrospective::getId).reversed()))
                .toList();

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());

        if (start > sorted.size()) {
            return new PageImpl<>(List.of(), pageable, sorted.size());
        }

        List<Retrospective> pagedList = sorted.subList(start, end);

        // DTO 변환
        List<RetrospectiveListResponse> content = pagedList.stream()
                .map(retro -> toListResponse(retro, userId))
                .toList();

        log.info("회고 목록 조회 완료 - studyId: {}, count: {}", studyId, content.size());

        return new PageImpl<>(content, pageable, sorted.size());
    }

    /**
     * 회고 상세 조회
     */
    public RetrospectiveDetailResponse getRetrospectiveDetail(Long studyId, Long retroId) {
        log.info("회고 상세 조회 - studyId: {}, retroId: {}", studyId, retroId);

        // 스터디 존재 여부 확인
        if (!studyRepository.existsById(studyId)) {
            throw new StudyException.StudyNotFoundException(studyId);
        }

        // 회고 조회
        Retrospective retrospective = retrospectiveRepository.findByIdAndStudyId(retroId, studyId)
                .orElseThrow(RetrospectiveException.RetrospectiveNotFoundException::new);

        // 세션 정보 조회
        StudySession session = null;
        if (retrospective.getSessionId() != null) {
            session = studySessionRepository.findById(retrospective.getSessionId()).orElse(null);
        }

        // 회고 항목 조회 및 카테고리별 그룹핑
        List<RetrospectiveItem> items = retrospectiveItemRepository.findByRetrospectiveId(retroId);

        // 항목을 생성순으로 정렬 후 카테고리별로 그룹핑
        Map<Category, List<RetrospectiveDetailResponse.ItemResponse>> itemsMap = items.stream()
                .sorted(Comparator.comparing(RetrospectiveItem::getCreatedAt))
                .collect(Collectors.groupingBy(
                        RetrospectiveItem::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                item -> {
                                    User user = userRepository.findById(item.getUserId()).orElse(null);
                                    return RetrospectiveDetailResponse.ItemResponse.of(item, user);
                                },
                                Collectors.toList()
                        )
                ));

        log.info("회고 상세 조회 완료 - retroId: {}, itemCount: {}", retroId, items.size());

        return RetrospectiveDetailResponse.of(retrospective, session, itemsMap);
    }

    /**
     * 회고 생성 (스터디 멤버 누구나)
     */
    @Transactional
    public RetrospectiveDetailResponse createRetrospective(Long studyId, RetrospectiveCreateRequest request, Long userId) {
        log.info("회고 생성 - studyId: {}, userId: {}, title: {}", studyId, userId, request.getTitle());

        // 스터디 존재 여부 확인
        if (!studyRepository.existsById(studyId)) {
            throw new StudyException.StudyNotFoundException(studyId);
        }

        // TODO: 스터디 멤버 여부 확인 (StudyMemberRepository 필요)

        // 세션 ID가 있으면 유효성 검증
        if (request.getSessionId() != null) {
            boolean sessionExists = studySessionRepository.existsById(request.getSessionId());
            if (!sessionExists) {
                throw new RetrospectiveException.InvalidRetrospectiveRequestException("존재하지 않는 세션입니다.");
            }
        }

        // 회고 생성
        Retrospective retrospective = request.toEntity(studyId, userId);
        Retrospective saved = retrospectiveRepository.save(retrospective);

        log.info("회고 생성 완료 - retroId: {}", saved.getId());

        // 스터디 이름 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 게이미피케이션 이벤트 발행 - 회고록 작성
        eventPublisher.publishEvent(new RetrospectiveWriteEvent(
                userId,
                studyId,
                study.getName(),
                saved.getId(),
                LocalDate.now()
        ));

        // 상세 조회로 응답 반환
        return getRetrospectiveDetail(studyId, saved.getId());
    }

    /**
     * 회고 삭제 (스터디장 또는 생성자)
     */
    @Transactional
    public void deleteRetrospective(Long studyId, Long retroId, Long userId) {
        log.info("회고 삭제 - studyId: {}, retroId: {}, userId: {}", studyId, retroId, userId);

        // 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        // 회고 조회
        Retrospective retrospective = retrospectiveRepository.findByIdAndStudyId(retroId, studyId)
                .orElseThrow(RetrospectiveException.RetrospectiveNotFoundException::new);

        // 권한 확인: 스터디장 또는 생성자
        boolean isLeader = study.getLeaderId().equals(userId);
        boolean isCreator = retrospective.getCreatedBy().equals(userId);

        if (!isLeader && !isCreator) {
            throw new RetrospectiveException.NotRetrospectiveOwnerException();
        }

        // 회고 항목 먼저 삭제
        retrospectiveItemRepository.deleteByRetrospectiveId(retroId);

        // 회고 삭제
        retrospectiveRepository.delete(retrospective);

        log.info("회고 삭제 완료 - retroId: {}", retroId);
    }

    /**
     * Retrospective -> RetrospectiveListResponse 변환
     */
    private RetrospectiveListResponse toListResponse(Retrospective retrospective, Long userId) {
        // 세션 정보 조회
        StudySession session = null;
        if (retrospective.getSessionId() != null) {
            session = studySessionRepository.findById(retrospective.getSessionId()).orElse(null);
        }

        // 항목 개수
        int itemCount = retrospectiveItemRepository.countByRetrospectiveId(retrospective.getId()).intValue();

        // 참여자 수
        int participantCount = retrospectiveItemRepository.countDistinctUserByRetrospectiveId(retrospective.getId()).intValue();

        // 내 항목 존재 여부
        Boolean hasMyItem = retrospectiveItemRepository.existsByRetrospectiveIdAndUserId(
                retrospective.getId(), userId);

        return RetrospectiveListResponse.of(
                retrospective,
                session,
                itemCount,
                participantCount,
                hasMyItem
        );
    }
}