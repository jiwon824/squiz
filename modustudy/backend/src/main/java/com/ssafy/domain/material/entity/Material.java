package com.ssafy.domain.material.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 스터디 ID
     */
    @Column(name = "study_id", nullable = false)
    private Long studyId;

    /**
     * 업로더 ID
     */
    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    /**
     * 자료 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 자료 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 자료 타입 (LINK, FILE, IMAGE, VIDEO)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false)
    private MaterialType materialType;

    /**
     * 외부 링크 URL (LINK 타입인 경우)
     */
    @Column(length = 500)
    private String url;

    /**
     * 저장된 파일 경로 (FILE, IMAGE, VIDEO 타입인 경우)
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * 원본 파일명
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 파일 크기 (bytes)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 커리큘럼 주차
     */
    @Column(name = "week_number")
    private Integer weekNumber;

    /**
     * 조회수
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============================================================
    // 비즈니스 로직
    // ============================================================

    /**
     * 자료 정보 수정 (제목, 설명, 주차)
     */
    public void update(String title, String description, Integer weekNumber) {
        this.title = title;
        this.description = description;
        this.weekNumber = weekNumber;
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 본인이 업로드한 자료인지 확인
     */
    public boolean isUploader(Long userId) {
        return this.uploaderId.equals(userId);
    }

    /**
     * 링크 타입 자료인지 확인
     */
    public boolean isLinkType() {
        return this.materialType == MaterialType.LINK;
    }

    /**
     * 파일 타입 자료인지 확인 (FILE, IMAGE, VIDEO)
     */
    public boolean isFileType() {
        return this.materialType == MaterialType.FILE ||
                this.materialType == MaterialType.IMAGE ||
                this.materialType == MaterialType.VIDEO;
    }

    /**
     * 이미지 타입인지 확인
     */
    public boolean isImageType() {
        return this.materialType == MaterialType.IMAGE;
    }

    /**
     * 동영상 타입인지 확인
     */
    public boolean isVideoType() {
        return this.materialType == MaterialType.VIDEO;
    }

    // ============================================================
    // 정적 팩토리 메서드
    // ============================================================

    /**
     * 링크 자료 생성
     */
    public static Material createLinkMaterial(Long studyId, Long uploaderId,
                                              String title, String description,
                                              String url, Integer weekNumber) {
        return Material.builder()
                .studyId(studyId)
                .uploaderId(uploaderId)
                .title(title)
                .description(description)
                .materialType(MaterialType.LINK)
                .url(url)
                .weekNumber(weekNumber)
                .build();
    }

    /**
     * 파일 자료 생성
     */
    public static Material createFileMaterial(Long studyId, Long uploaderId,
                                              String title, String description,
                                              MaterialType materialType,
                                              String filePath, String fileName,
                                              Long fileSize, Integer weekNumber) {
        return Material.builder()
                .studyId(studyId)
                .uploaderId(uploaderId)
                .title(title)
                .description(description)
                .materialType(materialType)
                .filePath(filePath)
                .fileName(fileName)
                .fileSize(fileSize)
                .weekNumber(weekNumber)
                .build();
    }
}
