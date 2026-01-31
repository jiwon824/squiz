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
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudySessionService studySessionService;
    private final SfuProperties sfuProperties;
    private final MeetingServiceHelper helper;
    private final MeetingRecordingService meetingRecordingService;
    private final MeetingAudioService meetingAudioService;

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
        MeetingSummaryResponse summaryResponse = summary == null ? null : new MeetingSummaryResponse(
                summary.getId(),
                summaryText,
                helper.parseActionItems(summary.getActionItemsJson()),
                helper.parseKeywords(summary.getKeywordsJson()),
                summaryStatus.name(),
                summary.getCreatedAt()
        );

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
        MeetingType meetingType = request.meetingType() == null ? MeetingType.OTHER : request.meetingType();
        boolean autoShareSummary = Boolean.TRUE.equals(request.autoShareSummary());
        int plannedDurationSeconds = resolvePlannedDurationSeconds(
                studyId, request.sessionId(), request.plannedDurationSeconds());
        Meeting meeting = Meeting.start(studyId, request.sessionId(), request.workspaceId(),
                request.title(), meetingType, autoShareSummary, request.shareWorkspaceId(), LocalDateTime.now(),
                plannedDurationSeconds);
        Meeting saved = meetingRepository.save(meeting);
        meetingRecordingService.triggerSfuRecordingStart(saved.getId());
        return new MeetingResponse(saved.getId(), saved.getTitle(), helper.buildRoomToken(saved), saved.getStatus().name(),
                saved.getMeetingType().name(), saved.getRecordingStatus().name(), saved.getSttStatus().name(),
                helper.resolveSummaryStatus(saved).name());
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
