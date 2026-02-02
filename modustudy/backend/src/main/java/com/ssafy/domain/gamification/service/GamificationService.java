package com.ssafy.domain.gamification.service;

import com.ssafy.domain.gamification.config.ExperienceConfig;
import com.ssafy.domain.gamification.entity.BadgeCategory;
import com.ssafy.domain.gamification.dto.response.*;
import com.ssafy.domain.gamification.entity.*;
import com.ssafy.domain.gamification.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import java.util.Objects;
import java.util.Collections;
import com.ssafy.domain.study.entity.StudyMember;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamificationService {

    private final UserStatsRepository userStatsRepository;
    private final DailyContributionRepository dailyContributionRepository;
    private final ContributionDetailRepository contributionDetailRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;

    /**
     * 내 활동 통계 조회
     */
    @Transactional
    public UserStatsResponse getMyStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserStats stats = userStatsRepository.findByUser_Id(userId)
                .orElseGet(() -> createAndSaveDefaultUserStats(user));

        // 다음 레벨 정보
        Integer nextLevel = Math.min(stats.getLevel() + 1, ExperienceConfig.MAX_LEVEL);
        String nextLevelName = ExperienceConfig.getLevelName(nextLevel);

        // 현재 레벨 경험치 진행도 (경험치 기반)
        Integer currentExp = stats.getCurrentExperience();
        Integer requiredExp = stats.getRequiredExpForNextLevel();
        Double percentage = stats.getLevelProgressPercentage();

        return UserStatsResponse.builder()
                .level(stats.getLevel())
                .levelName(stats.getLevelName())
                .levelProgress(UserStatsResponse.LevelProgress.builder()
                        .current(currentExp)
                        .required(requiredExp)
                        .percentage(percentage)
                        .build())
                .nextLevel(UserStatsResponse.NextLevel.builder()
                        .level(nextLevel)
                        .name(nextLevelName)
                        .build())
                .totalActivityDays(stats.getTotalActivityDays())
                .currentStreak(stats.getCurrentStreak())
                .maxStreak(stats.getMaxStreak())
                .lastActivityDate(stats.getLastActivityDate())
                .totalStudiesJoined(stats.getTotalStudiesJoined())
                .totalStudiesLed(stats.getTotalStudiesLed())
                .totalAttendance(stats.getTotalAttendance())
                .totalChatCount(stats.getTotalChatCount())
                .totalQuizCount(stats.getTotalQuizCount())
                .totalMaterialsUploaded(stats.getTotalMaterialsUploaded())
                .totalRetrospectives(stats.getTotalRetrospectives())
                .joinedAt(user.getCreatedAt())
                .build();
    }

    /**
     * 기본 UserStats 생성 및 저장
     */
    private UserStats createAndSaveDefaultUserStats(User user) {
        UserStats stats = UserStats.builder()
                .user(user)
                .build();
        return userStatsRepository.save(stats);
    }
    /**
     * 잔디 그래프 조회 (월간 또는 연간)
     */
    public ContributionResponse getContributions(Long userId, int year, Integer month) {
        if (month != null) {
            return getMonthlyContributions(userId, year, month);
        } else {
            return getYearlyContributions(userId, year);
        }
    }

    /**
     * 월간 잔디 그래프
     */
    private ContributionResponse getMonthlyContributions(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DailyContribution> contributions = dailyContributionRepository
                .findByUserIdAndContributionDateBetweenOrderByContributionDateAsc(userId, startDate, endDate);

        // 날짜별 활동 맵 생성
        Map<LocalDate, Boolean> activityMap = contributions.stream()
                .collect(Collectors.toMap(
                        DailyContribution::getContributionDate,
                        DailyContribution::getHasActivity
                ));

        // 전체 날짜 생성
        List<ContributionResponse.ContributionDay> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            days.add(ContributionResponse.ContributionDay.builder()
                    .date(date)
                    .hasActivity(activityMap.getOrDefault(date, false))
                    .build());
        }

        // 통계 계산
        UserStats stats = userStatsRepository.findByUser_Id(userId).orElse(null);
        int activeDays = (int) activityMap.values().stream().filter(v -> v).count();

        return ContributionResponse.builder()
                .year(year)
                .month(month)
                .contributions(days)
                .summary(ContributionResponse.ContributionSummary.builder()
                        .totalDays(yearMonth.lengthOfMonth())
                        .activeDays(activeDays)
                        .currentStreak(stats != null ? stats.getCurrentStreak() : 0)
                        .maxStreak(stats != null ? stats.getMaxStreak() : 0)
                        .build())
                .build();
    }

    /**
     * 연간 잔디 그래프
     */
    private ContributionResponse getYearlyContributions(Long userId, int year) {
        List<DailyContribution> contributions = dailyContributionRepository
                .findByUserIdAndYear(userId, year);

        Map<LocalDate, Boolean> activityMap = contributions.stream()
                .collect(Collectors.toMap(
                        DailyContribution::getContributionDate,
                        DailyContribution::getHasActivity
                ));

        // 전체 날짜 생성 (1년치)
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        List<ContributionResponse.ContributionDay> days = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            days.add(ContributionResponse.ContributionDay.builder()
                    .date(date)
                    .hasActivity(activityMap.getOrDefault(date, false))
                    .build());
        }

        // 월별 통계
        Map<Integer, Long> monthlyActivityCount = contributions.stream()
                .filter(DailyContribution::getHasActivity)
                .collect(Collectors.groupingBy(
                        dc -> dc.getContributionDate().getMonthValue(),
                        Collectors.counting()
                ));

        List<ContributionResponse.MonthlyStats> monthlyStats = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            monthlyStats.add(ContributionResponse.MonthlyStats.builder()
                    .month(m)
                    .activeDays(monthlyActivityCount.getOrDefault(m, 0L).intValue())
                    .build());
        }

        UserStats stats = userStatsRepository.findByUser_Id(userId).orElse(null);
        int activeDays = (int) activityMap.values().stream().filter(v -> v).count();

        return ContributionResponse.builder()
                .year(year)
                .contributions(days)
                .summary(ContributionResponse.ContributionSummary.builder()
                        .totalDays(365)
                        .activeDays(activeDays)
                        .currentStreak(stats != null ? stats.getCurrentStreak() : 0)
                        .maxStreak(stats != null ? stats.getMaxStreak() : 0)
                        .build())
                .monthlyStats(monthlyStats)
                .build();
    }
    /**
     * 뱃지 목록 조회 (카테고리별)
     */
    public BadgeListResponse getBadges(Long userId) {
        // 모든 뱃지 조회
        List<Badge> allBadges = badgeRepository.findAllByOrderByCategoryAscSortOrderAsc();

        // 사용자가 획득한 뱃지 조회
        List<UserBadge> userBadges = userBadgeRepository.findByUserIdWithBadge(userId);
        Map<Long, UserBadge> earnedBadgeMap = userBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub));

        // 사용자 통계 (진행도 계산용)
        UserStats stats = userStatsRepository.findByUser_Id(userId).orElse(null);

        // 카테고리별 그룹핑
        Map<BadgeCategory, List<Badge>> badgesByCategory = allBadges.stream()
                .collect(Collectors.groupingBy(Badge::getCategory));

        List<BadgeListResponse.BadgeCategory> categories = badgesByCategory.entrySet().stream()
                .map(entry -> {
                    BadgeCategory category = entry.getKey();
                    List<Badge> badges = entry.getValue();

                    List<BadgeListResponse.BadgeInfo> badgeInfos = badges.stream()
                            .map(badge -> {
                                UserBadge userBadge = earnedBadgeMap.get(badge.getId());
                                boolean isEarned = userBadge != null;

                                // 진행도 계산 (미획득 뱃지만)
                                BadgeListResponse.BadgeProgress progress = null;
                                if (!isEarned && stats != null) {
                                    progress = calculateBadgeProgress(badge, stats);
                                }

                                return BadgeListResponse.BadgeInfo.builder()
                                        .id(badge.getId())
                                        .code(badge.getCode())
                                        .name(badge.getName())
                                        .description(badge.getDescription())
                                        .icon(badge.getIcon())
                                        .isEarned(isEarned)
                                        .earnedAt(isEarned ? userBadge.getEarnedAt() : null)
                                        .progress(progress)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return BadgeListResponse.BadgeCategory.builder()
                            .category(category.name())
                            .categoryName(getCategoryName(category))
                            .badges(badgeInfos)
                            .build();
                })
                .collect(Collectors.toList());

        return BadgeListResponse.builder()
                .categories(categories)
                .totalBadges(allBadges.size())
                .earnedCount(userBadges.size())
                .build();
    }

    /**
     * 뱃지 진행도 계산
     */
    private BadgeListResponse.BadgeProgress calculateBadgeProgress(Badge badge, UserStats stats) {
        Integer current = getCurrentProgressForBadge(badge, stats);
        Integer required = badge.getConditionValue();
        Double percentage = required > 0 ? (current * 100.0 / required) : 0.0;

        return BadgeListResponse.BadgeProgress.builder()
                .current(current)
                .required(required)
                .percentage(Math.min(percentage, 100.0))
                .build();
    }

    /**
     * 뱃지별 현재 진행 값 가져오기
     */
    private Integer getCurrentProgressForBadge(Badge badge, UserStats stats) {
        String conditionType = badge.getConditionType();

        return switch (conditionType) {
            case "TOTAL_ACTIVITY_DAYS" -> stats.getTotalActivityDays();
            case "CURRENT_STREAK" -> stats.getCurrentStreak();
            case "MAX_STREAK" -> stats.getMaxStreak();
            case "TOTAL_STUDIES" -> stats.getTotalStudiesJoined();
            case "TOTAL_QUIZ" -> stats.getTotalQuizCount();
            default -> 0;
        };
    }

    /**
     * 카테고리 한글명
     */
    private String getCategoryName(BadgeCategory category) {
        return switch (category) {
            case ACTIVITY -> "활동";
            case STREAK -> "스트릭";
            case STUDY -> "스터디";
            case ATTENDANCE -> "출석";
            case PARTICIPATION -> "참여";
            case QUIZ -> "퀴즈";
            case MASTER -> "마스터";
            case SPECIAL -> "특별";
        };
    }
    /**
     * 패널티 목록 조회
     */
    public PenaltyListResponse getPenalties(Long userId) {
        // 활성 패널티 조회
        List<UserPenalty> activePenalties = userPenaltyRepository
                .findByUserIdAndIsActiveTrueWithDetails(userId);

        // 해소된 패널티 조회
        List<UserPenalty> removedPenalties = userPenaltyRepository
                .findByUserIdAndIsActiveFalseWithDetails(userId);

        // DTO 변환
        List<PenaltyListResponse.PenaltyInfo> activePenaltyInfos = activePenalties.stream()
                .map(up -> PenaltyListResponse.PenaltyInfo.builder()
                        .id(up.getId())
                        .code(up.getPenalty().getCode())
                        .name(up.getPenalty().getName())
                        .description(up.getPenalty().getDescription())
                        .icon(up.getPenalty().getIcon())
                        .grantCondition(up.getPenalty().getGrantCondition())
                        .removalCondition(up.getPenalty().getRemovalCondition())
                        .removalProgress(up.getRemovalProgress())
                        .removalRequired(up.getPenalty().getRemovalRequired())
                        .studyId(up.getStudy() != null ? up.getStudy().getId() : null)
                        .studyName(up.getStudy() != null ? up.getStudy().getName() : null)
                        .grantedAt(up.getGrantedAt())
                        .build())
                .toList();

        List<PenaltyListResponse.RemovedPenalty> removedPenaltyInfos = removedPenalties.stream()
                .map(up -> PenaltyListResponse.RemovedPenalty.builder()
                        .id(up.getId())
                        .code(up.getPenalty().getCode())
                        .name(up.getPenalty().getName())
                        .description(up.getPenalty().getDescription())
                        .icon(up.getPenalty().getIcon())
                        .grantedAt(up.getGrantedAt())
                        .removedAt(up.getRemovedAt())
                        .build())
                .toList();

        return PenaltyListResponse.builder()
                .totalActive(activePenalties.size())
                .totalRemoved(removedPenalties.size())
                .activePenalties(activePenaltyInfos)
                .removedPenalties(removedPenaltyInfos)
                .build();
    }
    /**
     * 스터디 내 랭킹 조회
     */
    public StudyRankingResponse getStudyRanking(Long userId, Long studyId) {
        // 1. 스터디의 활성 멤버 조회
        List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(
                studyId,
                MemberStatus.APPROVED
        );

        if (members.isEmpty()) {
            return StudyRankingResponse.builder()
                    .rankings(Collections.emptyList())
                    .myRank(null)
                    .totalMembers(0)
                    .build();
        }

        // 2. 각 멤버의 통계 정보 조회 및 랭킹 생성
        List<StudyRankingResponse.RankingInfo> rankings = members.stream()
                .map(member -> {
                    // 사용자 정보 조회
                    User user = userRepository.findById(member.getUserId())
                            .orElse(null);
                    if (user == null) {
                        return null;
                    }

                    // 통계 정보 조회
                    UserStats stats = userStatsRepository.findByUser_Id(member.getUserId())
                            .orElse(null);

                    // 출석률 계산 (임시로 활동일 기반)
                    int activityDays = stats != null ? stats.getTotalActivityDays() : 0;
                    double attendanceRate = activityDays > 0 ? (activityDays * 10.0) : 0.0;
                    attendanceRate = Math.min(attendanceRate, 100.0);

                    return StudyRankingResponse.RankingInfo.builder()
                            .rank(0) // 정렬 후 설정
                            .user(StudyRankingResponse.UserInfo.builder()
                                    .id(user.getId())
                                    .nickname(user.getNickname())
                                    .profileImage(user.getProfileImage())
                                    .level(stats != null ? stats.getLevel() : 1)
                                    .levelName(stats != null ? stats.getLevelName() : "새싹")
                                    .build())
                            .totalExperience(stats != null ? stats.getTotalExperience() : 0)
                            .activityDays(activityDays)
                            .attendanceRate(attendanceRate)
                            .isMe(user.getId().equals(userId))
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    // 1차: 경험치로 정렬
                    int expCompare = b.getTotalExperience().compareTo(a.getTotalExperience());
                    if (expCompare != 0) {
                        return expCompare;
                    }
                    // 2차: 활동일수로 정렬
                    int activityCompare = b.getActivityDays().compareTo(a.getActivityDays());
                    if (activityCompare != 0) {
                        return activityCompare;
                    }
                    // 3차: 출석률로 정렬
                    return b.getAttendanceRate().compareTo(a.getAttendanceRate());
                })
                .collect(Collectors.toList());

        // 3. 순위 설정
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        // 4. 내 순위 찾기
        Integer myRank = rankings.stream()
                .filter(StudyRankingResponse.RankingInfo::getIsMe)
                .findFirst()
                .map(StudyRankingResponse.RankingInfo::getRank)
                .orElse(null);

        return StudyRankingResponse.builder()
                .rankings(rankings)
                .myRank(myRank)
                .totalMembers(rankings.size())
                .build();
    }
    /**
     * 특정 날짜 활동 상세 조회
     */
    public ContributionDetailResponse getContributionDetail(Long userId, LocalDate date) {
        List<ContributionDetail> details = contributionDetailRepository
                .findByUserIdAndContributionDateOrderByCreatedAtDesc(userId, date);

        boolean hasActivity = !details.isEmpty();

        List<ContributionDetailResponse.Activity> activities = details.stream()
                .map(detail -> ContributionDetailResponse.Activity.builder()
                        .type(detail.getActivityType().name())
                        .referenceId(detail.getReferenceId())
                        .referenceName(detail.getReferenceName())
                        .createdAt(detail.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ContributionDetailResponse.builder()
                .date(date)
                .hasActivity(hasActivity)
                .activities(activities)
                .build();
    }
}