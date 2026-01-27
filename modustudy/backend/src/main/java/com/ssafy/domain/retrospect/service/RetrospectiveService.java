package com.ssafy.domain.retrospect.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.retrospect.dto.response.RetrospectiveListResponse;
import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.repository.RetrospectiveItemRepository;
import com.ssafy.domain.retrospect.repository.RetrospectiveRepository;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final RetrospectiveItemRepository retrospectiveItemRepository;
    private final StudyRepository studyRepository;
    private final StudySessionRepository studySessionRepository;

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

        // 최신순 정렬
        List<Retrospective> sorted = retrospectives.stream()
                .sorted(Comparator.comparing(Retrospective::getCreatedAt).reversed())
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