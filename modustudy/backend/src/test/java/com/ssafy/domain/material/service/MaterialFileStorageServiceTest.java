package com.ssafy.domain.material.service;

import com.ssafy.common.exception.MaterialException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalFileStorageService 단위 테스트
 */
class MaterialFileStorageServiceTest {

    private MaterialFileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new MaterialFileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "serverPort", "8080");
        ReflectionTestUtils.setField(fileStorageService, "sslEnabled", true);
    }

    @Nested
    @DisplayName("파일 업로드 테스트")
    class UploadTest {

        @Test
        @DisplayName("PDF 파일 업로드 성공")
        void upload_PdfFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test_document.pdf",
                    "application/pdf",
                    "PDF content".getBytes()
            );

            // when
            FileStorageService.FileUploadResult result = fileStorageService.upload(file, "materials");

            // then
            assertThat(result).isNotNull();
            assertThat(result.fileName()).isEqualTo("test_document.pdf");
            assertThat(result.fileSize()).isEqualTo(file.getSize());
            assertThat(result.filePath()).startsWith("materials/");
            assertThat(result.filePath()).endsWith(".pdf");

            // 파일이 실제로 저장되었는지 확인
            Path savedFile = tempDir.resolve(result.filePath());
            assertThat(Files.exists(savedFile)).isTrue();
        }

        @Test
        @DisplayName("이미지 파일 업로드 성공")
        void upload_ImageFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test_image.png",
                    "image/png",
                    "PNG content".getBytes()
            );

            // when
            FileStorageService.FileUploadResult result = fileStorageService.upload(file, "images");

            // then
            assertThat(result.fileName()).isEqualTo("test_image.png");
            assertThat(result.filePath()).endsWith(".png");
            assertThat(result.contentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("다양한 문서 파일 업로드 성공")
        void upload_DocumentFiles_Success() {
            // given
            String[] extensions = {"doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md"};

            for (String ext : extensions) {
                MockMultipartFile file = new MockMultipartFile(
                        "file",
                        "document." + ext,
                        "application/octet-stream",
                        "content".getBytes()
                );

                // when
                FileStorageService.FileUploadResult result = fileStorageService.upload(file, "docs");

                // then
                assertThat(result.filePath()).endsWith("." + ext);
            }
        }

        @Test
        @DisplayName("압축 파일 업로드 성공")
        void upload_ArchiveFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "archive.zip",
                    "application/zip",
                    "ZIP content".getBytes()
            );

            // when
            FileStorageService.FileUploadResult result = fileStorageService.upload(file, "archives");

            // then
            assertThat(result.filePath()).endsWith(".zip");
        }

        @Test
        @DisplayName("영상 파일 업로드 성공")
        void upload_VideoFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "video.mp4",
                    "video/mp4",
                    "MP4 content".getBytes()
            );

            // when
            FileStorageService.FileUploadResult result = fileStorageService.upload(file, "videos");

            // then
            assertThat(result.filePath()).endsWith(".mp4");
        }

        @Test
        @DisplayName("하위 디렉토리에 파일 업로드 성공")
        void upload_SubDirectory_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "content".getBytes()
            );

            // when
            FileStorageService.FileUploadResult result = fileStorageService.upload(file, "materials/study_1");

            // then
            assertThat(result.filePath()).startsWith("materials/study_1/");

            Path savedFile = tempDir.resolve(result.filePath());
            assertThat(Files.exists(savedFile)).isTrue();
        }

        @Test
        @DisplayName("빈 파일 업로드 시 예외 발생")
        void upload_EmptyFile_ThrowsException() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "empty.pdf",
                    "application/pdf",
                    new byte[0]
            );

            // when & then
            assertThatThrownBy(() -> fileStorageService.upload(file, "materials"))
                    .isInstanceOf(MaterialException.FileUploadFailedException.class);
        }

        @Test
        @DisplayName("null 파일 업로드 시 예외 발생")
        void upload_NullFile_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> fileStorageService.upload(null, "materials"))
                    .isInstanceOf(MaterialException.FileUploadFailedException.class);
        }

        @Test
        @DisplayName("파일명 없는 파일 업로드 시 예외 발생")
        void upload_NoFileName_ThrowsException() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "",
                    "application/pdf",
                    "content".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> fileStorageService.upload(file, "materials"))
                    .isInstanceOf(MaterialException.FileUploadFailedException.class);
        }

        @Test
        @DisplayName("지원하지 않는 파일 형식 업로드 시 예외 발생")
        void upload_UnsupportedFileType_ThrowsException() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "application/x-msdownload",
                    "content".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> fileStorageService.upload(file, "materials"))
                    .isInstanceOf(MaterialException.InvalidFileTypeException.class);
        }

        @Test
        @DisplayName("고유한 파일명으로 저장됨")
        void upload_UniqueFileName_Success() {
            // given
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "same_name.pdf", "application/pdf", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "same_name.pdf", "application/pdf", "content2".getBytes());

            // when
            FileStorageService.FileUploadResult result1 = fileStorageService.upload(file1, "materials");
            FileStorageService.FileUploadResult result2 = fileStorageService.upload(file2, "materials");

            // then - 같은 원본 파일명이지만 다른 경로로 저장
            assertThat(result1.filePath()).isNotEqualTo(result2.filePath());
            assertThat(result1.fileName()).isEqualTo(result2.fileName()); // 원본 파일명은 동일
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("파일 삭제 성공")
        void delete_ExistingFile_Success() throws IOException {
            // given
            Path testFile = tempDir.resolve("materials/test.pdf");
            Files.createDirectories(testFile.getParent());
            Files.write(testFile, "content".getBytes());
            assertThat(Files.exists(testFile)).isTrue();

            // when
            fileStorageService.delete("materials/test.pdf");

            // then
            assertThat(Files.exists(testFile)).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 파일 삭제 시 예외 없음")
        void delete_NonExistingFile_NoException() {
            // when & then - 예외 발생하지 않음
            fileStorageService.delete("nonexistent/file.pdf");
        }

        @Test
        @DisplayName("null 경로 삭제 시 예외 없음")
        void delete_NullPath_NoException() {
            // when & then
            fileStorageService.delete(null);
        }

        @Test
        @DisplayName("빈 경로 삭제 시 예외 없음")
        void delete_EmptyPath_NoException() {
            // when & then
            fileStorageService.delete("");
            fileStorageService.delete("   ");
        }
    }

    @Nested
    @DisplayName("파일 URL 생성 테스트")
    class GetFileUrlTest {

        @Test
        @DisplayName("SSL 활성화 시 https URL 생성")
        void getFileUrl_SslEnabled_HttpsUrl() {
            // given
            ReflectionTestUtils.setField(fileStorageService, "sslEnabled", true);

            // when
            String url = fileStorageService.getFileUrl("materials/test.pdf");

            // then
            assertThat(url).isEqualTo("https://localhost:8080/files/materials/test.pdf");
        }

        @Test
        @DisplayName("SSL 비활성화 시 http URL 생성")
        void getFileUrl_SslDisabled_HttpUrl() {
            // given
            ReflectionTestUtils.setField(fileStorageService, "sslEnabled", false);

            // when
            String url = fileStorageService.getFileUrl("materials/test.pdf");

            // then
            assertThat(url).isEqualTo("http://localhost:8080/files/materials/test.pdf");
        }

        @Test
        @DisplayName("null 경로면 null 반환")
        void getFileUrl_NullPath_ReturnsNull() {
            // when
            String url = fileStorageService.getFileUrl(null);

            // then
            assertThat(url).isNull();
        }

        @Test
        @DisplayName("빈 경로면 null 반환")
        void getFileUrl_EmptyPath_ReturnsNull() {
            // when & then
            assertThat(fileStorageService.getFileUrl("")).isNull();
            assertThat(fileStorageService.getFileUrl("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("MaterialType 감지 테스트")
    class DetectMaterialTypeTest {

        @Test
        @DisplayName("이미지 파일 감지")
        void detectMaterialType_ImageFiles() {
            assertThat(MaterialFileStorageService.detectMaterialType("photo.jpg")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("photo.jpeg")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("photo.png")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("photo.gif")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("photo.webp")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("photo.svg")).isEqualTo("IMAGE");
        }

        @Test
        @DisplayName("영상 파일 감지")
        void detectMaterialType_VideoFiles() {
            assertThat(MaterialFileStorageService.detectMaterialType("video.mp4")).isEqualTo("VIDEO");
            assertThat(MaterialFileStorageService.detectMaterialType("video.avi")).isEqualTo("VIDEO");
            assertThat(MaterialFileStorageService.detectMaterialType("video.mov")).isEqualTo("VIDEO");
            assertThat(MaterialFileStorageService.detectMaterialType("video.wmv")).isEqualTo("VIDEO");
        }

        @Test
        @DisplayName("일반 파일 감지")
        void detectMaterialType_RegularFiles() {
            assertThat(MaterialFileStorageService.detectMaterialType("document.pdf")).isEqualTo("FILE");
            assertThat(MaterialFileStorageService.detectMaterialType("document.doc")).isEqualTo("FILE");
            assertThat(MaterialFileStorageService.detectMaterialType("document.docx")).isEqualTo("FILE");
            assertThat(MaterialFileStorageService.detectMaterialType("archive.zip")).isEqualTo("FILE");
        }

        @Test
        @DisplayName("대소문자 구분 없이 감지")
        void detectMaterialType_CaseInsensitive() {
            assertThat(MaterialFileStorageService.detectMaterialType("photo.JPG")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("photo.PNG")).isEqualTo("IMAGE");
            assertThat(MaterialFileStorageService.detectMaterialType("video.MP4")).isEqualTo("VIDEO");
        }
    }
}