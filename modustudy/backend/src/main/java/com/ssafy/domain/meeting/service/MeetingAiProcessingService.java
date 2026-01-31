package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.ai.service.AiService;
import com.ssafy.domain.meeting.entity.ActionItemStatus;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import com.ssafy.domain.quiz.service.StudyQuizService;
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
    private final MeetingActionItemService meetingActionItemService;
    private final StudyQuizService studyQuizService;
    private final SpeechSegmentService speechSegmentService;

    @Transactional
    public String startAiProcessing(Long studyId, Long meetingId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() != MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_NOT_ENDED");
        }

        meeting.updateSummaryStatus(SummaryStatus.PROCESSING);

        // 1. 실시간 STT segments 확인 (미팅 중 수집된 발화 세그먼트)
        long segmentCount = speechSegmentService.countByMeetingId(meetingId);

        if (segmentCount > 0) {
            // 실시간 STT 결과가 있으면 transcript-only 처리 (STT 스킵)
            log.info("실시간 STT 세그먼트 발견 - meetingId: {}, count: {}", meetingId, segmentCount);

            // 시간순으로 정렬된 transcript 조회
            List<String> transcriptLines = speechSegmentService.getTranscriptByMeetingId(meetingId);
            String transcript = String.join("\n", transcriptLines);

            // 화자 ID 목록 추출 (개별 녹음에서)
            List<MeetingAudioRecording> individualRecordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.INDIVIDUAL);
            List<Long> speakerIds = individualRecordings.stream()
                    .map(MeetingAudioRecording::getUserId)
                    .filter(userId -> userId != null)
                    .distinct()
                    .toList();

            String jobId = aiService.summarizeTranscriptAsync(transcript, speakerIds, true);
            log.info("Transcript 기반 AI 처리 시작 - meetingId: {}, jobId: {}", meetingId, jobId);
            return jobId;
        }

        // 2. 실시간 STT 없으면 기존 방식 (오디오 파일 STT + 요약)
        log.info("실시간 STT 없음, 오디오 파일 처리 - meetingId: {}", meetingId);

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

        String jobId = aiService.processMeetingAsync(mixedAudioPath, individualPaths, true);

        log.info("AI 처리 시작 - meetingId: {}, jobId: {}", meetingId, jobId);
        return jobId;
    }

    @Transactional
    public String checkAndSaveAiResult(Long studyId, Long meetingId, String jobId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);

        AiService.MeetingProcessResult result = aiService.getMeetingProcessResult(jobId);

        if ("completed".equals(result.getStatus())) {
            boolean hasData = result.getTranscript() != null
                    || result.getSummary() != null
                    || (result.getKeywords() != null && !result.getKeywords().isEmpty());

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

            meetingSttService.saveSummary(summary);

            // 액션 아이템 저장
            if (result.getActionItems() != null && !result.getActionItems().isEmpty()) {
                for (AiService.MeetingProcessResult.ActionItem item : result.getActionItems()) {
                    meetingActionItemService.saveActionItem(
                            meetingId,
                            item.getContent(),
                            item.getUserId(),
                            ActionItemStatus.TODO
                    );
                }
                log.info("액션 아이템 저장 완료 - meetingId: {}, count: {}", meetingId, result.getActionItems().size());
            }

            // 퀴즈 저장
            if (result.getQuizRaw() != null && !result.getQuizRaw().isBlank()) {
                String quizTitle = meeting.getTitle() != null
                        ? meeting.getTitle() + " 복습 퀴즈"
                        : "미팅 복습 퀴즈";
                studyQuizService.saveQuizFromMeeting(studyId, meetingId, quizTitle, result.getQuizRaw());
            }

            if (hasData) {
                meeting.updateSummaryStatus(SummaryStatus.DONE);
                log.info("AI 처리 완료 - meetingId: {}", meetingId);
            } else {
                meeting.updateSummaryStatus(SummaryStatus.PENDING);
                log.warn("AI 처리 완료했으나 데이터 없음 - meetingId: {}", meetingId);
            }

        } else if ("failed".equals(result.getStatus())) {
            meeting.updateSummaryStatus(SummaryStatus.PENDING);
            log.error("AI 처리 실패 - meetingId: {}, error: {}", meetingId, result.getError());
        }

        return result.getStatus();
    }
}
