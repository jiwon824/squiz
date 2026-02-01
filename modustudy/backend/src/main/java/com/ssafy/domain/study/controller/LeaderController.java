package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.LeaderReviewCreateRequest;
import com.ssafy.domain.study.dto.request.LeaderReviewUpdateRequest;
import com.ssafy.domain.study.dto.response.LeaderInfoResponse;
import com.ssafy.domain.study.dto.response.LeaderReviewResponse;
import com.ssafy.domain.study.service.LeaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/study/{studyId}/leader")
@RequiredArgsConstructor
@Slf4j
public class LeaderController {

    private final LeaderService leaderService;

    /**
     * 스터디장 정보 조회
     */
    @GetMapping
    public ResponseEntity<LeaderInfoResponse> getLeaderInfo(
            @PathVariable Long studyId) {

        log.info("API 호출 - 스터디장 정보 조회: studyId={}", studyId);

        LeaderInfoResponse response = leaderService.getLeaderInfo(studyId);

        log.info("API 응답 - 스터디장 정보: userId={}, name={}, rating={}",
                response.getUserId(), response.getName(), response.getLeaderRating());

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디장 리뷰 목록 조회
     */
    @GetMapping("/reviews")
    public ResponseEntity<Page<LeaderReviewResponse>> getLeaderReviews(
            @PathVariable Long studyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("API 호출 - 스터디장 리뷰 목록 조회: studyId={}, page={}, size={}",
                studyId, pageable.getPageNumber(), pageable.getPageSize());


        Page<LeaderReviewResponse> response = leaderService.getLeaderReviews(studyId, pageable);

        log.info("API 응답 - 리뷰 목록: totalElements={}, totalPages={}",
                response.getTotalElements(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디장 리뷰 작성
     */
    @PostMapping("/reviews")
    public ResponseEntity<LeaderReviewResponse> createReview(
            @PathVariable Long studyId,
            @RequestHeader("user-id") Long userId,
            @Valid @RequestBody LeaderReviewCreateRequest request) {

        log.info("API 호출 - 스터디장 리뷰 작성: studyId={}, userId={}", studyId, userId);

        LeaderReviewResponse response = leaderService.createReview(studyId, userId, request);

        log.info("API 응답 - 리뷰 작성 완료: reviewId={}", response.getReviewId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 리뷰 조회 (특정 스터디에서 내가 작성한 리뷰)
     */
    @GetMapping("/reviews/my")
    public ResponseEntity<LeaderReviewResponse> getMyReview(
            @PathVariable Long studyId,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 내 리뷰 조회: studyId={}, userId={}", studyId, userId);

        LeaderReviewResponse response = leaderService.getMyReview(studyId, userId);

        if (response == null) {
            log.info("API 응답 - 내 리뷰 없음: studyId={}, userId={}", studyId, userId);
            return ResponseEntity.noContent().build();
        }

        log.info("API 응답 - 내 리뷰 조회 완료: reviewId={}", response.getReviewId());
        return ResponseEntity.ok(response);
    }

    /**
     * 스터디장 리뷰 수정
     */
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<LeaderReviewResponse> updateReview(
            @PathVariable Long studyId,
            @PathVariable Long reviewId,
            @RequestHeader("user-id") Long userId,
            @Valid @RequestBody LeaderReviewUpdateRequest request) {

        log.info("API 호출 - 스터디장 리뷰 수정: studyId={}, reviewId={}, userId={}", studyId, reviewId, userId);

        LeaderReviewResponse response = leaderService.updateReview(studyId, reviewId, userId, request);

        log.info("API 응답 - 리뷰 수정 완료: reviewId={}", response.getReviewId());

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디장 리뷰 삭제
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long studyId,
            @PathVariable Long reviewId,
            @RequestHeader("user-id") Long userId) {

        log.info("API 호출 - 스터디장 리뷰 삭제: studyId={}, reviewId={}, userId={}", studyId, reviewId, userId);

        leaderService.deleteReview(studyId, reviewId, userId);

        log.info("API 응답 - 리뷰 삭제 완료: reviewId={}", reviewId);

        return ResponseEntity.noContent().build();
    }
}
