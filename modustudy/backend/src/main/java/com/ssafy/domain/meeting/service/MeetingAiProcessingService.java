package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.ai.service.AiService;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingAiProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAiProcessingService.class);

    private final MeetingAudioRecordingRepository meetingAudioRecordingRepository;
    private final LocalFileStorageService localFileStorageService;
    private final AiService aiService;
    private final MeetingServiceHelper helper;
    private final MeetingSttService meetingSttService;

    @Transactional
    public String startAiProcessing(Long studyId, Long meetingId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() != MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_NOT_ENDED");
        }

        List<MeetingAudioRecording> mixedRecordings = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);
        if (mixedRecordings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_MIXED_AUDIO");
        }
        Path mixedAudioPath = localFileStorageService.resolveUploadedPath(mixedRecordings.get(0).getRecordingUrl());
        if (mixedAudioPath == null || !Files.exists(mixedAudioPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIXED_AUDIO_NOT_FOUND");
        }

        List<MeetingAudioRecording> individualRecordings = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.INDIVIDUAL);
        Map<Long, Path> individualPaths = new HashMap<>();
        for (MeetingAudioRecording rec : individualRecordings) {
            if (rec.getUserId() != null) {
                Path path = localFileStorageService.resolveUploadedPath(rec.getRecordingUrl());
                if (path != null && Files.exists(path)) {
                    individualPaths.put(rec.getUserId(), path);
                }
            }
        }

        meeting.updateSummaryStatus(SummaryStatus.PROCESSING);
        String jobId = aiService.processMeetingAsync(mixedAudioPath, individualPaths, true);

        log.info("AI 처리 시작 - meetingId: {}, jobId: {}", meetingId, jobId);
        return jobId;
    }

    @Transactional
    public String checkAndSaveAiResult(Long studyId, Long meetingId, String jobId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);

        AiService.MeetingProcessResult result = aiService.getMeetingProcessResult(jobId);

        if ("completed".equals(result.getStatus())) {
            if (result.getTranscript() != null) {
                String sttFileUrl = localFileStorageService.saveMeetingTextContent(
                        meetingId, null, true, "stt.txt", result.getTranscript());
                meetingSttService.saveSttFile(meetingId, sttFileUrl);
            }

            MeetingSttSummary summary = meetingSttService.getOrCreateSummary(meetingId);

            if (result.getSummary() != null) {
                String summaryFileUrl = localFileStorageService.saveMeetingTextContent(
                        meetingId, null, true, "summary.txt", result.getSummary());
                summary.updateFileUrl(summaryFileUrl);
            }

            if (result.getKeywords() != null && !result.getKeywords().isEmpty()) {
                summary.updateKeywordsJson(helper.writeJson(result.getKeywords()));
            }

            // action_items는 사용하지 않으므로 저장하지 않음

            meetingSttService.saveSummary(summary);

            meeting.updateSummaryStatus(SummaryStatus.DONE);
            log.info("AI 처리 완료 - meetingId: {}", meetingId);

        } else if ("failed".equals(result.getStatus())) {
            meeting.updateSummaryStatus(SummaryStatus.PENDING);
            log.error("AI 처리 실패 - meetingId: {}, error: {}", meetingId, result.getError());
        }

        return result.getStatus();
    }
}
