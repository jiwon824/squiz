package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.meeting.dto.request.MeetingKeywordUpdateRequest;
import com.ssafy.domain.meeting.dto.request.MeetingSummaryUpdateRequest;
import com.ssafy.domain.meeting.dto.response.MeetingSttFileResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSttSummaryResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSummaryResponse;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingSttFile;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import com.ssafy.domain.meeting.entity.SttStatus;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingSttFileRepository;
import com.ssafy.domain.meeting.repository.MeetingSttSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MeetingSttService {

    private final MeetingSttFileRepository meetingSttFileRepository;
    private final MeetingSttSummaryRepository meetingSttSummaryRepository;
    private final LocalFileStorageService localFileStorageService;
    private final MeetingServiceHelper helper;

    @Transactional(readOnly = true)
    public MeetingSummaryResponse getSummary(Long studyId, Long meetingId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUMMARY_NOT_READY"));
        SummaryStatus currentStatus = helper.resolveSummaryStatus(meeting);
        String summaryText = helper.readUploadedTextFile(summary.getFileUrl());
        if (currentStatus == SummaryStatus.PENDING && summaryText != null && !summaryText.isBlank()) {
            meeting.updateSummaryStatus(SummaryStatus.DONE);
        }
        if (helper.resolveSummaryStatus(meeting) != SummaryStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SUMMARY_NOT_READY");
        }
        return new MeetingSummaryResponse(
                summary.getId(),
                summaryText,
                helper.parseActionItems(summary.getActionItemsJson()),
                helper.parseKeywords(summary.getKeywordsJson()),
                helper.parseKeywords(summary.getHighlightsJson()),
                helper.resolveSummaryStatus(meeting).name(),
                summary.getCreatedAt()
        );
    }

    @Transactional
    public MeetingSummaryResponse upsertSummary(Long studyId, Long meetingId, MeetingSummaryUpdateRequest request) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl("")
                        .build()));
        if (request.summary() != null) {
            String fileUrl = localFileStorageService.saveMeetingTextContent(
                    meetingId, null, true, "summary.txt", request.summary());
            summary.updateFileUrl(fileUrl);
        }
        // action_items는 사용하지 않으므로 저장하지 않음
        if (request.keywords() != null) {
            summary.updateKeywordsJson(helper.writeJson(request.keywords()));
        }
        SummaryStatus status = request.status();
        if (status == null) {
            status = request.summary() == null ? helper.resolveSummaryStatus(meeting) : SummaryStatus.DONE;
        }
        if (status != null) {
            meeting.updateSummaryStatus(status);
        }
        meetingSttSummaryRepository.save(summary);
        String summaryText = helper.readUploadedTextFile(summary.getFileUrl());
        return new MeetingSummaryResponse(
                summary.getId(),
                summaryText,
                helper.parseActionItems(summary.getActionItemsJson()),
                helper.parseKeywords(summary.getKeywordsJson()),
                helper.parseKeywords(summary.getHighlightsJson()),
                helper.resolveSummaryStatus(meeting).name(),
                summary.getCreatedAt()
        );
    }

    @Transactional
    public void updateSummaryStatus(Long studyId, Long meetingId, SummaryStatus status) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        meeting.updateSummaryStatus(status);
    }

    @Transactional
    public void updateKeywords(Long studyId, Long meetingId, MeetingKeywordUpdateRequest request) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl("")
                        .build()));
        summary.updateKeywordsJson(helper.writeJson(request.keywords()));
        meetingSttSummaryRepository.save(summary);
    }

    @Transactional
    public MeetingSttFileResponse uploadSttTextFile(Long studyId, Long meetingId,
                                                    MeetingTextTrackType trackType,
                                                    Long userId,
                                                    MultipartFile file) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED");
        }
        validateTextTrack(trackType, userId);
        boolean mixed = trackType == MeetingTextTrackType.MIXED;
        String fileUrl = localFileStorageService.saveMeetingTextFile(meetingId, userId, mixed, "stt.txt", file);
        MeetingSttFile saved = (userId == null
                ? meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .map(existing -> {
                    existing.updateFileUrl(fileUrl);
                    return meetingSttFileRepository.save(existing);
                })
                .orElseGet(() -> meetingSttFileRepository.save(MeetingSttFile.builder()
                        .meetingId(meetingId)
                        .userId(userId)
                        .trackType(trackType)
                        .fileUrl(fileUrl)
                        .build()));
        return toSttFileResponse(saved);
    }

    @Transactional
    public MeetingSttSummaryResponse uploadSummaryTextFile(Long studyId, Long meetingId,
                                                           MeetingTextTrackType trackType,
                                                           Long userId,
                                                           MultipartFile file) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED");
        }
        validateTextTrack(trackType, userId);
        boolean mixed = trackType == MeetingTextTrackType.MIXED;
        String fileUrl = localFileStorageService.saveMeetingTextFile(meetingId, userId, mixed, "summary.txt", file);
        MeetingSttSummary saved = (userId == null
                ? meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .map(existing -> {
                    existing.updateFileUrl(fileUrl);
                    return meetingSttSummaryRepository.save(existing);
                })
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .userId(userId)
                        .trackType(trackType)
                        .fileUrl(fileUrl)
                        .build()));
        return toSttSummaryResponse(saved);
    }

    @Transactional(readOnly = true)
    public MeetingSttFileResponse getMeetingSttFile(Long studyId, Long meetingId,
                                                    MeetingTextTrackType trackType,
                                                    Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingSttFile file = (userId == null
                ? meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TEXT_FILE_NOT_FOUND"));
        return toSttFileResponse(file);
    }

    @Transactional(readOnly = true)
    public MeetingSttSummaryResponse getMeetingSttSummary(Long studyId, Long meetingId,
                                                          MeetingTextTrackType trackType,
                                                          Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary file = (userId == null
                ? meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TEXT_FILE_NOT_FOUND"));
        return toSttSummaryResponse(file);
    }

    public MeetingSttSummary getOrCreateSummary(Long meetingId) {
        return meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl("")
                        .build()));
    }

    public void saveSummary(MeetingSttSummary summary) {
        meetingSttSummaryRepository.save(summary);
    }

    public void saveSttFile(Long meetingId, String sttFileUrl) {
        meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateFileUrl(sttFileUrl);
                            meetingSttFileRepository.save(existing);
                        },
                        () -> meetingSttFileRepository.save(MeetingSttFile.builder()
                                .meetingId(meetingId)
                                .trackType(MeetingTextTrackType.MIXED)
                                .fileUrl(sttFileUrl)
                                .build())
                );
    }

    @Transactional
    public MeetingSttFileResponse upsertSttFileInternal(Long meetingId, String fileUrl) {
        Meeting meeting = helper.getMeetingOrThrow(meetingId);
        String resolvedUrl = (fileUrl == null || fileUrl.isBlank())
                ? "/uploads/meetings/" + meetingId + "/stt/mixed/stt.txt"
                : fileUrl;
        MeetingSttFile saved = meetingSttFileRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .map(existing -> {
                    existing.updateFileUrl(resolvedUrl);
                    return meetingSttFileRepository.save(existing);
                })
                .orElseGet(() -> meetingSttFileRepository.save(MeetingSttFile.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl(resolvedUrl)
                        .build()));
        meeting.updateSttStatus(SttStatus.DONE);
        return toSttFileResponse(saved);
    }

    @Transactional
    public MeetingSttSummaryResponse upsertSummaryFileInternal(Long meetingId, String fileUrl) {
        Meeting meeting = helper.getMeetingOrThrow(meetingId);
        String resolvedUrl = (fileUrl == null || fileUrl.isBlank())
                ? "/uploads/meetings/" + meetingId + "/stt/mixed/summary.txt"
                : fileUrl;
        MeetingSttSummary saved = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .map(existing -> {
                    existing.updateFileUrl(resolvedUrl);
                    return meetingSttSummaryRepository.save(existing);
                })
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl(resolvedUrl)
                        .build()));
        meeting.updateSummaryStatus(SummaryStatus.DONE);
        return toSttSummaryResponse(saved);
    }

    /**
     * AI 서버에서 STT/요약 텍스트를 직접 저장
     * 파일로 저장 후 DB 레코드 생성, 미팅 상태를 DONE으로 업데이트
     */
    @Transactional
    public MeetingSttSummaryResponse saveAiResultDirect(Long meetingId, String sttText, String summaryText) {
        Meeting meeting = helper.getMeetingOrThrow(meetingId);

        // 1. STT 텍스트 저장
        if (sttText != null && !sttText.isBlank()) {
            String sttFileUrl = localFileStorageService.saveMeetingTextContent(
                    meetingId, null, true, "stt.txt", sttText);
            meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                    .ifPresentOrElse(
                            existing -> {
                                existing.updateFileUrl(sttFileUrl);
                                meetingSttFileRepository.save(existing);
                            },
                            () -> meetingSttFileRepository.save(MeetingSttFile.builder()
                                    .meetingId(meetingId)
                                    .trackType(MeetingTextTrackType.MIXED)
                                    .fileUrl(sttFileUrl)
                                    .build())
                    );
            meeting.updateSttStatus(SttStatus.DONE);
        }

        // 2. 요약 텍스트 저장
        MeetingSttSummary saved;
        if (summaryText != null && !summaryText.isBlank()) {
            String summaryFileUrl = localFileStorageService.saveMeetingTextContent(
                    meetingId, null, true, "summary.txt", summaryText);
            saved = meetingSttSummaryRepository
                    .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                    .map(existing -> {
                        existing.updateFileUrl(summaryFileUrl);
                        return meetingSttSummaryRepository.save(existing);
                    })
                    .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                            .meetingId(meetingId)
                            .trackType(MeetingTextTrackType.MIXED)
                            .fileUrl(summaryFileUrl)
                            .build()));
        } else {
            saved = meetingSttSummaryRepository
                    .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                    .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                            .meetingId(meetingId)
                            .trackType(MeetingTextTrackType.MIXED)
                            .fileUrl("")
                            .build()));
        }

        // 3. 미팅 상태 업데이트
        meeting.updateSummaryStatus(SummaryStatus.DONE);

        return toSttSummaryResponse(saved);
    }

    private void validateTextTrack(MeetingTextTrackType trackType, Long userId) {
        if (trackType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRACK_TYPE_REQUIRED");
        }
        if (trackType == MeetingTextTrackType.INDIVIDUAL && userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        if (trackType == MeetingTextTrackType.MIXED && userId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_NOT_ALLOWED");
        }
    }

    private MeetingSttFileResponse toSttFileResponse(MeetingSttFile file) {
        return new MeetingSttFileResponse(
                file.getId(),
                file.getMeetingId(),
                file.getUserId(),
                file.getTrackType(),
                file.getFileUrl(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    private MeetingSttSummaryResponse toSttSummaryResponse(MeetingSttSummary file) {
        return new MeetingSttSummaryResponse(
                file.getId(),
                file.getMeetingId(),
                file.getUserId(),
                file.getTrackType(),
                file.getFileUrl(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }
}
