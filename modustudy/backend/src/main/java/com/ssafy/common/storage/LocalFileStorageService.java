package com.ssafy.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final Path basePath;

    public LocalFileStorageService(@Value("${app.storage.base-path:./uploads}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    public String saveMeetingPhoto(Long meetingId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "photo" : originalName.replace("\\", "_").replace("/", "_");
        String extension = extractExtension(safeName);
        String timestamp = LocalDateTime.now().format(FILE_TS);
        String filename = timestamp + "-" + UUID.randomUUID() + extension;
        Path targetDir = basePath.resolve("meetings").resolve(String.valueOf(meetingId)).normalize();
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        return "/uploads/meetings/" + meetingId + "/" + filename;
    }

    public String saveMeetingRecordingVideo(Long meetingId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "recording" : originalName.replace("\\", "_").replace("/", "_");
        String extension = extractExtension(safeName);
        String timestamp = LocalDateTime.now().format(FILE_TS);
        String filename = timestamp + "-" + UUID.randomUUID() + extension;
        Path targetDir = basePath.resolve("meetings")
                .resolve(String.valueOf(meetingId))
                .resolve("recordings")
                .resolve("video")
                .normalize();
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        return "/uploads/meetings/" + meetingId + "/recordings/video/" + filename;
    }

    public String saveMeetingRecordingAudio(Long meetingId, Long userId, boolean mixed, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "audio" : originalName.replace("\\", "_").replace("/", "_");
        String extension = extractExtension(safeName);
        String timestamp = LocalDateTime.now().format(FILE_TS);
        String filename = timestamp + "-" + UUID.randomUUID() + extension;
        Path targetDir = basePath.resolve("meetings")
                .resolve(String.valueOf(meetingId))
                .resolve("recordings")
                .resolve("audio")
                .normalize();
        if (!mixed) {
            targetDir = targetDir.resolve("users").resolve(String.valueOf(userId)).normalize();
        } else {
            targetDir = targetDir.resolve("mixed").normalize();
        }
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        if (mixed) {
            return "/uploads/meetings/" + meetingId + "/recordings/audio/mixed/" + filename;
        }
        return "/uploads/meetings/" + meetingId + "/recordings/audio/users/" + userId + "/" + filename;
    }

    public Path saveMeetingVoiceSegment(Long meetingId, Long userId, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String safeName = originalName == null ? "segment" : originalName.replace("\\", "_").replace("/", "_");
        String extension = extractExtension(safeName);
        if (extension.isBlank()) {
            extension = ".webm";
        }
        String timestamp = LocalDateTime.now().format(FILE_TS);
        String filename = timestamp + "-" + UUID.randomUUID() + extension;
        Path segmentsDir = resolveMeetingVoiceSegmentsDir(meetingId, userId);
        Path targetFile = segmentsDir.resolve(filename).normalize();
        if (!targetFile.startsWith(segmentsDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(segmentsDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        return targetFile;
    }

    public String saveMeetingVoiceFinal(Long meetingId, String filename, MultipartFile file) {
        Path voiceDir = resolveMeetingVoiceDir(meetingId);
        Path targetFile = voiceDir.resolve(filename).normalize();
        if (!targetFile.startsWith(voiceDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(voiceDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        return buildMeetingVoiceUrl(meetingId, filename);
    }

    public Path resolveMeetingVoiceDir(Long meetingId) {
        return basePath.resolve("meetings")
                .resolve(String.valueOf(meetingId))
                .resolve("recordings")
                .resolve("voice")
                .normalize();
    }

    public Path resolveMeetingVoiceSegmentsDir(Long meetingId, Long userId) {
        return resolveMeetingVoiceDir(meetingId)
                .resolve("segments")
                .resolve(String.valueOf(userId))
                .normalize();
    }

    public Path resolveMeetingVoiceFile(Long meetingId, String filename) {
        return resolveMeetingVoiceDir(meetingId).resolve(filename).normalize();
    }

    public String buildMeetingVoiceUrl(Long meetingId, String filename) {
        return "/uploads/meetings/" + meetingId + "/recordings/voice/" + filename;
    }

    public String saveMeetingTextFile(Long meetingId, Long userId, boolean mixed, String filename, MultipartFile file) {
        Path targetDir = basePath.resolve("meetings")
                .resolve(String.valueOf(meetingId))
                .resolve("stt")
                .normalize();
        if (mixed) {
            targetDir = targetDir.resolve("mixed").normalize();
        } else {
            targetDir = targetDir.resolve("users").resolve(String.valueOf(userId)).normalize();
        }
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        if (mixed) {
            return "/uploads/meetings/" + meetingId + "/stt/mixed/" + filename;
        }
        return "/uploads/meetings/" + meetingId + "/stt/users/" + userId + "/" + filename;
    }

    public String saveMeetingTextContent(Long meetingId, Long userId, boolean mixed, String filename, String content) {
        Path targetDir = basePath.resolve("meetings")
                .resolve(String.valueOf(meetingId))
                .resolve("stt")
                .normalize();
        if (mixed) {
            targetDir = targetDir.resolve("mixed").normalize();
        } else {
            targetDir = targetDir.resolve("users").resolve(String.valueOf(userId)).normalize();
        }
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
            Files.writeString(targetFile, content == null ? "" : content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        if (mixed) {
            return "/uploads/meetings/" + meetingId + "/stt/mixed/" + filename;
        }
        return "/uploads/meetings/" + meetingId + "/stt/users/" + userId + "/" + filename;
    }

    public Path getBasePath() {
        return basePath;
    }

    public Path resolveUploadedPath(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return null;
        }
        String relative = fileUrl.substring("/uploads/".length());
        return basePath.resolve(relative).normalize();
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        String ext = filename.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (ext.length() > 10) {
            return "";
        }
        return ext;
    }
}
