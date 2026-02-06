package com.ssafy.domain.meeting.service;

import com.ssafy.domain.meeting.dto.request.MeetingTranscriptRequest;
import com.ssafy.domain.meeting.dto.response.MeetingTranscriptItemResponse;
import com.ssafy.domain.meeting.dto.response.MeetingUserResponse;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingTranscript;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import com.ssafy.domain.meeting.repository.MeetingTranscriptRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingTranscriptService {

    private final MeetingRepository meetingRepository;
    private final MeetingTranscriptRepository meetingTranscriptRepository;
    private final UserRepository userRepository;

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

        // 사용자 닉네임 조회
        String nickname = userRepository.findById(request.userId())
                .map(User::getNickname)
                .orElse(null);

        MeetingUserResponse userResponse = new MeetingUserResponse(request.userId(), nickname);

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

        List<MeetingTranscript> transcripts = meetingTranscriptRepository
                .findByMeetingIdOrderByTimestampSecondsAsc(meetingId);

        // 모든 userId를 수집하여 한번에 User 정보 조회 (N+1 방지)
        List<Long> userIds = transcripts.stream()
                .map(MeetingTranscript::getUserId)
                .distinct()
                .toList();

        Map<Long, String> userNicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return transcripts.stream()
                .map(t -> new MeetingTranscriptItemResponse(
                        t.getId(),
                        new MeetingUserResponse(t.getUserId(), userNicknameMap.get(t.getUserId())),
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
