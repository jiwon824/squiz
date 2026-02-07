package com.ssafy.domain.study.scheduler;

import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 모집 기간 종료 스케줄러
 * - 매일 자정(00:00)에 실행
 * - 모집 기간이 종료된 스터디 상태 변경:
 *   - 인원 충족 시: RECRUITING → RECRUIT_CLOSED
 *   - 인원 미충족 시: RECRUITING → PENDING (확정대기)
 */
 @Component
 @RequiredArgsConstructor
 @Slf4j
 public class RecruitmentDeadlineScheduler {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;

    /**
     * 매일 자정에 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkRecruitmentDeadlines() {
        LocalDate today = LocalDate.now();

        // 모집중인 스터디 중 모집 종료일이 오늘 이전인 스터디 조회
        List<Study> expiredStudies = studyRepository.findByStatusAndRecruitEndDateBefore(
                Status.RECRUITING, today);

                for (Study study : expiredStudies) {
            processExpiredStudy(study);
        }

}

    /**
     * 모집 기간이 종료된 스터디 처리
     */
    private void processExpiredStudy(Study study) {
        try {
            int currentMembers = studyMemberRepository.countByStudyIdAndStatus(
                    study.getId(), MemberStatus.APPROVED);
            Integer maxMembers = study.getMaxMembers();

// 최소 인원 기준 (스터디장 포함 2명 이상이면 인원 충족으로 판단)
            // 또는 maxMembers에 도달했으면 충족
            boolean isRecruitmentComplete = (maxMembers != null && currentMembers >= maxMembers)
                    || currentMembers >= 2;

            if (isRecruitmentComplete) {
                // 인원 충족 → 모집완료/시작대기
                study.updateStatus(Status.RECRUIT_CLOSED);

                // 스터디장에게 알림
                notificationService.createNotification(
                        study.getLeaderId(),
                        NotificationType.STUDY_RECRUITMENT_COMPLETE,
                        "모집 기간이 종료되었습니다",
                        String.format("'%s' 스터디의 모집 기간이 종료되었습니다. 스터디를 시작해주세요!", study.getName()),
                        "STUDY",
                        study.getId()
                );

} else {
                // 인원 미충족 → 확정대기
                study.updateStatus(Status.PENDING);

                // 스터디장에게 알림
                notificationService.createNotification(
                        study.getLeaderId(),
                        NotificationType.STUDY_UPDATE,
                        "모집 인원이 부족합니다",
                        String.format("'%s' 스터디의 모집 기간이 종료되었지만 인원이 부족합니다. 모집을 연장하거나 현재 인원으로 시작할 수 있습니다.",
                                study.getName()),
                        "STUDY",
                        study.getId()
                );

}
        } catch (Exception e) {
}
    }
}

