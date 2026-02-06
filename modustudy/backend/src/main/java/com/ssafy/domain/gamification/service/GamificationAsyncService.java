package com.ssafy.domain.gamification.service;

import com.ssafy.domain.gamification.config.ExperienceConfig;
import com.ssafy.domain.gamification.entity.*;
import com.ssafy.domain.gamification.event.StudyCreateEvent;
import com.ssafy.domain.gamification.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 게이미피케이션 비동기 처리 서비스
 * - @Async 메서드를 별도 서비스로 분리하여 프록시 문제 해결
 */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 public class GamificationAsyncService {

    private final UserStatsRepository userStatsRepository;
    private final DailyContributionRepository dailyContributionRepository;
    private final ContributionDetailRepository contributionDetailRepository;
    private final UserRepository userRepository;

    /**
     * 스터디 생성 이벤트 비동기 처리
     */
    @Async
    @Transactional
    public void processStudyCreateAsync(StudyCreateEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 스터디 리더 카운트 증가 (통계용, 항상 증가)
        stats.incrementStudiesLed();

        // 첫 스터디 생성인 경우에만 경험치 지급
        if (event.isFirstStudy()) {
            // 1. 잔디 기록
            recordDailyContribution(user, event.getCreateDate());

            // 2. 활동 상세 기록
            recordContributionDetail(user, event.getCreateDate(),
                    ContributionDetail.ActivityType.STUDY_CREATE,
                    event.getStudyId(), event.getStudyName());

            // 3. 활동일 기록
            int streakBonus = stats.recordActivity(event.getCreateDate());

            // 4. 경험치 부여
            int totalExp = ExperienceConfig.FIRST_STUDY_CREATE_BONUS + streakBonus;
            boolean leveledUp = stats.addExperience(totalExp);

} else {
}

        userStatsRepository.save(stats);
    }

    // ========== Helper Methods ==========

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private UserStats getOrCreateUserStats(User user) {
        return userStatsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    UserStats newStats = UserStats.builder()
                            .user(user)
                            .build();
                    return userStatsRepository.save(newStats);
                });
    }

    private void recordDailyContribution(User user, LocalDate date) {
        Optional<DailyContribution> existingContribution =
                dailyContributionRepository.findByUserIdAndContributionDate(user.getId(), date);

        if (existingContribution.isPresent()) {
            DailyContribution contribution = existingContribution.get();
            contribution.incrementCount();
            dailyContributionRepository.save(contribution);
        } else {
            DailyContribution newContribution = DailyContribution.builder()
                    .user(user)
                    .contributionDate(date)
                    .activityCount(1)
                    .build();
            dailyContributionRepository.save(newContribution);
        }
    }

    private void recordContributionDetail(
            User user, LocalDate date,
            ContributionDetail.ActivityType activityType,
            Long referenceId, String referenceName
    ) {
        ContributionDetail detail = ContributionDetail.builder()
                .user(user)
                .contributionDate(date)
                .activityType(activityType)
                .referenceId(referenceId)
                .referenceName(referenceName)
                .build();
        contributionDetailRepository.save(detail);
    }
}

