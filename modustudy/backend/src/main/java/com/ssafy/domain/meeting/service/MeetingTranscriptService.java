package com.ssafy.domain.meeting.service;

import com.ssafy.domain.meeting.dto.request.MeetingTranscriptRequest;
import com.ssafy.domain.meeting.dto.response.MeetingTranscriptItemResponse;
import com.ssafy.domain.meeting.dto.response.MeetingUserResponse;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingTranscript;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import com.ssafy.domain.meeting.repository.MeetingTranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingTranscriptService {

    private final MeetingRepository meetingRepository;
    private final MeetingTranscriptRepository meetingTranscriptRepository;

    @Transactional
    public MeetingTranscriptItemResponse addTranscript(Long studyId, Long meetingId, MeetingTranscriptRequest request) {
        Meeting meeting = meetingRepository.findByIdAndStudyId(meetingId, studyId)
                .orElseThrow(() -> new IllegalArgumentException("미팅을 찾을 수 없습니다."));

        if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 미팅에만 트랜스크립트를 추가할 수 있습니다.");
        }

        MeetingTranscript transcript = MeetingTranscript.create(
                meetingId,
                request.userId(),
                request.content(),
                request.timestampSeconds()
        );

        transcript = meetingTranscriptRepository.save(transcript);

        MeetingUserResponse userResponse = new MeetingUserResponse(request.userId(), null);

        return new MeetingTranscriptItemResponse(
                transcript.getId(),
                userResponse,
                transcript.getContent(),
                transcript.getTimestampSeconds(),
                request.startMs(),
                request.endMs(),
                transcript.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<MeetingTranscriptItemResponse> getTranscripts(Long studyId, Long meetingId) {
        meetingRepository.findByIdAndStudyId(meetingId, studyId)
                .orElseThrow(() -> new IllegalArgumentException("미팅을 찾을 수 없습니다."));

        return meetingTranscriptRepository.findByMeetingIdOrderByTimestampSecondsAsc(meetingId)
                .stream()
                .map(t -> new MeetingTranscriptItemResponse(
                        t.getId(),
                        new MeetingUserResponse(t.getUserId(), null),
                        t.getContent(),
                        t.getTimestampSeconds(),
                        null,
                        null,
                        t.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getTranscriptText(Long meetingId) {
        List<String> contents = meetingTranscriptRepository.findAllContentByMeetingId(meetingId);
        return String.join(" ", contents);
    }
}
