package com.ssafy.domain.gamification.event;

import com.ssafy.domain.gamification.config.ExperienceConfig;
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
        log.info("[Gamification] 스터디 출석: userId={}, studyId={}", event.getUserId(), event.getStudyId());

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

        log.info("[Gamification] 스터디 출석 완료: +{}XP (기본 {}XP + 연속보너스 {}XP), 레벨업={}",
                totalExp, ExperienceConfig.STUDY_ATTENDANCE, streakBonus, leveledUp);
    }

    /**
     * 퀴즈 풀이 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleQuizSolved(QuizSolvedEvent event) {
        log.info("[Gamification] 퀴즈 풀이: userId={}, quizId={}, correct={}",
                event.getUserId(), event.getQuizId(), event.isCorrect());

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

        log.info("[Gamification] 퀴즈 풀이 완료: +{}XP, 레벨업={}", totalExp, leveledUp);
    }

    /**
     * 스터디 가입 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleStudyJoin(StudyJoinEvent event) {
        log.info("[Gamification] 스터디 가입: userId={}, studyId={}, isFirst={}",
                event.getUserId(), event.getStudyId(), event.isFirstStudy());

        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getJoinDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getJoinDate(),
                ContributionDetail.ActivityType.STUDY_JOIN,
                event.getStudyId(), event.getStudyName());

        // 3. 스터디 참여 카운트 증가
        stats.incrementStudiesJoined();

        // 4. 활동일 기록
        int streakBonus = stats.recordActivity(event.getJoinDate());

        // 5. 경험치 부여 (첫 스터디면 보너스 추가)
        int baseExp = ExperienceConfig.STUDY_JOIN;
        int firstBonus = event.isFirstStudy() ? ExperienceConfig.FIRST_STUDY_BONUS : 0;
        int totalExp = baseExp + firstBonus + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

        log.info("[Gamification] 스터디 가입 완료: +{}XP (기본 {}XP + 첫가입 {}XP + 연속 {}XP), 레벨업={}",
                totalExp, baseExp, firstBonus, streakBonus, leveledUp);
    }

    /**
     * 스터디 생성 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleStudyCreate(StudyCreateEvent event) {
        log.info("[Gamification] 스터디 생성: userId={}, studyId={}", event.getUserId(), event.getStudyId());

        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 1. 잔디 기록
        recordDailyContribution(user, event.getCreateDate());

        // 2. 활동 상세 기록
        recordContributionDetail(user, event.getCreateDate(),
                ContributionDetail.ActivityType.STUDY_CREATE,
                event.getStudyId(), event.getStudyName());

        // 3. 스터디 리더 카운트 증가
        stats.incrementStudiesLed();

        // 4. 활동일 기록
        int streakBonus = stats.recordActivity(event.getCreateDate());

        // 5. 경험치 부여
        int totalExp = ExperienceConfig.STUDY_CREATE + streakBonus;
        boolean leveledUp = stats.addExperience(totalExp);

        userStatsRepository.save(stats);

        log.info("[Gamification] 스터디 생성 완료: +{}XP, 레벨업={}", totalExp, leveledUp);
    }

    /**
     * 자료 업로드 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleMaterialUpload(MaterialUploadEvent event) {
        log.info("[Gamification] 자료 업로드: userId={}, materialId={}", event.getUserId(), event.getMaterialId());

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

        log.info("[Gamification] 자료 업로드 완료: +{}XP, 레벨업={}", totalExp, leveledUp);
    }

    /**
     * 회고록 작성 이벤트 처리
     */
    @EventListener
    @Transactional
    public void handleRetrospectiveWrite(RetrospectiveWriteEvent event) {
        log.info("[Gamification] 회고록 작성: userId={}, retrospectiveId={}", event.getUserId(), event.getRetrospectiveId());

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

        log.info("[Gamification] 회고록 작성 완료: +{}XP, 레벨업={}", totalExp, leveledUp);
    }

    /**
     * 채팅 메시지 이벤트 처리 (일일 제한 적용)
     */
    @EventListener
    @Transactional
    public void handleChatMessage(ChatMessageEvent event) {
        log.debug("[Gamification] 채팅 메시지: userId={}, studyId={}", event.getUserId(), event.getStudyId());

        User user = findUser(event.getUserId());
        UserStats stats = getOrCreateUserStats(user);

        // 채팅 경험치 추가 (일일 제한 적용)
        int expGain = stats.addChatExperience(event.getChatDate());

        if (expGain > 0) {
            // 경험치 획득 시에만 잔디 기록
            recordDailyContribution(user, event.getChatDate());

            // 활동일 기록
            stats.recordActivity(event.getChatDate());

            // 경험치 부여
            stats.addExperience(expGain);

            userStatsRepository.save(stats);

            log.debug("[Gamification] 채팅 경험치: +{}XP", expGain);
        }
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
