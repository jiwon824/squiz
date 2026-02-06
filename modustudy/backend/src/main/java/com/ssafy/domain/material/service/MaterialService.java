package com.ssafy.domain.material.service;

import com.ssafy.common.exception.MaterialException;
import com.ssafy.domain.material.dto.request.MaterialCreateRequest;
import com.ssafy.domain.material.dto.request.MaterialSearchCondition;
import com.ssafy.domain.material.dto.request.MaterialUpdateRequest;
import com.ssafy.domain.material.dto.response.MaterialCreateResponse;
import com.ssafy.domain.material.dto.response.MaterialDetailResponse;
import com.ssafy.domain.material.dto.response.MaterialListResponse;
import com.ssafy.domain.material.dto.response.UploaderInfo;
import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import com.ssafy.domain.material.repository.MaterialCommentRepository;
import com.ssafy.domain.material.repository.MaterialRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.gamification.event.MaterialUploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 자료 서비스
 */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 @Transactional(readOnly = true)
 public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 자료 목록 조회 (동적 검색)
     */
    public Page<MaterialListResponse> getMaterials(Long studyId, MaterialSearchCondition condition, Pageable pageable) {
        Page<Material> materials = materialRepository.searchMaterials(
                studyId,
                condition.getWeekNumber(),
                condition.getType(),
                condition.getKeyword(),
                pageable
        );

        return materials.map(material -> {
            UploaderInfo uploaderInfo = getUploaderInfo(material.getUploaderId());
            Long commentCount = commentRepository.countByMaterialId(material.getId());
            return MaterialListResponse.from(material, uploaderInfo, commentCount);
        });
    }

    /**
     * 자료 상세 조회
     */
    @Transactional
    public MaterialDetailResponse getMaterialDetail(Long studyId, Long materialId) {
        Material material = materialRepository.findByIdAndStudyId(materialId, studyId)
                .orElseThrow(() -> new MaterialException.MaterialNotFoundException(materialId));

        // 조회수 증가
        material.incrementViewCount();

        UploaderInfo uploaderInfo = getUploaderInfo(material.getUploaderId());
        return MaterialDetailResponse.from(material, uploaderInfo);
    }

    /**
     * 링크 자료 생성
     */
    @Transactional
    public MaterialCreateResponse createLinkMaterial(Long studyId, Long userId, MaterialCreateRequest request) {
// 링크 타입 검증
        if (request.getMaterialType() != MaterialType.LINK) {
            throw new MaterialException.InvalidFileTypeException("링크 자료만 생성 가능합니다");
        }

        Material material = Material.createLinkMaterial(
                studyId,
                userId,
                request.getTitle(),
                request.getDescription(),
                request.getUrl(),
                request.getWeekNumber()
        );

        Material saved = materialRepository.save(material);
// 게이미피케이션 이벤트 발행 - 자료 업로드
        Study study = studyRepository.findById(studyId).orElse(null);
        String studyName = study != null ? study.getName() : "";
        eventPublisher.publishEvent(new MaterialUploadEvent(
                userId,
                studyId,
                studyName,
                saved.getId(),
                saved.getTitle(),
                LocalDate.now()
        ));

        return MaterialCreateResponse.from(saved);
    }

    /**
     * 파일 자료 생성 (파일 저장은 FileStorageService에서 처리 후 호출)
     */
    @Transactional
    public MaterialCreateResponse createFileMaterial(Long studyId, Long userId, String title, String description,
                                                     MaterialType materialType, String filePath, String fileName,
                                                     Long fileSize, Integer weekNumber) {
                                                         Material material = Material.createFileMaterial(
                studyId,
                userId,
                title,
                description,
                materialType,
                filePath,
                fileName,
                fileSize,
                weekNumber
        );

        Material saved = materialRepository.save(material);
// 게이미피케이션 이벤트 발행 - 자료 업로드
        Study study = studyRepository.findById(studyId).orElse(null);
        String studyName = study != null ? study.getName() : "";
        eventPublisher.publishEvent(new MaterialUploadEvent(
                userId,
                studyId,
                studyName,
                saved.getId(),
                saved.getTitle(),
                LocalDate.now()
        ));

        return MaterialCreateResponse.from(saved);
    }

    /**
     * 자료 수정
     */
    @Transactional
    public void updateMaterial(Long studyId, Long materialId, Long userId, MaterialUpdateRequest request) {
        Material material = materialRepository.findByIdAndStudyId(materialId, studyId)
                .orElseThrow(() -> new MaterialException.MaterialNotFoundException(materialId));

        // 본인 확인
        if (!material.isUploader(userId)) {
            throw new MaterialException.NotMaterialOwnerException();
        }

        material.update(request.getTitle(), request.getDescription(), request.getWeekNumber());
}

    /**
     * 자료 삭제
     */
    @Transactional
    public void deleteMaterial(Long studyId, Long materialId, Long userId, boolean isLeader) {
        Material material = materialRepository.findByIdAndStudyId(materialId, studyId)
                .orElseThrow(() -> new MaterialException.MaterialNotFoundException(materialId));

        // 본인 또는 스터디장만 삭제 가능
        if (!material.isUploader(userId) && !isLeader) {
            throw new MaterialException.NotMaterialOwnerException();
        }

        // 댓글 먼저 삭제
        commentRepository.deleteByMaterialId(materialId);

        // 자료 삭제
        materialRepository.delete(material);
}

    /**
     * 업로더 정보 조회
     */
    private UploaderInfo getUploaderInfo(Long userId) {
        return userRepository.findById(userId)
                .map(user -> UploaderInfo.of(user.getId(), user.getNickname(), user.getProfileImage()))
                .orElse(UploaderInfo.of(userId, "알 수 없음", null));
    }
}
