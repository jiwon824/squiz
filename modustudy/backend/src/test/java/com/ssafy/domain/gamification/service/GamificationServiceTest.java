package com.ssafy.domain.gamification.service;

import com.ssafy.domain.gamification.dto.response.*;
import com.ssafy.domain.gamification.entity.*;
import com.ssafy.domain.gamification.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ssafy.domain.user.entity.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private DailyContributionRepository dailyContributionRepository;

    @Mock
    private ContributionDetailRepository contributionDetailRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private PenaltyRepository penaltyRepository;

    @Mock
    private UserPenaltyRepository userPenaltyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GamificationService gamificationService;

    private User testUser;
    private UserStats testUserStats;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("test@test.com")
                .password("password123")
                .nickname("테스터")
                .email("test@test.com")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();

        testUserStats = UserStats.builder()
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("내 활동 통계 조회 - 성공")
    void getMyStats_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userStatsRepository.findByUserId(1L)).willReturn(Optional.of(testUserStats));

        // when
        UserStatsResponse response = gamificationService.getMyStats(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(response.getLevelName()).isEqualTo("새싹");
    }

    @Test
    @DisplayName("내 활동 통계 조회 - UserStats가 없으면 기본값 반환")
    void getMyStats_NoUserStats_ReturnsDefault() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userStatsRepository.findByUserId(1L)).willReturn(Optional.empty());

        // when
        UserStatsResponse response = gamificationService.getMyStats(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(response.getLevelName()).isEqualTo("새싹");
    }

    @Test
    @DisplayName("특정 날짜 활동 상세 조회 - 활동 있음")
    void getContributionDetail_WithActivity() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 15);

        List<ContributionDetail> details = List.of(
                ContributionDetail.builder()
                        .user(testUser)
                        .contributionDate(date)
                        .activityType(ContributionDetail.ActivityType.STUDY_ATTENDANCE)
                        .referenceId(1L)
                        .referenceName("알고리즘 스터디")
                        .build()
        );

        given(contributionDetailRepository.findByUserIdAndContributionDateOrderByCreatedAtDesc(1L, date))
                .willReturn(details);

        // when
        ContributionDetailResponse response = gamificationService.getContributionDetail(1L, date);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDate()).isEqualTo(date);
        assertThat(response.getHasActivity()).isTrue();
        assertThat(response.getActivities()).hasSize(1);
        assertThat(response.getActivities().get(0).getReferenceName()).isEqualTo("알고리즘 스터디");
    }

    @Test
    @DisplayName("특정 날짜 활동 상세 조회 - 활동 없음")
    void getContributionDetail_NoActivity() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 15);
        given(contributionDetailRepository.findByUserIdAndContributionDateOrderByCreatedAtDesc(1L, date))
                .willReturn(new ArrayList<>());

        // when
        ContributionDetailResponse response = gamificationService.getContributionDetail(1L, date);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDate()).isEqualTo(date);
        assertThat(response.getHasActivity()).isFalse();
        assertThat(response.getActivities()).isEmpty();
    }

    @Test
    @DisplayName("뱃지 목록 조회 - 성공")
    void getBadges_Success() {
        // given
        Badge badge1 = Badge.builder()
                .id(1L)
                .code("FIRST_ACTIVITY")
                .name("첫 발걸음")
                .description("첫 활동 기록")
                .icon("👣")
                .category(BadgeCategory.ACTIVITY)
                .conditionType("TOTAL_ACTIVITY_DAYS")
                .conditionValue(1)
                .build();

        given(badgeRepository.findAllByOrderByCategoryAscSortOrderAsc())
                .willReturn(List.of(badge1));
        given(userBadgeRepository.findByUserIdWithBadge(1L))
                .willReturn(new ArrayList<>());
        given(userStatsRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserStats));

        // when
        BadgeListResponse response = gamificationService.getBadges(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalBadges()).isEqualTo(1);
        assertThat(response.getEarnedCount()).isEqualTo(0);
        assertThat(response.getCategories()).isNotEmpty();
    }

    @Test
    @DisplayName("패널티 목록 조회 - 성공")
    void getPenalties_Success() {
        // given
        given(userPenaltyRepository.findByUserIdAndIsActiveWithDetails(1L, true))
                .willReturn(new ArrayList<>());
        given(userPenaltyRepository.findByUserIdAndIsActiveWithDetails(1L, false))
                .willReturn(new ArrayList<>());

        // when
        PenaltyListResponse response = gamificationService.getPenalties(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalActive()).isEqualTo(0);
        assertThat(response.getTotalRemoved()).isEqualTo(0);
        assertThat(response.getActivePenalties()).isEmpty();
        assertThat(response.getRemovedPenalties()).isEmpty();
    }
}