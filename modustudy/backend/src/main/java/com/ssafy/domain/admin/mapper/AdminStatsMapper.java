package com.ssafy.domain.admin.mapper;

import com.ssafy.domain.admin.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminStatsMapper {

    /**
     * 총 회원 수
     */
    int countTotalUsers();

    /**
     * 활성 스터디 수 (RECRUITING, IN_PROGRESS)
     */
    int countActiveStudies();

    /**
     * 오늘 가입자 수
     */
    int countTodaySignups();

    /**
     * 미처리 신고 수
     */
    int countPendingReports();

    /**
     * 일별 회원 가입 추이
     */
    List<UserSignupStatsDto> getUserSignupStats(@Param("days") int days);

    /**
     * 스터디 상태별 통계
     */
    List<StudyStatusStatsDto> getStudyStatusStats();

    /**
     * 로그인 방식별 통계
     */
    List<LoginMethodStatsDto> getLoginMethodStats();

    /**
     * 일별 퀴즈 시도 통계
     */
    List<QuizStatsDto.DailyQuizAttemptDto> getDailyQuizAttempts(@Param("days") int days);

    /**
     * 코스별 참여자 통계
     */
    List<QuizStatsDto.CourseParticipationDto> getCourseParticipation();

    /**
     * 최근 가입 회원
     */
    List<RecentUserDto> getRecentUsers(@Param("limit") int limit);

    /**
     * 인기 스터디 TOP N
     */
    List<PopularStudyDto> getPopularStudies(@Param("limit") int limit);

    // ========== 새로운 시계열 통계 ==========

    /**
     * 일별 미팅 통계
     */
    List<DailyMeetingStatsDto> getDailyMeetingStats(@Param("days") int days);

    /**
     * 일별 출석 통계
     */
    List<DailyAttendanceStatsDto> getDailyAttendanceStats(@Param("days") int days);

    /**
     * 일별 활동 통계 (잔디)
     */
    List<DailyActivityStatsDto> getDailyActivityStats(@Param("days") int days);

    /**
     * 레벨별 사용자 분포
     */
    List<UserLevelStatsDto> getUserLevelStats();

    /**
     * 토픽별 스터디 분포
     */
    List<StudyTopicStatsDto> getStudyTopicStats();
}
