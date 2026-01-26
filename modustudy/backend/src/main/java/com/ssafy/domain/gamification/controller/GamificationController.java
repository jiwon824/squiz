package com.ssafy.domain.gamification.controller;

import com.ssafy.domain.gamification.dto.response.*;
import com.ssafy.domain.gamification.service.GamificationService;
import com.ssafy.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        ContributionResponse response = gamificationService.getContributions(userId, year, month);
        return ApiResponse.success(response);
    }

    /**
     * 특정 날짜 활동 상세
     */
    @GetMapping("/contributions/{date}")
    public ApiResponse<ContributionDetailResponse> getContributionDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        ContributionDetailResponse response = gamificationService.getContributionDetail(userId, date);
        return ApiResponse.success(response);
    }

    /**
     * 내 활동 통계
     */
    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getMyStats(
            @AuthenticationPrincipal Long userId
    ) {
        UserStatsResponse response = gamificationService.getMyStats(userId);
        return ApiResponse.success(response);
    }

    /**
     * 뱃지 목록
     */
    @GetMapping("/badges")
    public ApiResponse<BadgeListResponse> getBadges(
            @AuthenticationPrincipal Long userId
    ) {
        BadgeListResponse response = gamificationService.getBadges(userId);
        return ApiResponse.success(response);
    }

    /**
     * 패널티 목록
     */
    @GetMapping("/penalties")
    public ApiResponse<PenaltyListResponse> getPenalties(
            @AuthenticationPrincipal Long userId
    ) {
        PenaltyListResponse response = gamificationService.getPenalties(userId);
        return ApiResponse.success(response);
    }

    /**
     * 팀 내 랭킹
     */
    @GetMapping("/studies/{studyId}/ranking")
    public ApiResponse<StudyRankingResponse> getStudyRanking(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long studyId
    ) {
        StudyRankingResponse response = gamificationService.getStudyRanking(userId, studyId);
        return ApiResponse.success(response);
    }
}