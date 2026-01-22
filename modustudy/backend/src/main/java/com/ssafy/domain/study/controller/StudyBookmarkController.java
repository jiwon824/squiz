package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.StudyBookmarkResponse;
import com.ssafy.domain.study.service.StudyBookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class StudyBookmarkController {

    private final StudyBookmarkService bookmarkService;

    // ============================================================
    // 북마크 토글 (추가/삭제)
    // ============================================================

    /**
     * 북마크 토글 (있으면 삭제, 없으면 추가)
     */
    @PostMapping("/study/{studyId}/bookmark")
    public ResponseEntity<StudyBookmarkResponse> toggleBookmark(
            @PathVariable Long studyId,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 북마크 토글: studyId={}, userId={}", studyId, userId);

        StudyBookmarkResponse response = bookmarkService.toggleBookmark(studyId, userId);

        log.info("API 응답 - 북마크 토글 완료: studyId={}, isBookmarked={}",
                studyId, response.getIsBookmarked());

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 북마크 조회
    // ============================================================

    /**
     * 내 북마크 목록 조회
     */
    @GetMapping("/my/bookmarks")
    public ResponseEntity<Page<StudyBookmarkResponse>> getMyBookmarks(
            @RequestHeader("user-id") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("API 호출 - 내 북마크 목록 조회: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        Page<StudyBookmarkResponse> response = bookmarkService.getMyBookmarks(userId, pageable);

        log.info("API 응답 - 내 북마크 목록: userId={}, totalElements={}",
                userId, response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 스터디 북마크 여부 확인
     */
    @GetMapping("/study/{studyId}/bookmark/check")
    public ResponseEntity<Boolean> isBookmarked(
            @PathVariable Long studyId,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 북마크 여부 확인: studyId={}, userId={}", studyId, userId);

        boolean result = bookmarkService.isBookmarked(studyId, userId);

        log.info("API 응답 - 북마크 여부: studyId={}, isBookmarked={}", studyId, result);

        return ResponseEntity.ok(result);
    }

    // ============================================================
    // 통계 조회
    // ============================================================

    /**
     * 특정 스터디의 북마크 개수 조회
     */
    @GetMapping("/study/{studyId}/bookmark/count")
    public ResponseEntity<Long> getBookmarkCount(
            @PathVariable Long studyId) {

        log.info("API 호출 - 스터디 북마크 개수 조회: studyId={}", studyId);

        Long count = bookmarkService.getBookmarkCount(studyId);

        log.info("API 응답 - 북마크 개수: studyId={}, count={}", studyId, count);

        return ResponseEntity.ok(count);
    }

    /**
     * 내 북마크 개수 조회
     * GET /api/v1/my/bookmarks/count
     */
    @GetMapping("/my/bookmarks/count")
    public ResponseEntity<Long> getMyBookmarkCount(
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 내 북마크 개수 조회: userId={}", userId);

        Long count = bookmarkService.getMyBookmarkCount(userId);

        log.info("API 응답 - 내 북마크 개수: userId={}, count={}", userId, count);

        return ResponseEntity.ok(count);
    }
}
