package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Async;

/**
 * 미팅 AI 처리 스케줄러
 * - 종료된 미팅의 음성 파일을 감지하여 AI 처리 시작
 * - AI 처리 결과를 폴링하여 저장
 */
@Service
@RequiredArgsConstructor
public class MeetingAiScheduler {

    private static final Logger log = LoggerFactory.getLogger(MeetingAiScheduler.class);

    private final MeetingRepository meetingRepository;
    private final MeetingAudioRecordingRepository meetingAudioRecordingRepository;
    private final LocalFileStorageService localFileStorageService;
    private final MeetingAiProcessingService meetingAiProcessingService;
    private final MeetingSttService meetingSttService;
    private final SpeechSegmentService speechSegmentService;

    // 진행 중인 AI 작업 추적 (meetingId -> jobId)
    private final Map<Long, String> processingJobs = new ConcurrentHashMap<>();

    // AI 작업 시작 시간 추적 (meetingId -> startTimeMillis)
    private final Map<Long, Long> jobStartTimes = new ConcurrentHashMap<>();

    // AI 응답 타임아웃 (10분)
    private static final long AI_PROCESSING_TIMEOUT_MS = 10 * 60 * 1000;

    /**
     * 미팅 종료 직후 단발성으로 AI 처리를 시도.
     * (녹음 파일이 아직 준비되지 않았다면 스킵되고, 스케줄러가 재시도함)
     */
    @Async
    public void triggerProcessing(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) {
            return;
        }
        if (meeting.getStatus() != MeetingStatus.ENDED) {
            return;
        }
        if (meeting.getSummaryStatus() != SummaryStatus.PROCESSING) {
            return;
        }
        if (processingJobs.containsKey(meetingId)) {
            return;
        }
        try {
            processMeetingIfReady(meeting);
        } catch (Exception e) {
            log.error("AI 처리 트리거 실패 - meetingId: {}, error: {}", meetingId, e.getMessage());
        }
    }

    /**
     * 30초마다 실행 - AI 처리가 필요한 미팅 찾아서 처리 시작
     */
    @Scheduled(fixedDelay = 30000)
    public void processEndedMeetings() {
        log.info("[AI 스케줄러] 폴링 시작 - 현재 진행중인 작업: {}", processingJobs.size());

        // 효율적인 쿼리로 AI 처리 대기 중인 미팅만 조회
        List<Meeting> allProcessingMeetings = meetingRepository
                .findByStatusAndSummaryStatus(MeetingStatus.ENDED, SummaryStatus.PROCESSING);

        log.info("[AI 스케줄러] PROCESSING 상태 미팅 수: {}", allProcessingMeetings.size());

        List<Meeting> meetings = allProcessingMeetings.stream()
                .filter(m -> !processingJobs.containsKey(m.getId()))
                .toList();

        log.info("[AI 스케줄러] 처리 대상 미팅 수: {} (이미 진행중 제외)", meetings.size());

        for (Meeting meeting : meetings) {
            try {
                log.info("[AI 스케줄러] 미팅 처리 시도 - meetingId: {}, studyId: {}", meeting.getId(), meeting.getStudyId());
                processMeetingIfReady(meeting);
            } catch (Exception e) {
                log.error("미팅 AI 처리 시작 실패 - meetingId: {}, error: {}", meeting.getId(), e.getMessage());
            }
        }
    }

    /**
     * 15초마다 실행 - 진행 중인 AI 작업 결과 확인
     */
    @Scheduled(fixedDelay = 15000)
    public void checkProcessingJobs() {
        if (processingJobs.isEmpty()) {
            log.debug("[AI 스케줄러] 진행 중인 작업 없음");
            return;
        }

        log.info("[AI 스케줄러] 작업 결과 확인 시작 - 진행중인 작업: {}", processingJobs);
        long now = System.currentTimeMillis();

        for (Map.Entry<Long, String> entry : processingJobs.entrySet()) {
            Long meetingId = entry.getKey();
            String jobId = entry.getValue();

            // 타임아웃 체크 (10분 초과 시 중단)
            Long startTime = jobStartTimes.get(meetingId);
            if (startTime != null && (now - startTime) > AI_PROCESSING_TIMEOUT_MS) {
                log.warn("AI 처리 타임아웃 (10분 초과) - meetingId: {}, jobId: {}", meetingId, jobId);
                processingJobs.remove(meetingId);
                jobStartTimes.remove(meetingId);
                try {
                    Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
                    if (meeting != null) {
                        meeting.updateSummaryStatus(SummaryStatus.PENDING);
                        meetingRepository.save(meeting);
                    }
                } catch (Exception e) {
                    log.error("타임아웃 후 상태 업데이트 실패 - meetingId: {}", meetingId, e);
                }
                continue;
            }

            try {
                Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
                if (meeting == null) {
                    processingJobs.remove(meetingId);
                    jobStartTimes.remove(meetingId);
                    continue;
                }

                String status = meetingAiProcessingService.checkAndSaveAiResult(
                        meeting.getStudyId(), meetingId, jobId);
                syncGeneratedTextFiles(meetingId);

                if ("completed".equals(status) || "failed".equals(status)) {
                    processingJobs.remove(meetingId);
                    jobStartTimes.remove(meetingId);
                    log.info("AI 처리 완료 - meetingId: {}, status: {}", meetingId, status);
                }
            } catch (Exception e) {
                log.error("AI 결과 확인 실패 - meetingId: {}, jobId: {}, error: {}", meetingId, jobId, e.getMessage());
            }
        }
    }

    /**
     * 미팅 AI 처리 준비 확인 및 시작
     * 1. 실시간 STT segments가 있으면 → 바로 AI 처리 시작 (오디오 불필요)
     * 2. segments가 없으면 → voice.webm 파일 확인 후 처리
     */
    @Transactional
    public void processMeetingIfReady(Meeting meeting) {
        Long meetingId = meeting.getId();
        Long studyId = meeting.getStudyId();
        log.info("[AI 처리 준비] meetingId: {}, studyId: {} - 처리 조건 확인 시작", meetingId, studyId);

        // 1. 실시간 STT segments 확인 (우선 처리)
        long segmentCount = speechSegmentService.countByMeetingId(meetingId);
        log.info("[AI 처리 준비] meetingId: {} - speech segment 수: {}", meetingId, segmentCount);

        if (segmentCount > 0) {
            log.info("[AI 처리 준비] meetingId: {} - 실시간 STT 세그먼트 발견, AI 처리 시작", meetingId);
            startAiProcessingJob(studyId, meetingId, meeting);
            return;
        }

        // 2. 실시간 STT 없으면 오디오 파일 확인
        log.info("[AI 처리 준비] meetingId: {} - speech segment 없음, 오디오 파일 확인", meetingId);
        List<MeetingAudioRecording> mixedRecordings = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);

        if (mixedRecordings.isEmpty()) {
            // DB에 없으면 디스크에서 voice.webm 파일 확인
            Path voiceFile = localFileStorageService.resolveMeetingVoiceDir(meetingId).resolve("voice.webm");
            log.info("[AI 처리 준비] meetingId: {} - DB에 오디오 없음, 파일 확인: {}", meetingId, voiceFile);

            if (!Files.exists(voiceFile)) {
                log.info("[AI 처리 준비] meetingId: {} - 음성 파일도 없음. 처리 스킵", meetingId);
                return;
            }

            // 파일이 있으면 DB에 저장
            try {
                Long fileSize = Files.size(voiceFile);
                String recordingUrl = buildMixedVoiceUrl(meetingId);

                MeetingAudioRecording recording = MeetingAudioRecording.builder()
                        .meetingId(meetingId)
                        .userId(null)
                        .trackType(MeetingAudioTrackType.MIXED)
                        .recordingUrl(recordingUrl)
                        .format("webm")
                        .fileSize(fileSize)
                        .build();

                meetingAudioRecordingRepository.save(recording);
                log.info("MIXED 오디오 녹음 저장 - meetingId: {}, fileSize: {}", meetingId, fileSize);

            } catch (IOException e) {
                log.error("음성 파일 크기 확인 실패 - meetingId: {}", meetingId, e);
                return;
            }
        }

        // AI 처리 시작
        startAiProcessingJob(studyId, meetingId, meeting);
    }

    /**
     * AI 처리 작업 시작 (공통 로직)
     */
    private void startAiProcessingJob(Long studyId, Long meetingId, Meeting meeting) {
        try {
            String jobId = meetingAiProcessingService.startAiProcessing(studyId, meetingId);
            processingJobs.put(meetingId, jobId);
            jobStartTimes.put(meetingId, System.currentTimeMillis());
            log.info("AI 처리 시작 - meetingId: {}, jobId: {}", meetingId, jobId);
        } catch (Exception e) {
            log.error("AI 처리 시작 실패 - meetingId: {}, error: {}", meetingId, e.getMessage());
            // 오디오 없음 등의 예외 시 PENDING으로 롤백
            if (e.getMessage() != null && e.getMessage().contains("NO_MIXED_AUDIO")) {
                meeting.updateSummaryStatus(SummaryStatus.PENDING);
            }
        }
    }

    /**
     * 60초마다 실행 - 종료된 미팅의 생성된 파일을 DB와 동기화
     * (AI 처리 상태와 무관하게 파일 존재 시 DB 업서트)
     */
    @Scheduled(fixedDelay = 60000)
    public void syncEndedMeetingsTextFiles() {
        List<Meeting> meetings = meetingRepository.findTop200ByStatusOrderByEndedAtDesc(MeetingStatus.ENDED);
        if (meetings.isEmpty()) {
            return;
        }

        for (Meeting meeting : meetings) {
            syncGeneratedTextFiles(meeting.getId());
        }
    }

    private String buildMixedVoiceUrl(Long meetingId) {
        return "/uploads/meetings/" + meetingId + "/recordings/voice/voice.webm";
    }

    private void syncGeneratedTextFiles(Long meetingId) {
        try {
            Path sttPath = localFileStorageService.getBasePath()
                    .resolve("meetings")
                    .resolve(String.valueOf(meetingId))
                    .resolve("stt")
                    .resolve("mixed")
                    .resolve("stt.txt")
                    .normalize();
            if (Files.exists(sttPath)) {
                meetingSttService.upsertSttFileInternal(meetingId, null);
            }

            Path summaryPath = localFileStorageService.getBasePath()
                    .resolve("meetings")
                    .resolve(String.valueOf(meetingId))
                    .resolve("stt")
                    .resolve("mixed")
                    .resolve("summary.txt")
                    .normalize();
            if (Files.exists(summaryPath)) {
                meetingSttService.upsertSummaryFileInternal(meetingId, null);
            }
        } catch (Exception e) {
            log.error("생성된 텍스트 파일 동기화 실패 - meetingId: {}, error: {}", meetingId, e.getMessage());
        }
    }
}
