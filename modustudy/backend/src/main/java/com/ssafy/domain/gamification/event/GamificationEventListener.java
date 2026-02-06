package com.ssafy.domain.gamification.event;

import com.ssafy.domain.gamification.config.ExperienceConfig;
import com.ssafy.domain.gamification.entity.*;
import com.ssafy.domain.gamification.repository.*;
import com.ssafy.domain.gamification.service.GamificationAsyncService;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {
    private static final int REFERENCE_NAME_MAX_LENGTH = 200;

    private final UserStatsRepository userStatsRepository;
    private final DailyContributionRepository dailyContributionRepository;
    private final ContributionDetailRepository contributionDetailRepository;
    private final UserRepository userRepository;
    private final GamificationAsyncService gamificationAsyncService;

    /**
     * 스터디 출석 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleStudyAttendance(StudyAttendanceEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getAttendanceDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getAttendanceDate(),
                ContributionDetail.ActivityType.STUDY_ATTENDANCE,
                event.getStudyId(), event.getStudyName());

        // 3. 출석 카운트 증가
        stats.incrementAttendance();

        // 4. 활동일 기록 및 연속 출석 보너스 확인
        int streakBonus = stats.recordActivity(event.getAttendanceDate());

        // 5. 경험치 부여 (기본 + 연속 출석 보너스)
        int totalExp = ExperienceConfig.STUDY_ATTENDANCE + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

}

    /**
     * 퀴즈 풀이 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleQuizSolved(QuizSolvedEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getSolvedDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getSolvedDate(),
                ContributionDetail.ActivityType.QUIZ_SOLVED,
                event.getQuizId(), event.getQuizTitle());

        // 3. 퀴즈 카운트 증가
        stats.incrementQuizCount();

        // 4. 활동일 기록
        int streakBonus = stats.recordActivity(event.getSolvedDate());

        // 5. 경험치 부여 (정답/오답에 따라 다름)
        int baseExp = event.isCorrect() ? ExperienceConfig.QUIZ_CORRECT : ExperienceConfig.QUIZ_WRONG;
        int totalExp = baseExp + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

}

    /**
     * 스터디 가입 이벤트 처리 (첫 가입만 경험치 지급)
     */
    @EventListener
    @Transactional
    public void handleStudyJoin(StudyJoinEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 스터디 참여 카운트 증가 (통계용, 항상 증가)
        stats.incrementStudiesJoined();

        // 첫 스터디 가입인 경우에만 경험치 지급
        if (event.isFirstStudy()) {
            // 1. 잔디 기록
            recordDailyContribution(user, event.getJoinDate());

            // 2. 활동 상세 기록
            recordContributionDetail(user, event.getJoinDate(),
                    ContributionDetail.ActivityType.STUDY_JOIN,
                    event.getStudyId(), event.getStudyName());

            // 3. 활동일 기록
            int streakBonus = stats.recordActivity(event.getJoinDate());

            // 4. 경험치 부여
            int totalExp = ExperienceConfig.FIRST_STUDY_JOIN_BONUS + streakBonus;
            boolean leveledUp = stats.addExperience(totalExp);

} else {
}

        userStatsRepository.save(stats);
    }

    /**
     * 스터디 생성 이벤트 처리 (첫 생성만 경험치 지급)
     * - AFTER_COMMIT: 스터디 생성 트랜잭션 커밋 후 실행
     * - GamificationAsyncService로 비동기 처리 위임
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudyCreate(StudyCreateEvent event) {
        gamificationAsyncService.processStudyCreateAsync(event);
    }

    /**
     * 자료 업로드 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleMaterialUpload(MaterialUploadEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getUploadDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getUploadDate(),
                ContributionDetail.ActivityType.MATERIAL_UPLOAD,
                event.getMaterialId(), event.getMaterialName());

        // 3. 자료 업로드 카운트 증가
        stats.incrementMaterialsUploaded();

        // 4. 활동일 기록
        int streakBonus = stats.recordActivity(event.getUploadDate());

        // 5. 경험치 부여
        int totalExp = ExperienceConfig.MATERIAL_UPLOAD + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

}

    /**
     * 회고록 작성 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleRetrospectiveWrite(RetrospectiveWriteEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getWriteDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getWriteDate(),
                ContributionDetail.ActivityType.RETROSPECTIVE,
                event.getRetrospectiveId(), "회고록");

        // 3. 회고록 카운트 증가
        stats.incrementRetrospectives();

        // 4. 활동일 기록
        int streakBonus = stats.recordActivity(event.getWriteDate());

        // 5. 경험치 부여
        int totalExp = ExperienceConfig.RETROSPECTIVE_WRITE + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

}

    // 스터디 채팅 경험치 비활성화 - 친구 DM 첫 채팅만 경험치 지급
    // /**
    //  * 채팅 메시지 이벤트 처리 (일일 제한 적용)
    //  */

    /**
     * 친구와 첫 채팅 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleFirstFriendChat(FirstFriendChatEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 친구 이름 조회
        String friendName = userRepository.findById(event.getFriendId())
                .map(friend -> friend.getNickname() != null ? friend.getNickname() : friend.getName())
                .orElse("친구");

        // 1. 잔디 기록
        recordDailyContribution(user, event.getChatDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getChatDate(),
                ContributionDetail.ActivityType.FIRST_FRIEND_CHAT,
                event.getFriendId(), friendName + "님과 첫 대화");

        // 3. 활동일 기록
        int streakBonus = stats.recordActivity(event.getChatDate());

        // 4. 경험치 부여
        int totalExp = ExperienceConfig.FIRST_FRIEND_CHAT_BONUS + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

}

    /**
     * 첫 스터디장 리뷰 작성 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleFirstLeaderReview(FirstLeaderReviewEvent event) {
        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getReviewDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getReviewDate(),
                ContributionDetail.ActivityType.LEADER_REVIEW,
                event.getStudyId(), event.getStudyName() + " 스터디장 리뷰");

        // 3. 활동일 기록
        int streakBonus = stats.recordActivity(event.getReviewDate());

        // 4. 경험치 부여
        int totalExp = ExperienceConfig.FIRST_LEADER_REVIEW_BONUS + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

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
        String normalizedReferenceName = normalizeReferenceName(referenceName);
        ContributionDetail detail = ContributionDetail.builder()
                .user(user)
                .contributionDate(date)
                .activityType(activityType)
                .referenceId(referenceId)
                .referenceName(normalizedReferenceName)
                .build();
        contributionDetailRepository.save(detail);
    }

    private String normalizeReferenceName(String referenceName) {
        if (referenceName == null) {
            return "";
        }
        String trimmed = referenceName.trim();
        if (trimmed.length() <= REFERENCE_NAME_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, REFERENCE_NAME_MAX_LENGTH);
    }
}

