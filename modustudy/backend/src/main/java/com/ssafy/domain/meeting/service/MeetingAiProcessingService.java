package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.ai.service.AiService;
import com.ssafy.domain.meeting.dto.response.MeetingActionItemResponse;
import com.ssafy.domain.meeting.entity.ActionItemStatus;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.SummarySource;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.quiz.service.StudyQuizService;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.repository.StudyMemberRepository;
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
import java.util.ArrayList;

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
    private final NotificationService notificationService;
    private final StudyMemberRepository studyMemberRepository;

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
            return jobId;
        }

        // 2. 실시간 STT 없으면 기존 방식 (오디오 파일 STT + 요약)
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

            // 요약 소스 설정: 실시간 STT 세그먼트가 있으면 REALTIME_STT, 없으면 FULL_AUDIO
            long segmentCount = speechSegmentService.countByMeetingId(meetingId);
            SummarySource summarySource = segmentCount > 0 ? SummarySource.REALTIME_STT : SummarySource.FULL_AUDIO;
            summary.updateSummarySource(summarySource);
            if (result.getSummary() != null) {
                String summaryFileUrl = localFileStorageService.saveMeetingTextContent(
                        meetingId, null, true, "summary.txt", result.getSummary());
                summary.updateFileUrl(summaryFileUrl);
            }

            if (result.getKeywords() != null && !result.getKeywords().isEmpty()) {
                summary.updateKeywordsJson(helper.writeJson(result.getKeywords()));
            }

            if (result.getHighlights() != null && !result.getHighlights().isEmpty()) {
                summary.updateHighlightsJson(helper.writeJson(result.getHighlights()));
}

            // 액션 아이템 저장
            if (result.getActionItems() != null && !result.getActionItems().isEmpty()) {
                List<MeetingActionItemResponse> actionItemResponses = new ArrayList<>();
                for (AiService.MeetingProcessResult.ActionItem item : result.getActionItems()) {
                    var saved = meetingActionItemService.saveActionItem(
                            meetingId,
                            item.getContent(),
                            item.getUserId(),
                            ActionItemStatus.TODO
                    );
                    actionItemResponses.add(new MeetingActionItemResponse(
                            saved.getId(),
                            saved.getContent(),
                            saved.getAssigneeId(),
                            saved.getStatus()
                    ));
                }
                summary.updateActionItemsJson(helper.writeJson(actionItemResponses));
}

            meetingSttService.saveSummary(summary);

            // 퀴즈 저장 (스터디 + 세션(회차) + 미팅 연결)
            if (result.getQuizRaw() != null && !result.getQuizRaw().isBlank()) {
                String quizTitle = meeting.getTitle() != null
                        ? meeting.getTitle() + " 복습 퀴즈"
                        : "미팅 복습 퀴즈";
                studyQuizService.saveQuizFromMeeting(studyId, meeting.getSessionId(), meetingId, quizTitle, result.getQuizRaw());
            }

            if (hasData) {
                meeting.updateSummaryStatus(SummaryStatus.DONE);
// 미팅 참가자들에게 AI 요약 완료 알림 전송
                sendAiSummaryCompletedNotification(meeting, meetingId);
            } else {
                meeting.updateSummaryStatus(SummaryStatus.PENDING);
}

        } else if ("failed".equals(result.getStatus())) {
            meeting.updateSummaryStatus(SummaryStatus.PENDING);
}

        return result.getStatus();
    }

    /**
     * 스터디원 모두에게 AI 요약 완료 알림 전송
     */
    private void sendAiSummaryCompletedNotification(Meeting meeting, Long meetingId) {
        try {
            Long studyId = meeting.getStudyId();
            List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);

            String meetingTitle = meeting.getTitle() != null ? meeting.getTitle() : "미팅";
            String title = "AI 요약 완료";
            String content = meetingTitle + " 미팅의 AI 요약 및 복습 퀴즈가 준비되었습니다.";

            for (StudyMember member : members) {
                Long userId = member.getUserId();
                if (userId != null) {
                    notificationService.createNotification(
                            userId,
                            NotificationType.STUDY_UPDATE,
                            title,
                            content,
                            "meeting",
                            meetingId
                    );
                }
            }

} catch (Exception e) {
}
    }
}

