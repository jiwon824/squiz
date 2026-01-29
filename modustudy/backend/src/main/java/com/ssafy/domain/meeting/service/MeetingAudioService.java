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

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

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

    @Transactional
    public MeetingAudioRecordingResponse concatRecordingAudioSegments(Long studyId, Long meetingId, Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
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
                log.warn("Failed to finalize voice segments. meetingId={} userId={} error={}",
                        meetingId, userId, e.toString());
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
            log.warn("Voice segment read failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
            return;
        }
        if (segments.isEmpty()) {
            return;
        }
        Path voiceDir = localFileStorageService.resolveMeetingVoiceDir(meetingId);
        try {
            Files.createDirectories(voiceDir);
        } catch (IOException e) {
            log.warn("Voice dir create failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
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
            log.warn("Voice concat list failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
            return;
        }
        Path outputPath = localFileStorageService.resolveMeetingVoiceFile(meetingId, helper.buildIndividualVoiceFilename(userId));
        try {
            runFfmpegConcat(concatFile, outputPath);
        } catch (ResponseStatusException e) {
            log.warn("Voice concat ffmpeg failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
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
            log.warn("Voice segment user scan failed. meetingId={} error={}", meetingId, e.toString());
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
                log.warn("FFmpeg failed. label={} meetingId={} exit={} output={}", label, meetingId, exit, output.trim());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FFMPEG_FAILED");
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("FFmpeg error. label={} meetingId={} error={}", label, meetingId, e.toString());
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
}
