package com.ssafy.domain.gamification.service;

import com.ssafy.domain.gamification.entity.BadgeCategory;
import com.ssafy.domain.gamification.dto.response.*;
import com.ssafy.domain.gamification.entity.*;
import com.ssafy.domain.gamification.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 레벨 설정 (임시로 하드코딩, 나중에 LevelConfig 테이블에서 가져오기)
    private static final Map<Integer, String> LEVEL_NAMES = Map.of(
            1, "새싹",
            2, "학습자",
            3, "열공러",
            4, "성실러",
            5, "마스터",
            6, "그랜드마스터"
    );

    private static final Map<Integer, Integer> LEVEL_REQUIREMENTS = Map.of(
            1, 0,
            2, 7,
            3, 15,
            4, 30,
            5, 60,
            6, 100
    );

    /**
     * 내 활동 통계 조회
     */
    public UserStatsResponse getMyStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultUserStats(user));

        // 다음 레벨 정보
        Integer nextLevel = stats.getLevel() + 1;
        String nextLevelName = LEVEL_NAMES.getOrDefault(nextLevel, "최고 레벨");
        Integer nextLevelRequired = LEVEL_REQUIREMENTS.getOrDefault(nextLevel, 999);

        // 현재 레벨 진행도
        Integer currentRequired = LEVEL_REQUIREMENTS.getOrDefault(stats.getLevel(), 0);
        Integer current = stats.getTotalActivityDays() - currentRequired;
        Integer required = nextLevelRequired - currentRequired;
        Double percentage = required > 0 ? (current * 100.0 / required) : 100.0;

        return UserStatsResponse.builder()
                .level(stats.getLevel())
                .levelName(stats.getLevelName())
                .levelProgress(UserStatsResponse.LevelProgress.builder()
                        .current(current)
                        .required(required)
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
                .totalAttendance(stats.getTotalActivityDays()) // 임시
                .totalChatCount(stats.getTotalChatCount())
                .totalQuizCount(stats.getTotalQuizCount())
                .totalMaterialsUploaded(stats.getTotalMaterialsUploaded())
                .totalRetrospectives(stats.getTotalRetrospectives())
                .joinedAt(user.getCreatedAt())
                .build();
    }

    private UserStats createDefaultUserStats(User user) {
        return UserStats.builder()
                .user(user)
                .build();
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
        UserStats stats = userStatsRepository.findByUserId(userId).orElse(null);
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

        UserStats stats = userStatsRepository.findByUserId(userId).orElse(null);
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
        UserStats stats = userStatsRepository.findByUserId(userId).orElse(null);

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
        // 활성 패널티
        List<UserPenalty> activePenalties = userPenaltyRepository
                .findByUserIdAndIsActiveWithDetails(userId, true);

        // 해소된 패널티
        List<UserPenalty> removedPenalties = userPenaltyRepository
                .findByUserIdAndIsActiveWithDetails(userId, false);

        List<PenaltyListResponse.PenaltyInfo> activePenaltyInfos = activePenalties.stream()
                .map(PenaltyListResponse.PenaltyInfo::from)
                .collect(Collectors.toList());

        List<PenaltyListResponse.RemovedPenalty> removedPenaltyInfos = removedPenalties.stream()
                .map(PenaltyListResponse.RemovedPenalty::from)
                .collect(Collectors.toList());

        return PenaltyListResponse.builder()
                .activePenalties(activePenaltyInfos)
                .removedPenalties(removedPenaltyInfos)
                .totalActive(activePenaltyInfos.size())
                .totalRemoved(removedPenaltyInfos.size())
                .build();
    }
    /**
     * 스터디 내 랭킹 조회
     */
    public StudyRankingResponse getStudyRanking(Long userId, Long studyId) {
        // TODO: StudyMember 엔티티로 스터디 멤버 목록 조회
        // 일단 임시로 빈 리스트 반환

        List<StudyRankingResponse.RankingInfo> rankings = new ArrayList<>();
        // 실제 구현 예시 (StudyMember가 있다고 가정)
        /*
        List<StudyMember> members = studyMemberRepository.findByStudyId(studyId);

        List<StudyRankingResponse.RankingInfo> rankings = members.stream()
            .map(member -> {
                UserStats stats = userStatsRepository.findByUserId(member.getUser().getId())
                    .orElse(null);

                // 출석률 계산 (임시로 100%)
                double attendanceRate = 100.0;

                return StudyRankingResponse.RankingInfo.builder()
                    .rank(0) // 정렬 후 설정
                    .user(StudyRankingResponse.UserInfo.builder()
                        .id(member.getUser().getId())
                        .nickname(member.getUser().getNickname())
                        .profileImage(member.getUser().getProfileImage())
                        .level(stats != null ? stats.getLevel() : 1)
                        .levelName(stats != null ? stats.getLevelName() : "새싹")
                        .build())
                    .activityDays(stats != null ? stats.getTotalActivityDays() : 0)
                    .attendanceRate(attendanceRate)
                    .isMe(member.getUser().getId().equals(userId))
                    .build();
            })
            .sorted((a, b) -> {
                // 활동일 수로 정렬
                int compare = b.getActivityDays().compareTo(a.getActivityDays());
                if (compare == 0) {
                    // 출석률로 정렬
                    return b.getAttendanceRate().compareTo(a.getAttendanceRate());
                }
                return compare;
            })
            .collect(Collectors.toList());

        // 순위 설정
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).rank = i + 1;
        }
        */
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