package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.meeting.dto.response.MeetingAudioRecordingResponse;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingParticipant;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MeetingAudioService {

    private static final Logger log = LoggerFactory.getLogger(MeetingAudioService.class);

    private final MeetingAudioRecordingRepository meetingAudioRecordingRepository;
    private final LocalFileStorageService localFileStorageService;
    private final MeetingServiceHelper helper;
    private final StudyDailyUsageService dailyUsageService;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Transactional(readOnly = true)
    public List<MeetingAudioRecordingResponse> getAudioRecordings(Long studyId, Long meetingId,
                                                                  MeetingAudioTrackType trackType,
                                                                  Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        List<MeetingAudioRecording> recordings;
        if (trackType != null && userId != null) {
            recordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtAsc(meetingId, trackType, userId);
        } else if (trackType != null) {
            recordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, trackType);
        } else if (userId != null) {
            recordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndUserIdOrderByCreatedAtAsc(meetingId, userId);
        } else {
            recordings = meetingAudioRecordingRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
        }
        return recordings.stream()
                .map(this::toAudioRecordingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingAudioRecordingResponse> getAudioRecordingsForUser(Long studyId, Long meetingId, Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        List<MeetingAudioRecording> mixed = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);
        List<MeetingAudioRecording> individual = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtAsc(
                        meetingId, MeetingAudioTrackType.INDIVIDUAL, userId);
        return Stream.concat(mixed.stream(), individual.stream())
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toAudioRecordingResponse)
                .toList();
    }

    /**
     * 오프라인 오디오 업로드 (일일 한도 2시간 제한)
     */
    @Transactional
    public MeetingAudioRecordingResponse uploadRecordingAudio(Long studyId, Long meetingId,
                                                              MeetingAudioTrackType trackType,
                                                              Long userId,
                                                              MultipartFile audio) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED");
        }
        if (trackType == MeetingAudioTrackType.INDIVIDUAL && userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        if (trackType == MeetingAudioTrackType.MIXED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIXED_AUDIO_NOT_SUPPORTED");
        }
        // 일일 오프라인 STT 한도 체크 (남은 시간이 0이면 업로드 차단)
        int remainingSeconds = dailyUsageService.getOfflineSttRemainingSeconds(studyId);
        if (remainingSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OFFLINE_STT_DAILY_LIMIT_EXCEEDED");
        }
        String filename = helper.buildIndividualVoiceFilename(userId);
        String recordingUrl = localFileStorageService.saveMeetingVoiceFinal(meetingId, filename, audio);
        String format = helper.extractFileExtension(audio.getOriginalFilename());
        MeetingAudioRecording saved = saveOrUpdateAudioRecording(
                meetingId,
                userId,
                trackType,
                recordingUrl,
                format == null ? "webm" : format,
                audio.getSize()
        );
        // 업로드 후 오디오 길이 추출하여 사용량 기록
        Path audioFile = localFileStorageService.resolveMeetingVoiceFile(meetingId, filename);
        int durationSeconds = getAudioDurationSeconds(audioFile);
        if (durationSeconds > 0) {
            dailyUsageService.addOfflineSttUsage(studyId, durationSeconds);
        }
        return toAudioRecordingResponse(saved);
    }

    @Transactional
    public void uploadRecordingAudioSegment(Long studyId, Long meetingId, Long userId, MultipartFile audio) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED");
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        localFileStorageService.saveMeetingVoiceSegment(meetingId, userId, audio);
    }

    /**
     * 오프라인 오디오 세그먼트 병합 (일일 한도 2시간 제한)
     */
    @Transactional
    public MeetingAudioRecordingResponse concatRecordingAudioSegments(Long studyId, Long meetingId, Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        // 일일 오프라인 STT 한도 체크 (남은 시간이 0이면 병합 차단)
        int remainingSeconds = dailyUsageService.getOfflineSttRemainingSeconds(studyId);
        if (remainingSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OFFLINE_STT_DAILY_LIMIT_EXCEEDED");
        }
        Path segmentsDir = localFileStorageService.resolveMeetingVoiceSegmentsDir(meetingId, userId);
        if (!Files.exists(segmentsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_AUDIO_SEGMENTS");
        }
        List<Path> segments;
        try {
            segments = Files.list(segmentsDir)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIO_SEGMENT_READ_FAILED");
        }
        if (segments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_AUDIO_SEGMENTS");
        }
        Path voiceDir = localFileStorageService.resolveMeetingVoiceDir(meetingId);
        try {
            Files.createDirectories(voiceDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "VOICE_DIR_CREATE_FAILED");
        }
        Path concatFile = voiceDir.resolve("segments-" + userId + ".txt").normalize();
        try {
            String contents = segments.stream()
                    .map(path -> "file '" + path.toAbsolutePath().toString().replace("\\", "/") + "'")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            Files.writeString(concatFile, contents);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIO_CONCAT_LIST_FAILED");
        }
        Path outputPath = localFileStorageService.resolveMeetingVoiceFile(meetingId, helper.buildIndividualVoiceFilename(userId));
        runFfmpegConcat(concatFile, outputPath);
        Long fileSize;
        try {
            fileSize = Files.size(outputPath);
        } catch (IOException e) {
            fileSize = null;
        }
        MeetingAudioRecording saved = saveOrUpdateAudioRecording(
                meetingId,
                userId,
                MeetingAudioTrackType.INDIVIDUAL,
                localFileStorageService.buildMeetingVoiceUrl(meetingId, helper.buildIndividualVoiceFilename(userId)),
                "webm",
                fileSize
        );
        // 병합 후 오디오 길이 추출하여 사용량 기록
        int durationSeconds = getAudioDurationSeconds(outputPath);
        if (durationSeconds > 0) {
            dailyUsageService.addOfflineSttUsage(studyId, durationSeconds);
        }
        cleanupVoiceSegments(segmentsDir, concatFile);
        return toAudioRecordingResponse(saved);
    }

    public void finalizeIndividualVoiceRecordings(Long meetingId, List<MeetingParticipant> participants) {
        Set<Long> userIds = new HashSet<>();
        if (participants != null) {
            participants.stream()
                    .map(MeetingParticipant::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
        }
        userIds.addAll(findVoiceSegmentUserIds(meetingId));
        userIds.forEach((userId) -> {
            try {
                concatVoiceSegmentsIfExists(meetingId, userId);
            } catch (Exception e) {
}
        });
    }

    public void concatVoiceSegmentsIfExists(Long meetingId, Long userId) {
        Path segmentsDir = localFileStorageService.resolveMeetingVoiceSegmentsDir(meetingId, userId);
        if (!Files.exists(segmentsDir)) {
            return;
        }
        List<Path> segments;
        try {
            segments = Files.list(segmentsDir)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return;
        }
        if (segments.isEmpty()) {
            return;
        }
        Path voiceDir = localFileStorageService.resolveMeetingVoiceDir(meetingId);
        try {
            Files.createDirectories(voiceDir);
        } catch (IOException e) {
            return;
        }
        Path concatFile = voiceDir.resolve("segments-" + userId + ".txt").normalize();
        try {
            String contents = segments.stream()
                    .map(path -> "file '" + path.toAbsolutePath().toString().replace("\\", "/") + "'")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            Files.writeString(concatFile, contents);
        } catch (IOException e) {
            return;
        }
        Path outputPath = localFileStorageService.resolveMeetingVoiceFile(meetingId, helper.buildIndividualVoiceFilename(userId));
        try {
            runFfmpegConcat(concatFile, outputPath);
        } catch (ResponseStatusException e) {
            return;
        }
        Long fileSize;
        try {
            fileSize = Files.size(outputPath);
        } catch (IOException e) {
            fileSize = null;
        }
        saveOrUpdateAudioRecording(
                meetingId,
                userId,
                MeetingAudioTrackType.INDIVIDUAL,
                localFileStorageService.buildMeetingVoiceUrl(meetingId, helper.buildIndividualVoiceFilename(userId)),
                "webm",
                fileSize
        );
        cleanupVoiceSegments(segmentsDir, concatFile);
    }

    private List<Long> findVoiceSegmentUserIds(Long meetingId) {
        Path segmentsRoot = localFileStorageService.resolveMeetingVoiceDir(meetingId).resolve("segments");
        if (!Files.exists(segmentsRoot)) {
            return List.of();
        }
        try {
            return Files.list(segmentsRoot)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .map(name -> {
                        try {
                            return Long.parseLong(name);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private MeetingAudioRecording saveOrUpdateAudioRecording(Long meetingId,
                                                             Long userId,
                                                             MeetingAudioTrackType trackType,
                                                             String recordingUrl,
                                                             String format,
                                                             Long fileSize) {
        MeetingAudioRecording existing = userId == null
                ? meetingAudioRecordingRepository
                        .findTopByMeetingIdAndTrackTypeAndUserIdIsNullOrderByCreatedAtDesc(meetingId, trackType)
                        .orElse(null)
                : meetingAudioRecordingRepository
                        .findTopByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtDesc(meetingId, trackType, userId)
                        .orElse(null);
        if (existing != null) {
            existing.updateRecording(recordingUrl, format, fileSize);
            return meetingAudioRecordingRepository.save(existing);
        }
        return meetingAudioRecordingRepository.save(MeetingAudioRecording.builder()
                .meetingId(meetingId)
                .userId(userId)
                .trackType(trackType)
                .recordingUrl(recordingUrl)
                .format(format)
                .fileSize(fileSize)
                .build());
    }

    private MeetingAudioRecordingResponse toAudioRecordingResponse(MeetingAudioRecording saved) {
        return new MeetingAudioRecordingResponse(
                saved.getId(),
                saved.getMeetingId(),
                saved.getUserId(),
                saved.getTrackType(),
                saved.getRecordingUrl(),
                saved.getFormat(),
                saved.getFileSize(),
                saved.getCreatedAt());
    }

    private void runFfmpegConcat(Path concatFile, Path outputPath) {
        List<String> args = List.of(
                ffmpegPath,
                "-y",
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                concatFile.toAbsolutePath().toString(),
                "-c",
                "copy",
                outputPath.toAbsolutePath().toString()
        );
        runFfmpeg(args, "voice-concat", null);
    }

    private void runFfmpeg(List<String> args, String label, Long meetingId) {
        try {
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FFMPEG_FAILED");
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FFMPEG_FAILED");
        }
    }

    private void cleanupVoiceSegments(Path segmentsDir, Path concatFile) {
        try {
            if (Files.exists(concatFile)) {
                Files.deleteIfExists(concatFile);
            }
            if (!Files.exists(segmentsDir)) {
                return;
            }
            try (var stream = Files.list(segmentsDir)) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * ffprobe를 사용해 오디오 파일의 길이(초)를 추출
     * @return 길이(초), 실패 시 0 반환
     */
    private int getAudioDurationSeconds(Path audioFile) {
        if (!Files.exists(audioFile)) {
            return 0;
        }
        try {
            List<String> args = List.of(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    audioFile.toAbsolutePath().toString()
            );
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exit = process.waitFor();
            if (exit != 0) {
                return 0;
            }
            double duration = Double.parseDouble(output);
            return (int) Math.ceil(duration);
        } catch (IOException | InterruptedException | NumberFormatException e) {
            return 0;
        }
    }
}

