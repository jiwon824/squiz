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

                LeaderInfoResponse response = leaderService.getLeaderInfo(studyId);

                return ResponseEntity.ok(response);
    }

    /**
     * 스터디장 리뷰 목록 조회
     */
    @GetMapping("/reviews")
    public ResponseEntity<Page<LeaderReviewResponse>> getLeaderReviews(
            @PathVariable Long studyId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<LeaderReviewResponse> response = leaderService.getLeaderReviews(studyId, pageable);

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

                LeaderReviewResponse response = leaderService.createReview(studyId, userId, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 리뷰 조회 (특정 스터디에서 내가 작성한 리뷰)
     */
    @GetMapping("/reviews/my")
    public ResponseEntity<LeaderReviewResponse> getMyReview(
            @PathVariable Long studyId,
            @RequestHeader("user-id") Long userId) {

                LeaderReviewResponse response = leaderService.getMyReview(studyId, userId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

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

                LeaderReviewResponse response = leaderService.updateReview(studyId, reviewId, userId, request);

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

                leaderService.deleteReview(studyId, reviewId, userId);

                return ResponseEntity.noContent().build();
    }
}

