package com.ssafy.domain.meeting.service;

import com.ssafy.domain.meeting.entity.StudyDailyUsage;
import com.ssafy.domain.meeting.repository.StudyDailyUsageRepository;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 스터디 일일 사용량 관리 서비스
 * - 온라인 미팅: 3시간 한도
 * - 오프라인 STT: 2시간 한도
 */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 public class StudyDailyUsageService {

    private final StudyDailyUsageRepository usageRepository;
    private final StudyRepository studyRepository;

    /**
     * 오늘의 사용량 조회 (없으면 생성)
     */
    @Transactional
    public StudyDailyUsage getOrCreateTodayUsage(Long studyId) {
        LocalDate today = LocalDate.now();

        return usageRepository.findByStudyIdAndUsageDate(studyId, today)
                .orElseGet(() -> {
                    Study study = studyRepository.findById(studyId)
                            .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다: " + studyId));

                    StudyDailyUsage newUsage = StudyDailyUsage.builder()
                            .study(study)
                            .usageDate(today)
                            .onlineMeetingSeconds(0)
                            .onlineMeetingCount(0)
                            .offlineSttSeconds(0)
                            .offlineSttCount(0)
                            .build();

                    return usageRepository.save(newUsage);
                });
    }

    /**
     * 온라인 미팅 시작 가능 여부 확인
     * @return 남은 시간(초), 0이면 시작 불가
     */
    @Transactional(readOnly = true)
    public int getOnlineMeetingRemainingSeconds(Long studyId) {
        return usageRepository.findByStudyIdAndUsageDate(studyId, LocalDate.now())
                .map(StudyDailyUsage::getOnlineMeetingRemainingSeconds)
                .orElse(StudyDailyUsage.ONLINE_MEETING_DAILY_LIMIT_SECONDS);
    }

    /**
     * 온라인 미팅 시작 가능 여부
     */
    @Transactional(readOnly = true)
    public boolean canStartOnlineMeeting(Long studyId) {
        return getOnlineMeetingRemainingSeconds(studyId) > 0;
    }

    /**
     * 오프라인 STT 업로드 가능 여부 확인
     * @param durationSeconds 업로드하려는 오디오 길이 (초)
     * @return true면 업로드 가능
     */
    @Transactional(readOnly = true)
    public boolean canUploadOfflineStt(Long studyId, int durationSeconds) {
        int remaining = usageRepository.findByStudyIdAndUsageDate(studyId, LocalDate.now())
                .map(StudyDailyUsage::getOfflineSttRemainingSeconds)
                .orElse(StudyDailyUsage.OFFLINE_STT_DAILY_LIMIT_SECONDS);

        return durationSeconds <= remaining;
    }

    /**
     * 오프라인 STT 남은 시간 조회
     */
    @Transactional(readOnly = true)
    public int getOfflineSttRemainingSeconds(Long studyId) {
        return usageRepository.findByStudyIdAndUsageDate(studyId, LocalDate.now())
                .map(StudyDailyUsage::getOfflineSttRemainingSeconds)
                .orElse(StudyDailyUsage.OFFLINE_STT_DAILY_LIMIT_SECONDS);
    }

    /**
     * 온라인 미팅 시간 사용량 추가
     */
    @Transactional
    public void addOnlineMeetingUsage(Long studyId, int durationSeconds) {
        StudyDailyUsage usage = getOrCreateTodayUsage(studyId);
        usage.addOnlineMeetingTime(durationSeconds);
        usageRepository.save(usage);

}

    /**
     * 오프라인 STT 시간 사용량 추가
     */
    @Transactional
    public void addOfflineSttUsage(Long studyId, int durationSeconds) {
        StudyDailyUsage usage = getOrCreateTodayUsage(studyId);
        usage.addOfflineSttTime(durationSeconds);
        usageRepository.save(usage);

}

    /**
     * 사용량 현황 조회 (DTO 반환용)
     */
    @Transactional(readOnly = true)
    public UsageStatus getUsageStatus(Long studyId) {
        StudyDailyUsage usage = usageRepository.findByStudyIdAndUsageDate(studyId, LocalDate.now())
                .orElse(null);

        if (usage == null) {
            return new UsageStatus(
                    0, StudyDailyUsage.ONLINE_MEETING_DAILY_LIMIT_SECONDS,
                    0, StudyDailyUsage.OFFLINE_STT_DAILY_LIMIT_SECONDS
            );
        }

        return new UsageStatus(
                usage.getOnlineMeetingSeconds(),
                usage.getOnlineMeetingRemainingSeconds(),
                usage.getOfflineSttSeconds(),
                usage.getOfflineSttRemainingSeconds()
        );
    }

    /**
     * 사용량 현황 DTO
     */
    public record UsageStatus(
            int onlineMeetingUsedSeconds,
            int onlineMeetingRemainingSeconds,
            int offlineSttUsedSeconds,
            int offlineSttRemainingSeconds
    ) {
        public boolean canStartOnlineMeeting() {
            return onlineMeetingRemainingSeconds > 0;
        }

        public boolean canUploadOfflineStt(int durationSeconds) {
            return durationSeconds <= offlineSttRemainingSeconds;
        }
    }
}

