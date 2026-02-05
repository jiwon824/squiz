package com.ssafy.domain.meeting.service;

import com.ssafy.config.SfuProperties;
import com.ssafy.domain.meeting.dto.request.MeetingRequest;
import com.ssafy.domain.meeting.dto.response.*;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingParticipant;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import com.ssafy.domain.meeting.entity.MeetingType;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.entity.StudyDailyUsage;
import com.ssafy.domain.meeting.repository.MeetingActionItemRepository;
import com.ssafy.domain.meeting.repository.MeetingParticipantRepository;
import com.ssafy.domain.meeting.repository.MeetingPhotoRepository;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import com.ssafy.domain.meeting.repository.MeetingSttFileRepository;
import com.ssafy.domain.meeting.repository.MeetingSttSummaryRepository;
import com.ssafy.domain.attendance.service.AttendanceService;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.study.service.StudySessionService;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);
    private static final int MAX_PLANNED_DURATION_SECONDS = 3 * 60 * 60;

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingPhotoRepository meetingPhotoRepository;
    private final MeetingSttFileRepository meetingSttFileRepository;
    private final MeetingSttSummaryRepository meetingSttSummaryRepository;
    private final MeetingActionItemRepository meetingActionItemRepository;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;
    private final SfuProperties sfuProperties;
    private final MeetingServiceHelper helper;
    private final MeetingRecordingService meetingRecordingService;
    private final MeetingAudioService meetingAudioService;
    private final StudyDailyUsageService dailyUsageService;

    @Transactional(readOnly = true)
    public Page<MeetingListItemResponse> listMeetings(Long studyId, MeetingType meetingType,
                                                      LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endAt = endDate == null ? null : endDate.plusDays(1).atStartOfDay().minusNanos(1);
        return meetingRepository.searchMeetings(studyId, meetingType, startAt, endAt, pageable)
                .map(meeting -> new MeetingListItemResponse(
                        meeting.getId(),
                        meeting.getTitle(),
                        meeting.getSessionId() == null ? null : new MeetingSessionResponse(meeting.getSessionId(), null, null),
                        (meeting.getMeetingType() == null ? MeetingType.OTHER : meeting.getMeetingType()).name(),
                        meeting.getStartedAt(),
                        meeting.getEndedAt(),
                        meeting.getDurationSeconds(),
                        meeting.getParticipantCount(),
                        meetingSttSummaryRepository
                                .existsByMeetingIdAndTrackTypeAndUserIdIsNull(meeting.getId(),
                                        MeetingTextTrackType.MIXED),
                        meetingSttFileRepository
                                .existsByMeetingIdAndTrackTypeAndUserIdIsNull(meeting.getId(),
                                        MeetingTextTrackType.MIXED),
                        meetingPhotoRepository.countByMeetingId(meeting.getId())
                ));
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse getMeetingDetail(Long studyId, Long meetingId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        List<MeetingParticipant> participantEntities = meetingParticipantRepository.findByMeetingId(meetingId);

        // 모든 userId를 수집하여 한번에 User 정보 조회 (N+1 방지)
        List<Long> userIds = participantEntities.stream()
                .map(MeetingParticipant::getUserId)
                .distinct()
                .toList();
        Map<Long, String> userNicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<MeetingParticipantResponse> participants = participantEntities.stream()
                .map(participant -> new MeetingParticipantResponse(
                        participant.getUserId(),
                        userNicknameMap.get(participant.getUserId()),
                        participant.getJoinedAt(),
                        participant.getLeftAt()))
                .toList();

        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        SummaryStatus summaryStatus = helper.resolveSummaryStatus(meeting);
        String summaryText = summary == null ? null : helper.readUploadedTextFile(summary.getFileUrl());

        // meeting_action_item 테이블에서 직접 조회 (meeting_stt_summary.action_items_json 대신)
        List<MeetingActionItemResponse> actionItems = meetingActionItemRepository.findByMeetingId(meetingId).stream()
                .map(item -> new MeetingActionItemResponse(
                        item.getId(),
                        item.getContent(),
                        item.getAssigneeId(),
                        item.getStatus()))
                .toList();

        // summary가 없어도 actionItems가 있으면 summaryResponse 생성
        MeetingSummaryResponse summaryResponse;
        if (summary != null) {
            summaryResponse = new MeetingSummaryResponse(
                    summary.getId(),
                    summaryText,
                    actionItems,
                    helper.parseKeywords(summary.getKeywordsJson()),
                    helper.parseKeywords(summary.getHighlightsJson()),  // highlights도 문자열 배열이므로 동일 메서드 사용
                    summaryStatus.name(),
                    summary.getCreatedAt()
            );
        } else if (!actionItems.isEmpty()) {
            // summary 엔티티는 없지만 actionItems가 있는 경우
            summaryResponse = new MeetingSummaryResponse(
                    null,
                    null,
                    actionItems,
                    List.of(),
                    List.of(),
                    summaryStatus.name(),
                    null
            );
        } else {
            summaryResponse = null;
        }

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getSessionId() == null ? null : new MeetingSessionResponse(meeting.getSessionId(), null, null),
                meeting.getWorkspaceId() == null ? null : new MeetingWorkspaceResponse(meeting.getWorkspaceId(), null),
                (meeting.getMeetingType() == null ? MeetingType.OTHER : meeting.getMeetingType()).name(),
                meeting.getStartedAt(),
                meeting.getEndedAt(),
                meeting.getDurationSeconds(),
                meeting.getPlannedDurationSeconds(),
                meeting.getStatus().name(),
                meeting.getRecordingStatus().name(),
                meeting.getSttStatus().name(),
                summaryStatus.name(),
                meeting.getAutoShareSummary(),
                meeting.getShareWorkspaceId(),
                participants,
                summary == null ? List.of() : helper.parseKeywords(summary.getKeywordsJson()),
                summaryResponse
        );
    }

    @Transactional
    public MeetingResponse startMeeting(Long studyId, MeetingRequest request) {
        if (meetingRepository.existsByStudyIdAndStatus(studyId, MeetingStatus.IN_PROGRESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_IN_PROGRESS");
        }

        // 일일 온라인 미팅 한도 체크 (3시간)
        int remainingSeconds = dailyUsageService.getOnlineMeetingRemainingSeconds(studyId);
        if (remainingSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "DAILY_MEETING_LIMIT_EXCEEDED: 오늘의 온라인 미팅 한도(3시간)를 초과했습니다.");
        }

        MeetingType meetingType = request.meetingType() == null ? MeetingType.OTHER : request.meetingType();
        boolean autoShareSummary = Boolean.TRUE.equals(request.autoShareSummary());
        int plannedDurationSeconds = resolvePlannedDurationSeconds(
                studyId, request.sessionId(), request.plannedDurationSeconds());

        // 남은 시간보다 계획 시간이 길면 남은 시간으로 제한
        if (plannedDurationSeconds > remainingSeconds) {
            plannedDurationSeconds = remainingSeconds;
            log.info("[일일 한도] 계획 시간을 남은 한도로 제한 - studyId: {}, 남은: {}초", studyId, remainingSeconds);
        }

        Meeting meeting = Meeting.start(studyId, request.sessionId(), request.workspaceId(),
                request.title(), meetingType, autoShareSummary, request.shareWorkspaceId(), LocalDateTime.now(),
                plannedDurationSeconds);
        Meeting saved = meetingRepository.save(meeting);
        meetingRecordingService.triggerSfuRecordingStart(saved.getId());

        log.info("[미팅 시작] studyId: {}, meetingId: {}, 남은 일일 한도: {}초", studyId, saved.getId(), remainingSeconds);

        return new MeetingResponse(saved.getId(), saved.getTitle(), helper.buildRoomToken(saved), saved.getStatus().name(),
                saved.getMeetingType().name(), saved.getRecordingStatus().name(), saved.getSttStatus().name(),
                helper.resolveSummaryStatus(saved).name(), remainingSeconds);
    }

    @Transactional
    public MeetingDetailResponse updatePlannedDuration(Long studyId, Long meetingId, Long userId,
                                                       Integer plannedDurationSeconds) {
        validateLeader(studyId, userId);
        if (plannedDurationSeconds == null || plannedDurationSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLANNED_DURATION_REQUIRED");
        }
        if (plannedDurationSeconds > MAX_PLANNED_DURATION_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLANNED_DURATION_TOO_LONG");
        }
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        Integer current = meeting.getPlannedDurationSeconds();
        if (current != null && plannedDurationSeconds < current) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLANNED_DURATION_CANNOT_DECREASE");
        }
        meeting.updatePlannedDurationSeconds(plannedDurationSeconds);
        if (meeting.getSessionId() != null) {
            int minutes = (int) Math.ceil(plannedDurationSeconds / 60.0);
            studySessionService.updateDurationFromMeeting(studyId, meeting.getSessionId(), minutes);
        }
        return getMeetingDetail(studyId, meetingId);
    }

    @Transactional
    public MeetingEndResponse endMeeting(Long studyId, Long meetingId, Long userId) {
        validateLeader(studyId, userId);
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        List<MeetingParticipant> participants = meetingParticipantRepository.findByMeetingId(meetingId);
        LocalDateTime endedAt = LocalDateTime.now();
        for (MeetingParticipant participant : participants) {
            if (participant.getLeftAt() == null) {
                participant.leave(endedAt);
            }
        }
        int participantCount = participants.size();
        meeting.end(endedAt, participantCount);
        meeting.updateSummaryStatus(SummaryStatus.PROCESSING);
        meetingAudioService.finalizeIndividualVoiceRecordings(meetingId, participants);
        meetingRecordingService.triggerSfuRecordingStop(meetingId);
        if (meeting.getSessionId() != null && meeting.getDurationSeconds() != null) {
            int minutes = (int) Math.ceil(meeting.getDurationSeconds() / 60.0);
            studySessionService.updateDurationFromMeeting(studyId, meeting.getSessionId(), minutes);
        }
        // 미팅 종료 시 참가하지 않은 멤버 ABSENT 처리
        if (meeting.getSessionId() != null) {
            List<Long> participantUserIds = participants.stream()
                    .map(MeetingParticipant::getUserId)
                    .collect(Collectors.toList());
            try {
                attendanceService.markAbsentForNonParticipants(studyId, meeting.getSessionId(), participantUserIds);
            } catch (Exception ex) {
                log.warn("Failed to mark absent for non-participants. studyId={}, sessionId={}, error={}",
                        studyId, meeting.getSessionId(), ex.getMessage());
            }
        }

        // 일일 사용량 기록 (온라인 미팅)
        if (meeting.getDurationSeconds() != null && meeting.getDurationSeconds() > 0) {
            dailyUsageService.addOnlineMeetingUsage(studyId, meeting.getDurationSeconds());
        }

        return new MeetingEndResponse(meeting.getDurationSeconds(), meeting.getParticipantCount(),
                meeting.getSummaryStatus().name());
    }

    @Transactional(readOnly = true)
    public void startMeetingEnding(Long studyId, Long meetingId, Long userId) {
        validateLeader(studyId, userId);
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
    }

    /**
     * 미팅 강제 종료 (일일 한도 초과 시 시스템에서 호출)
     */
    @Transactional
    public MeetingEndResponse forceEndMeeting(Long studyId, Long meetingId, String reason) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            log.warn("[강제 종료] 이미 종료된 미팅 - meetingId: {}", meetingId);
            return new MeetingEndResponse(meeting.getDurationSeconds(), meeting.getParticipantCount(),
                    meeting.getSummaryStatus().name());
        }

        log.info("[강제 종료] 미팅 강제 종료 - studyId: {}, meetingId: {}, 사유: {}", studyId, meetingId, reason);

        List<MeetingParticipant> participants = meetingParticipantRepository.findByMeetingId(meetingId);
        LocalDateTime endedAt = LocalDateTime.now();
        for (MeetingParticipant participant : participants) {
            if (participant.getLeftAt() == null) {
                participant.leave(endedAt);
            }
        }
        int participantCount = participants.size();
        meeting.end(endedAt, participantCount);
        meeting.updateSummaryStatus(SummaryStatus.PROCESSING);
        meetingAudioService.finalizeIndividualVoiceRecordings(meetingId, participants);
        meetingRecordingService.triggerSfuRecordingStop(meetingId);

        // 일일 사용량 기록
        if (meeting.getDurationSeconds() != null && meeting.getDurationSeconds() > 0) {
            dailyUsageService.addOnlineMeetingUsage(studyId, meeting.getDurationSeconds());
        }

        return new MeetingEndResponse(meeting.getDurationSeconds(), meeting.getParticipantCount(),
                meeting.getSummaryStatus().name());
    }

    @Transactional
    public MeetingJoinResponse joinMeeting(Long studyId, Long meetingId, Long userId) {
        log.info("joinMeeting called. studyId={} meetingId={} userId={}", studyId, meetingId, userId);
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        meeting.startFromWaiting(LocalDateTime.now());
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseGet(() -> MeetingParticipant.join(meetingId, userId, LocalDateTime.now()));
        if (participant.getId() != null) {
            participant.rejoin(LocalDateTime.now());
        }
        meetingParticipantRepository.save(participant);
        int participantCount = meetingParticipantRepository.countByMeetingId(meetingId);
        meeting.updateParticipantCount(participantCount);

        if (meeting.getSessionId() != null) {
            studySessionRepository.findById(meeting.getSessionId()).ifPresent(session -> {
                if (Boolean.TRUE.equals(session.getIsOnline())) {
                    try {
                        attendanceService.checkAttendanceAutoOnline(studyId, session.getId(), userId);
                    } catch (Exception ex) {
                        log.warn("Auto attendance check failed. studyId={}, sessionId={}, userId={}, error={}",
                                studyId, session.getId(), userId, ex.getMessage());
                    }
                }
            });
        }

        // 참가할 때마다 SFU 녹음 시작 시도 (이미 녹음 중이면 SFU에서 already-recording 반환)
        meetingRecordingService.triggerSfuRecordingStart(meetingId);

        List<MeetingIceServerResponse> iceServers = sfuProperties.getIceServers().stream()
                .filter(server -> server.getUrls() != null && !server.getUrls().isBlank())
                .map(server -> new MeetingIceServerResponse(
                        server.getUrls(),
                        server.getUsername(),
                        server.getCredential()))
                .toList();
        return new MeetingJoinResponse(helper.buildRoomToken(meeting), iceServers);
    }

    @Transactional
    public void leaveMeeting(Long studyId, Long meetingId, Long userId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_IN_MEETING"));
        participant.leave(LocalDateTime.now());
        meeting.updateParticipantCount(meetingParticipantRepository.countByMeetingId(meetingId));
        meetingAudioService.concatVoiceSegmentsIfExists(meetingId, userId);
    }

    @Transactional
    public void updateParticipantMute(Long studyId, Long meetingId, Long userId, boolean muted) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_IN_MEETING"));
        participant.updateMute(muted);
    }

    /**
     * 오프라인 녹음 업로드 (회의 생성 + 오디오 업로드 + AI 처리 트리거)
     * 온라인 회의와 달리 IN_PROGRESS 체크 없이 바로 ENDED 상태로 생성
     * @param sessionId 연결할 세션 ID (선택사항, null이면 세션 없이 생성)
     */
    @Transactional
    public MeetingResponse createOfflineMeetingWithAudio(Long studyId, Long sessionId, String title,
                                                          org.springframework.web.multipart.MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED");
        }

        // sessionId 유효성 검증 (지정된 경우)
        if (sessionId != null) {
            boolean validSession = studySessionRepository.findById(sessionId)
                    .filter(session -> session.getStudyId().equals(studyId))
                    .isPresent();
            if (!validSession) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SESSION_ID");
            }
        }

        // 일일 오프라인 STT 한도 체크
        int remainingSeconds = dailyUsageService.getOfflineSttRemainingSeconds(studyId);
        if (remainingSeconds <= 0) {
            log.warn("[일일 한도 초과] 오프라인 녹음 업로드 차단 - studyId: {}", studyId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OFFLINE_STT_DAILY_LIMIT_EXCEEDED");
        }

        LocalDateTime now = LocalDateTime.now();
        Meeting meeting = Meeting.createOffline(studyId, sessionId, title, now, null);
        Meeting saved = meetingRepository.save(meeting);

        // 오디오 파일 저장
        String filename = "voice.webm";
        String recordingUrl = helper.getLocalFileStorageService().saveMeetingVoiceFinal(saved.getId(), filename, audio);

        // 오디오 길이 추출 및 사용량 기록
        java.nio.file.Path audioFile = helper.getLocalFileStorageService().resolveMeetingVoiceFile(saved.getId(), filename);
        int durationSeconds = helper.getAudioDurationSeconds(audioFile);
        if (durationSeconds > 0) {
            dailyUsageService.addOfflineSttUsage(studyId, durationSeconds);
            // 미팅에 duration 업데이트
            saved.updateDurationSeconds(durationSeconds);
        }

        // 세션이 있으면 세션 완료 처리 (온라인 미팅 종료와 동일한 처리)
        if (sessionId != null) {
            studySessionRepository.findById(sessionId).ifPresent(session -> {
                session.complete();
                if (durationSeconds > 0) {
                    int minutes = (int) Math.ceil(durationSeconds / 60.0);
                    studySessionService.updateDurationFromMeeting(studyId, sessionId, minutes);
                }
            });
        }

        log.info("[오프라인 녹음] 업로드 완료 - studyId: {}, sessionId: {}, meetingId: {}, duration: {}초",
                studyId, sessionId, saved.getId(), durationSeconds);

        return new MeetingResponse(saved.getId(), saved.getTitle(), null, saved.getStatus().name(),
                saved.getMeetingType().name(), saved.getRecordingStatus().name(), saved.getSttStatus().name(),
                helper.resolveSummaryStatus(saved).name(), remainingSeconds);
    }

    private void validateLeader(Long studyId, Long userId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND"));
        if (!study.getLeaderId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "LEADER_ONLY");
        }
    }

    private int resolvePlannedDurationSeconds(Long studyId, Long sessionId, Integer requestedSeconds) {
        int planned = requestedSeconds == null ? 3600 : requestedSeconds;
        if (planned <= 0) {
            planned = 3600;
        }
        if (sessionId != null && requestedSeconds == null) {
            Integer minutes = studySessionRepository.findById(sessionId)
                    .filter(session -> session.getStudyId().equals(studyId))
                    .map(session -> session.getDurationMinutes())
                    .orElse(null);
            if (minutes != null && minutes > 0) {
                planned = minutes * 60;
            }
        }
        if (planned > MAX_PLANNED_DURATION_SECONDS) {
            planned = MAX_PLANNED_DURATION_SECONDS;
        }
        return planned;
    }
}
