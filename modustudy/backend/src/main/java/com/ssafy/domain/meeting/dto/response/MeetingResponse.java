package com.ssafy.domain.meeting.dto.response;

public record MeetingResponse(
        Long id,
        String title,
        String roomToken,
        String status,
        String meetingType,
        String recordingStatus,
        String sttStatus,
        String summaryStatus,
        /** 오늘 남은 온라인 미팅 시간 (초), 한도 3시간 */
        Integer dailyRemainingSeconds
        ) {
    // 기존 생성자 호환용
    public MeetingResponse(Long id, String title, String roomToken, String status,
                           String meetingType, String recordingStatus, String sttStatus, String summaryStatus) {
        this(id, title, roomToken, status, meetingType, recordingStatus, sttStatus, summaryStatus, null);
    }
}
