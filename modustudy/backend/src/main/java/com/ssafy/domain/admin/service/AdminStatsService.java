package com.ssafy.domain.admin.service;

import com.ssafy.domain.admin.dto.*;
import com.ssafy.domain.admin.mapper.AdminStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final AdminStatsMapper adminStatsMapper;

    /**
     * 대시보드 요약 정보
     */
    public DashboardSummaryDto getDashboardSummary() {
        return DashboardSummaryDto.builder()
                .totalUsers(adminStatsMapper.countTotalUsers())
                .activeStudies(adminStatsMapper.countActiveStudies())
                .todaySignups(adminStatsMapper.countTodaySignups())
                .pendingReports(adminStatsMapper.countPendingReports())
                .build();
    }

    /**
     * 회원 가입 추이 (기본 30일)
     */
    public List<UserSignupStatsDto> getUserSignupStats(int days) {
        return adminStatsMapper.getUserSignupStats(days);
    }

    /**
     * 스터디 상태별 통계
     */
    public List<StudyStatusStatsDto> getStudyStatusStats() {
        return adminStatsMapper.getStudyStatusStats();
    }

    /**
     * 로그인 방식별 통계
     */
    public List<LoginMethodStatsDto> getLoginMethodStats() {
        return adminStatsMapper.getLoginMethodStats();
    }

    /**
     * 퀴즈 통계
     */
    public QuizStatsDto getQuizStats(int days) {
        List<QuizStatsDto.DailyQuizAttemptDto> dailyAttempts = adminStatsMapper.getDailyQuizAttempts(days);
        List<QuizStatsDto.CourseParticipationDto> courseParticipation = adminStatsMapper.getCourseParticipation();

        return QuizStatsDto.builder()
                .dailyAttempts(dailyAttempts)
                .courseParticipation(courseParticipation)
                .build();
    }

    /**
     * 최근 가입 회원
     */
    public List<RecentUserDto> getRecentUsers(int limit) {
        return adminStatsMapper.getRecentUsers(limit);
    }

    /**
     * 인기 스터디
     */
    public List<PopularStudyDto> getPopularStudies(int limit) {
        return adminStatsMapper.getPopularStudies(limit);
    }

    // ========== 새로운 시계열 통계 ==========

    /**
     * 일별 미팅 통계
     */
    public List<DailyMeetingStatsDto> getDailyMeetingStats(int days) {
        return adminStatsMapper.getDailyMeetingStats(days);
    }

    /**
     * 일별 출석 통계
     */
    public List<DailyAttendanceStatsDto> getDailyAttendanceStats(int days) {
        return adminStatsMapper.getDailyAttendanceStats(days);
    }

    /**
     * 일별 활동 통계 (잔디)
     */
    public List<DailyActivityStatsDto> getDailyActivityStats(int days) {
        return adminStatsMapper.getDailyActivityStats(days);
    }

    /**
     * 레벨별 사용자 분포
     */
    public List<UserLevelStatsDto> getUserLevelStats() {
        return adminStatsMapper.getUserLevelStats();
    }

    /**
     * 토픽별 스터디 분포
     */
    public List<StudyTopicStatsDto> getStudyTopicStats() {
        return adminStatsMapper.getStudyTopicStats();
    }
}
