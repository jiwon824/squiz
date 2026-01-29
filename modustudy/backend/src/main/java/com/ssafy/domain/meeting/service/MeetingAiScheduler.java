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

    // 진행 중인 AI 작업 추적 (meetingId -> jobId)
    private final Map<Long, String> processingJobs = new ConcurrentHashMap<>();

    /**
     * 30초마다 실행 - AI 처리가 필요한 미팅 찾아서 처리 시작
     */
    @Scheduled(fixedDelay = 30000)
    public void processEndedMeetings() {
        // 효율적인 쿼리로 AI 처리 대기 중인 미팅만 조회
        List<Meeting> meetings = meetingRepository
                .findByStatusAndSummaryStatus(MeetingStatus.ENDED, SummaryStatus.PROCESSING)
                .stream()
                .filter(m -> !processingJobs.containsKey(m.getId()))
                .toList();

        for (Meeting meeting : meetings) {
            try {
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
            return;
        }

        for (Map.Entry<Long, String> entry : processingJobs.entrySet()) {
            Long meetingId = entry.getKey();
            String jobId = entry.getValue();

            try {
                Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
                if (meeting == null) {
                    processingJobs.remove(meetingId);
                    continue;
                }

                String status = meetingAiProcessingService.checkAndSaveAiResult(
                        meeting.getStudyId(), meetingId, jobId);

                if ("completed".equals(status) || "failed".equals(status)) {
                    processingJobs.remove(meetingId);
                    log.info("AI 처리 완료 - meetingId: {}, status: {}", meetingId, status);
                }
            } catch (Exception e) {
                log.error("AI 결과 확인 실패 - meetingId: {}, jobId: {}, error: {}", meetingId, jobId, e.getMessage());
                // 실패 시 재시도를 위해 job 유지 (또는 일정 횟수 후 제거)
            }
        }
    }

    /**
     * 미팅 음성 파일이 준비되었는지 확인하고 AI 처리 시작
     */
    @Transactional
    public void processMeetingIfReady(Meeting meeting) {
        Long meetingId = meeting.getId();
        Long studyId = meeting.getStudyId();

        // MIXED 오디오가 이미 DB에 있는지 확인
        List<MeetingAudioRecording> mixedRecordings = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);

        if (mixedRecordings.isEmpty()) {
            // DB에 없으면 디스크에서 voice.webm 파일 확인
            Path voiceFile = localFileStorageService.resolveMeetingVoiceDir(meetingId).resolve("voice.webm");

            if (!Files.exists(voiceFile)) {
                log.debug("음성 파일 아직 없음 - meetingId: {}", meetingId);
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
        try {
            String jobId = meetingAiProcessingService.startAiProcessing(studyId, meetingId);
            processingJobs.put(meetingId, jobId);
            log.info("AI 처리 시작 - meetingId: {}, jobId: {}", meetingId, jobId);
        } catch (Exception e) {
            log.error("AI 처리 시작 실패 - meetingId: {}, error: {}", meetingId, e.getMessage());
            // 오디오 없음 등의 예외 시 PENDING으로 롤백
            if (e.getMessage() != null && e.getMessage().contains("NO_MIXED_AUDIO")) {
                meeting.updateSummaryStatus(SummaryStatus.PENDING);
            }
        }
    }

    private String buildMixedVoiceUrl(Long meetingId) {
        return "/uploads/meetings/" + meetingId + "/recordings/voice/voice.webm";
    }
}
