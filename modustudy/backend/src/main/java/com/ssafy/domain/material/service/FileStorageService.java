package com.ssafy.domain.material.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 서비스 인터페이스
 */
 public interface FileStorageService {

    /**
     * 파일 업로드
     *
     * @param file 업로드할 파일
     * @param directory 저장 디렉토리 (예: "materials", "profiles")
     * @return 저장된 파일 정보
     */
    FileUploadResult upload(MultipartFile file, String directory);

    /**
     * 파일 삭제
     *
     * @param filePath 삭제할 파일 경로
     */
    void delete(String filePath);

    /**
     * 파일 URL 생성
     *
     * @param filePath 파일 경로
     * @return 접근 가능한 URL
     */
    String getFileUrl(String filePath);

    /**
     * 파일 업로드 결과
     */
    record FileUploadResult(
            String filePath,    // 저장된 파일 경로
            String fileName,    // 원본 파일명
            Long fileSize,      // 파일 크기 (bytes)
            String contentType  // MIME 타입
    ) {}
}
