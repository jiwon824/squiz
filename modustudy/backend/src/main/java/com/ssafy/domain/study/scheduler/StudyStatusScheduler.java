package com.ssafy.domain.study.scheduler;

import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyMember;
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
 * 스터디 모집 상태 자동 관리 스케줄러
 * 10분마다 실행되어 날짜/정원 기반으로 스터디 상태를 자동 전이
 */
 @Slf4j
 @Component
 @RequiredArgsConstructor
 public class StudyStatusScheduler {

    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;

    /**
     * 10분마다 실행 - 스터디 상태 자동 전이
     */
    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void updateStudyStatuses() {
        LocalDate today = LocalDate.now();

        // 1) SCHEDULED → RECRUITING (모집 시작일 도래)
        processScheduledToRecruiting(today);

        // 2) RECRUITING → RECRUIT_CLOSED (모집 종료일 경과)
        processRecruitingToRecruitClosed(today);

        // 3) RECRUITING → RECRUIT_CLOSED (정원 도달)
        processFullCapacityStudies();

        // 4) RECRUIT_CLOSED → IN_PROGRESS (스터디 시작일 도래)
        processRecruitClosedToInProgress(today);

        // 5) IN_PROGRESS → COMPLETED (스터디 종료일 경과)
        processInProgressToCompleted(today);
    }

    /**
     * SCHEDULED → RECRUITING: 모집 시작일이 오늘이거나 지난 스터디
     */
    private void processScheduledToRecruiting(LocalDate today) {
        List<Study> studies = studyRepository.findByStatusAndRecruitStartDateLessThanEqual(
                Status.SCHEDULED, today);

        for (Study study : studies) {
            try {
                study.updateStatus(Status.RECRUITING);
                notifyLeader(study, "스터디 모집이 시작되었습니다",
                        String.format("'%s' 스터디의 모집이 자동으로 시작되었습니다.", study.getName()));
            } catch (Exception e) {
}
        }
    }

    /**
     * RECRUITING → RECRUIT_CLOSED: 모집 종료일이 지난 스터디
     */
    private void processRecruitingToRecruitClosed(LocalDate today) {
        List<Study> studies = studyRepository.findByStatusAndRecruitEndDateLessThan(
                Status.RECRUITING, today);

        for (Study study : studies) {
            try {
                study.updateStatus(Status.RECRUIT_CLOSED);
                notifyLeader(study, "스터디 모집이 마감되었습니다",
                        String.format("'%s' 스터디의 모집 기간이 종료되어 자동 마감되었습니다.", study.getName()));
            } catch (Exception e) {
}
        }
    }

    /**
     * RECRUITING → RECRUIT_CLOSED: 승인 멤버 수가 정원에 도달한 스터디
     */
    private void processFullCapacityStudies() {
        List<Study> recruitingStudies = studyRepository.findByStatus(Status.RECRUITING);

        for (Study study : recruitingStudies) {
            try {
                int approvedCount = studyMemberRepository.countByStudyIdAndStatus(
                        study.getId(), MemberStatus.APPROVED);

                if (study.getMaxMembers() != null && approvedCount >= study.getMaxMembers()) {
                    study.updateStatus(Status.RECRUIT_CLOSED);
                    notifyLeader(study, "스터디 정원이 찼습니다",
                            String.format("'%s' 스터디의 정원(%d명)이 모두 차서 모집이 자동 마감되었습니다.",
                                    study.getName(), study.getMaxMembers()));
                }
            } catch (Exception e) {
}
        }
    }

    /**
     * RECRUIT_CLOSED → IN_PROGRESS: 스터디 시작일이 오늘이거나 지난 스터디
     */
    private void processRecruitClosedToInProgress(LocalDate today) {
        List<Study> studies = studyRepository.findByStatusAndStartDateLessThanEqual(
                Status.RECRUIT_CLOSED, today);

        for (Study study : studies) {
            try {
                study.updateStatus(Status.IN_PROGRESS);
                notifyAllMembers(study, "스터디가 시작되었습니다",
                        String.format("'%s' 스터디가 시작되었습니다. 첫 세션을 확인해주세요!", study.getName()));
            } catch (Exception e) {
}
        }
    }

    /**
     * IN_PROGRESS → COMPLETED: 스터디 종료일이 지난 스터디
     */
    private void processInProgressToCompleted(LocalDate today) {
        List<Study> studies = studyRepository.findByStatusAndEndDateLessThan(
                Status.IN_PROGRESS, today);

        for (Study study : studies) {
            try {
                study.updateStatus(Status.COMPLETED);
                notifyAllMembers(study, "스터디가 완료되었습니다",
                        String.format("'%s' 스터디가 완료되었습니다. 수고하셨습니다!", study.getName()));
            } catch (Exception e) {
}
        }
    }

    /**
     * 스터디장에게 알림 전송
     */
    private void notifyLeader(Study study, String title, String content) {
        try {
            notificationService.createNotification(
                    study.getLeaderId(),
                    NotificationType.STUDY_UPDATE,
                    title,
                    content,
                    "STUDY",
                    study.getId()
            );
        } catch (Exception e) {
}
    }

    /**
     * 스터디 전체 멤버에게 알림 전송
     */
    private void notifyAllMembers(Study study, String title, String content) {
        try {
            List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(
                    study.getId(), MemberStatus.APPROVED);

            for (StudyMember member : members) {
                try {
                    notificationService.createNotification(
                            member.getUserId(),
                            NotificationType.STUDY_UPDATE,
                            title,
                            content,
                            "STUDY",
                            study.getId()
                    );
                } catch (Exception e) {
}
            }

            // 리더에게도 알림
            notifyLeader(study, title, content);
        } catch (Exception e) {
}
    }
}

