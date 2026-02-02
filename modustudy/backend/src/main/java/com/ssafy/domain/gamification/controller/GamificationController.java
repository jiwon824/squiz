package com.ssafy.domain.gamification.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.domain.gamification.dto.response.*;
import com.ssafy.domain.gamification.service.GamificationService;
import com.ssafy.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    /**
     * 잔디 그래프 조회
     */
    @GetMapping("/contributions")
    public ApiResponse<ContributionResponse> getContributions(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        Long userId = userDetails.getUser().getId();
        ContributionResponse response = gamificationService.getContributions(userId, year, month);
        return ApiResponse.success(response);
    }

    /**
     * 특정 날짜 활동 상세
     */
    @GetMapping("/contributions/{date}")
    public ApiResponse<ContributionDetailResponse> getContributionDetail(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        Long userId = userDetails.getUser().getId();
        ContributionDetailResponse response = gamificationService.getContributionDetail(userId, date);
        return ApiResponse.success(response);
    }

    /**
     * 내 활동 통계
     */
    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getMyStats(
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        log.info("[Gamification] 통계 조회 요청 - userId: {}", userId);
        try {
            UserStatsResponse response = gamificationService.getMyStats(userId);
            log.info("[Gamification] 통계 조회 성공 - userId: {}", userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("[Gamification] 통계 조회 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 뱃지 목록
     */
    @GetMapping("/badges")
    public ApiResponse<BadgeListResponse> getBadges(
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        BadgeListResponse response = gamificationService.getBadges(userId);
        return ApiResponse.success(response);
    }

    /**
     * 패널티 목록
     */
    @GetMapping("/penalties")
    public ApiResponse<PenaltyListResponse> getPenalties(
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        PenaltyListResponse response = gamificationService.getPenalties(userId);
        return ApiResponse.success(response);
    }

    /**
     * 팀 내 랭킹
     */
    @GetMapping("/studies/{studyId}/ranking")
    public ApiResponse<StudyRankingResponse> getStudyRanking(
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @PathVariable Long studyId
    ) {
        Long userId = userDetails.getUser().getId();
        StudyRankingResponse response = gamificationService.getStudyRanking(userId, studyId);
        return ApiResponse.success(response);
    }
}
