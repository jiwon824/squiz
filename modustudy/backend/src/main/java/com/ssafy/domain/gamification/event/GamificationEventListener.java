package com.ssafy.domain.gamification.event;

import com.ssafy.domain.gamification.entity.*;
import com.ssafy.domain.gamification.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {

    private final UserStatsRepository userStatsRepository;
    private final DailyContributionRepository dailyContributionRepository;
    private final ContributionDetailRepository contributionDetailRepository;
    private final UserRepository userRepository;

    /**
     * 스터디 출석 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleStudyAttendance(StudyAttendanceEvent event) {
        log.info("스터디 출석 이벤트 처리: userId={}, studyId={}", event.getUserId(), event.getStudyId());

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 잔디 기록 (DailyContribution)
        recordDailyContribution(user, event.getAttendanceDate());

        // 2. 활동 상세 기록 (ContributionDetail)
        recordContributionDetail(
                user,
                event.getAttendanceDate(),
                ContributionDetail.ActivityType.STUDY_ATTENDANCE,
                event.getStudyId(),
                event.getStudyName()
        );

        // 3. 통계 업데이트 (경험치 +10)
        updateUserStats(user, 10);

        log.info("스터디 출석 이벤트 처리 완료");
    }

    /**
     * 퀴즈 풀이 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleQuizSolved(QuizSolvedEvent event) {
        log.info("퀴즈 풀이 이벤트 처리: userId={}, quizId={}, isCorrect={}",
                event.getUserId(), event.getQuizId(), event.isCorrect());

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 잔디 기록
        recordDailyContribution(user, event.getSolvedDate());

        // 2. 활동 상세 기록
        recordContributionDetail(
                user,
                event.getSolvedDate(),
                ContributionDetail.ActivityType.QUIZ_SOLVED,
                event.getQuizId(),
                event.getQuizTitle()
        );

        // 3. 통계 업데이트 (정답: +5, 오답: +2)
        int exp = event.isCorrect() ? 5 : 2;
        updateUserStats(user, exp);

        log.info("퀴즈 풀이 이벤트 처리 완료");
    }

    /**
     * 일일 기여도 기록
     */
    private void recordDailyContribution(User user, LocalDate date) {
        Optional<DailyContribution> existingContribution =
                dailyContributionRepository.findByUserIdAndContributionDate(user.getId(), date);

        if (existingContribution.isPresent()) {
            // 이미 오늘 활동이 있으면 count만 증가
            DailyContribution contribution = existingContribution.get();
            contribution.incrementCount();
            dailyContributionRepository.save(contribution);
        } else {
            // 새로운 날짜의 첫 활동
            DailyContribution newContribution = DailyContribution.builder()
                    .user(user)
                    .contributionDate(date)
                    .activityCount(1)
                    .build();
            dailyContributionRepository.save(newContribution);
        }
    }

    /**
     * 활동 상세 기록
     */
    private void recordContributionDetail(
            User user,
            LocalDate date,
            ContributionDetail.ActivityType activityType,
            Long referenceId,
            String referenceName
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

    /**
     * 사용자 통계 업데이트
     */
    private void updateUserStats(User user, int expGain) {
        UserStats stats = userStatsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // UserStats가 없으면 새로 생성
                    UserStats newStats = UserStats.builder()
                            .user(user)
                            .build();
                    return userStatsRepository.save(newStats);
                });

        stats.addExperience(expGain);
        userStatsRepository.save(stats);
    }
}