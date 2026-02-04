package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {
    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false)
    private MeetingType meetingType;

    @Column(name = "started_at")
    private java.time.LocalDateTime startedAt;

    @Column(name = "ended_at")
    private java.time.LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "planned_duration_seconds")
    private Integer plannedDurationSeconds;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MeetingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "recording_status", nullable = false)
    private RecordingStatus recordingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "stt_status", nullable = false)
    private SttStatus sttStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", nullable = false)
    private SummaryStatus summaryStatus;

    @Column(name = "auto_share_summary", nullable = false)
    private Boolean autoShareSummary;

    @Column(name = "share_workspace_id")
    private Long shareWorkspaceId;

    @Builder
    private Meeting(Long studyId, Long sessionId, Long workspaceId, String title, MeetingType meetingType,
                    java.time.LocalDateTime startedAt, java.time.LocalDateTime endedAt,
                    Integer durationSeconds, Integer plannedDurationSeconds, Integer participantCount, MeetingStatus status,
                    RecordingStatus recordingStatus, SttStatus sttStatus, SummaryStatus summaryStatus,
                    Boolean autoShareSummary, Long shareWorkspaceId) {
        this.studyId = studyId;
        this.sessionId = sessionId;
        this.workspaceId = workspaceId;
        this.title = title;
        this.meetingType = meetingType;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.plannedDurationSeconds = plannedDurationSeconds;
        this.participantCount = participantCount;
        this.status = status;
        this.recordingStatus = recordingStatus;
        this.sttStatus = sttStatus;
        this.summaryStatus = summaryStatus;
        this.autoShareSummary = autoShareSummary;
        this.shareWorkspaceId = shareWorkspaceId;
    }

    public static Meeting start(Long studyId, Long sessionId, Long workspaceId, String title, MeetingType meetingType,
                                boolean autoShareSummary, Long shareWorkspaceId, java.time.LocalDateTime startedAt,
                                Integer plannedDurationSeconds) {
        return Meeting.builder()
                .studyId(studyId)
                .sessionId(sessionId)
                .workspaceId(workspaceId)
                .title(title)
                .meetingType(meetingType)
                .startedAt(startedAt)
                .participantCount(0)
                .plannedDurationSeconds(plannedDurationSeconds)
                .status(MeetingStatus.IN_PROGRESS)
                .recordingStatus(RecordingStatus.RECORDING)
                .sttStatus(SttStatus.PENDING)
                .summaryStatus(SummaryStatus.PENDING)
                .autoShareSummary(autoShareSummary)
                .shareWorkspaceId(shareWorkspaceId)
                .build();
    }

    public static Meeting schedule(Long studyId, Long sessionId, Long workspaceId, String title, MeetingType meetingType,
                                   java.time.LocalDateTime scheduledAt, Integer plannedDurationSeconds) {
        return Meeting.builder()
                .studyId(studyId)
                .sessionId(sessionId)
                .workspaceId(workspaceId)
                .title(title)
                .meetingType(meetingType)
                .startedAt(scheduledAt)
                .participantCount(0)
                .plannedDurationSeconds(plannedDurationSeconds)
                .status(MeetingStatus.WAITING)
                .recordingStatus(RecordingStatus.WAITING)
                .sttStatus(SttStatus.PENDING)
                .summaryStatus(SummaryStatus.PENDING)
                .autoShareSummary(false)
                .build();
    }

    /**
     * 오프라인 녹음용 회의 생성 (즉시 ENDED 상태로 생성)
     */
    public static Meeting createOffline(Long studyId, String title, java.time.LocalDateTime now, Integer durationSeconds) {
        return Meeting.builder()
                .studyId(studyId)
                .title(title)
                .meetingType(MeetingType.OFFLINE)
                .startedAt(now)
                .endedAt(now)
                .durationSeconds(durationSeconds)
                .participantCount(1)
                .status(MeetingStatus.ENDED)
                .recordingStatus(RecordingStatus.READY)
                .sttStatus(SttStatus.PENDING)
                .summaryStatus(SummaryStatus.PROCESSING)
                .autoShareSummary(false)
                .build();
    }

    public void startFromWaiting(java.time.LocalDateTime now) {
        if (this.status != MeetingStatus.WAITING) {
            return;
        }
        this.status = MeetingStatus.IN_PROGRESS;
        if (this.startedAt == null) {
            this.startedAt = now;
        }
        this.recordingStatus = RecordingStatus.RECORDING;
    }

    public void end(java.time.LocalDateTime endedAt, int participantCount) {
        this.endedAt = endedAt;
        if (startedAt != null) {
            this.durationSeconds = (int) java.time.Duration.between(startedAt, endedAt).getSeconds();
        }
        this.participantCount = participantCount;
        this.status = MeetingStatus.ENDED;
    }

    public void updateParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public void updateRecordingStatus(RecordingStatus recordingStatus) {
        this.recordingStatus = recordingStatus;
    }

    public void updateSttStatus(SttStatus sttStatus) {
        this.sttStatus = sttStatus;
    }

    public void updateSummaryStatus(SummaryStatus summaryStatus) {
        this.summaryStatus = summaryStatus;
    }

    public void updateShareOption(boolean autoShareSummary, Long shareWorkspaceId) {
        this.autoShareSummary = autoShareSummary;
        this.shareWorkspaceId = shareWorkspaceId;
    }

    public void updatePlannedDurationSeconds(Integer plannedDurationSeconds) {
        this.plannedDurationSeconds = plannedDurationSeconds;
    }
}
