package com.ssafy.domain.material.service;

import com.ssafy.common.exception.MaterialException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 로컬 파일 저장 서비스 구현체
 */
@Slf4j
@Service
public class MaterialFileStorageService implements FileStorageService {

    @Value("${app.storage.base-path:./uploads}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * 허용된 파일 확장자
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 문서
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md",
            // 이미지
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            // 압축
            "zip", "rar", "7z",
            // 영상
            "mp4", "avi", "mov", "wmv"
    );

    /**
     * 이미지 확장자
     */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    /**
     * 영상 확장자
     */
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "avi", "mov", "wmv"
    );

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("업로드 디렉토리 생성 실패", e);
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다.", e);
        }
    }

    @Override
    public FileUploadResult upload(MultipartFile file, String directory) {
        // 1. 파일 유효성 검사 (null 체크 포함)
        validateFile(file);

        log.info("파일 업로드 시작 - 원본파일명: {}, 크기: {}, 디렉토리: {}",
                file.getOriginalFilename(), file.getSize(), directory);

        // 2. 저장 디렉토리 생성
        Path targetDir = Paths.get(uploadDir, directory);
        createDirectoryIfNotExists(targetDir);

        // 3. 고유 파일명 생성
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + "." + extension;

        // 4. 파일 저장
        Path targetPath = targetDir.resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 저장 완료: {}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new MaterialException.FileUploadFailedException("파일 저장에 실패했습니다.");
        }

        // 5. 상대 경로 반환 (DB 저장용)
        String relativePath = directory + "/" + storedFileName;

        return new FileUploadResult(
                relativePath,
                originalFileName,
                file.getSize(),
                file.getContentType()
        );
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(uploadDir, filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("파일 삭제 완료: {}", path);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", filePath, e);
            // 파일 삭제 실패는 예외를 던지지 않음 (DB 데이터는 삭제됨)
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        String protocol = sslEnabled ? "https" : "http";
        String baseUrl = protocol + "://localhost:" + serverPort + "/files";
        return baseUrl + "/" + filePath;
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        // 빈 파일 체크
        if (file == null || file.isEmpty()) {
            throw new MaterialException.FileUploadFailedException("파일이 비어있습니다.");
        }

        // 파일명 체크
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new MaterialException.FileUploadFailedException("파일명이 없습니다.");
        }

        // 확장자 체크
        String extension = getExtension(fileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new MaterialException.InvalidFileTypeException(
                    "지원하지 않는 파일 형식입니다: " + extension);
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 디렉토리 생성
     */
    private void createDirectoryIfNotExists(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("디렉토리 생성 실패: {}", path, e);
            throw new MaterialException.FileUploadFailedException("저장 디렉토리를 생성할 수 없습니다.");
        }
    }

    /**
     * 확장자로 MaterialType 결정
     */
    public static String detectMaterialType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "IMAGE";
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            return "VIDEO";
        } else {
            return "FILE";
        }
    }
}