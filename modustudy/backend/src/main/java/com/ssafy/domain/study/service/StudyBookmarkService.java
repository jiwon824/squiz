package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.response.StudyBookmarkResponse;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyBookmark;
import com.ssafy.domain.study.entity.StudyRecommendAction;
import com.ssafy.domain.study.repository.StudyBookmarkRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyBookmarkService {

    private final StudyBookmarkRepository bookmarkRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StudyRecommendService studyRecommendService;

    // ============================================================
    // 북마크 토글 (추가/삭제)
    // ============================================================

    /**
     * 북마크 토글 (있으면 삭제, 없으면 추가)
     */
    @Transactional
    public StudyBookmarkResponse toggleBookmark(Long studyId, Long userId) {
// 1. 스터디 존재 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
        }

        // 3. 기존 북마크 확인 후 토글
        return bookmarkRepository.findByUserIdAndStudyId(userId, studyId)
                .map(existing -> {
                    // 이미 존재하면 삭제
                    bookmarkRepository.delete(existing);
                    return StudyBookmarkResponse.unbookmarked(studyId);
                })
                .orElseGet(() -> {
                    // 존재하지 않으면 추가
                    StudyBookmark bookmark = StudyBookmark.create(userId, studyId);
                    StudyBookmark saved = bookmarkRepository.save(bookmark);
// 추천 반응 자동 기록
                    studyRecommendService.tryLogAction(userId, studyId, StudyRecommendAction.ActionType.BOOKMARK);
                    return StudyBookmarkResponse.from(saved);
                });
    }

    // ============================================================
    // 북마크 조회
    // ============================================================

    /**
     * 내 북마크 목록 조회 (스터디 정보 포함)
     */
    public Page<StudyBookmarkResponse> getMyBookmarks(Long userId, Pageable pageable) {
// 1. 북마크 목록 조회
        Page<StudyBookmark> bookmarks = bookmarkRepository.findByUserId(userId, pageable);

        // 2. 스터디 ID 목록 추출
        List<Long> studyIds = bookmarks.getContent().stream()
                .map(StudyBookmark::getStudyId)
                .toList();

        // 3. 스터디 정보 일괄 조회 (N+1 방지)
        Map<Long, Study> studyMap = studyRepository.findAllById(studyIds).stream()
                .collect(Collectors.toMap(Study::getId, study -> study));

        // 4. 북마크 개수 일괄 조회 (N+1 방지)
        Map<Long, Long> bookmarkCountMap = getBookmarkCountMap(studyIds);

        // 5. DTO 변환
        Page<StudyBookmarkResponse> response = bookmarks.map(bookmark -> {
            Study study = studyMap.get(bookmark.getStudyId());
            Long count = bookmarkCountMap.getOrDefault(bookmark.getStudyId(), 0L);
            return StudyBookmarkResponse.from(bookmark, study, count);
        });

        return response;
    }

    /**
     * 특정 스터디 북마크 여부 확인
     */
    public boolean isBookmarked(Long studyId, Long userId) {
        boolean result = bookmarkRepository.existsByUserIdAndStudyId(userId, studyId);

        return result;
    }

    // ============================================================
    // 통계 조회
    // ============================================================

    /**
     * 특정 스터디의 북마크 개수 조회
     */
    public Long getBookmarkCount(Long studyId) {
        Long count = bookmarkRepository.countByStudyId(studyId);

        return count;
    }

    /**
     * 내 북마크 개수 조회
     */
    public Long getMyBookmarkCount(Long userId) {
        Long count = bookmarkRepository.countByUserId(userId);

        return count;
    }

    // ============================================================
    // Private 헬퍼 메서드
    // ============================================================

    /**
     * 여러 스터디의 북마크 개수 Map 조회
     */
    private Map<Long, Long> getBookmarkCountMap(List<Long> studyIds) {
        if (studyIds.isEmpty()) {
            return Map.of();
        }

        return bookmarkRepository.countByStudyIds(studyIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
