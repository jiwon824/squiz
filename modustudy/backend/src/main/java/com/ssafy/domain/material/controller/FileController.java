package com.ssafy.domain.material.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 파일 다운로드 컨트롤러
 * 정적 파일 서빙용
 */
 @Slf4j
 @RestController
 @RequestMapping("/files")
 public class FileController {

    @Value("${app.storage.base-path:./uploads}")
    private String uploadDir;

    /**
     * 파일 다운로드/조회
     * GET /files/{directory}/{filename}
     * 예: /files/materials/study_1/abc123.pdf
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> downloadFile(@RequestParam(value = "download", defaultValue = "false") boolean download,
                                                 jakarta.servlet.http.HttpServletRequest request) {

        // URL에서 /files/ 이후 경로 추출
        String filePath = request.getRequestURI().substring("/files/".length());

        try {
            Path path = Paths.get(uploadDir).resolve(filePath).normalize();

            // 보안: uploadDir 외부 접근 방지
            if (!path.startsWith(Paths.get(uploadDir).normalize())) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Content-Type 결정
            String contentType;
            try {
                contentType = Files.probeContentType(path);
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 파일명 추출 (다운로드 시 사용)
            String fileName = path.getFileName().toString();

            // 응답 빌더
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType));

            // 다운로드 모드면 Content-Disposition 헤더 추가
            if (download) {
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");
                responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"");
            }

            return responseBuilder.body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
