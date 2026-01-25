package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.request.CreateTemplateRequest;
import com.ssafy.domain.study.dto.request.UpdateTemplateRequest;
import com.ssafy.domain.study.dto.response.StudyTemplateResponse;
import com.ssafy.domain.study.entity.StudyTemplate;
import com.ssafy.domain.study.repository.StudyTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 스터디 템플릿 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyTemplateService {

    private final StudyTemplateRepository studyTemplateRepository;

    /**
     * 템플릿 생성
     */
    @Transactional
    public StudyTemplateResponse createTemplate(CreateTemplateRequest request, Long userId) {
        log.info("템플릿 생성 시작 - userId: {}, name: {}", userId, request.getName());

        // 이름 중복 체크
        if (studyTemplateRepository.existsByUserIdAndName(userId, request.getName())) {
            log.warn("템플릿 이름 중복 - userId: {}, name: {}", userId, request.getName());
            throw new IllegalStateException("이미 같은 이름의 템플릿이 존재합니다.");
        }

        // Entity 생성
        StudyTemplate template = StudyTemplate.builder()
                .userId(userId)
                .name(request.getName())
                .isSystem(false)
                .templateType(request.getTemplateType())
                .topic(request.getTopic())
                .format(request.getFormat())
                .meetingType(request.getMeetingType())
                .description(request.getDescription())
                .textbook(request.getTextbook())
                .goal(request.getGoal())
                .difficulty(request.getDifficulty())
                .prerequisites(request.getPrerequisites())
                .processDetail(request.getProcessDetail())
                .penaltyPolicy(request.getPenaltyPolicy())
                .build();

        StudyTemplate saved = studyTemplateRepository.save(template);
        log.info("템플릿 생성 완료 - templateId: {}", saved.getId());

        return StudyTemplateResponse.from(saved);
    }

    /**
     * 내 템플릿 목록 조회
     */
    public List<StudyTemplateResponse> getMyTemplates(Long userId) {
        log.info("내 템플릿 목록 조회 - userId: {}", userId);

        List<StudyTemplate> templates = studyTemplateRepository.findByUserId(userId);

        // 최신순 정렬 - 새로운 리스트로 변환 후 정렬
        return templates.stream()
                .sorted(Comparator.comparing(StudyTemplate::getCreatedAt).reversed())
                .map(StudyTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 시스템 템플릿 목록 조회
     */
    public List<StudyTemplateResponse> getSystemTemplates(String templateType) {
        log.info("시스템 템플릿 조회 - templateType: {}", templateType);

        List<StudyTemplate> templates;

        if (templateType != null && !templateType.isEmpty()) {
            templates = studyTemplateRepository.findByIsSystemTrueAndTemplateType(templateType);
        } else {
            templates = studyTemplateRepository.findByIsSystemTrue();
        }

        // 최신순 정렬 - 새로운 리스트로 변환 후 정렬
        return templates.stream()
                .sorted(Comparator.comparing(StudyTemplate::getCreatedAt).reversed())
                .map(StudyTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 템플릿 상세 조회
     */
    public StudyTemplateResponse getTemplate(Long templateId, Long userId) {
        log.info("템플릿 상세 조회 - templateId: {}, userId: {}", templateId, userId);

        StudyTemplate template = studyTemplateRepository.findById(templateId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 템플릿 - templateId: {}", templateId);
                    return new IllegalArgumentException("존재하지 않는 템플릿입니다.");
                });

        // 시스템 템플릿이거나 본인 템플릿인 경우만 조회 가능
        if (!template.isSystem() && !template.getUserId().equals(userId)) {
            log.warn("템플릿 조회 권한 없음 - templateId: {}, userId: {}", templateId, userId);
            throw new IllegalStateException("템플릿 조회 권한이 없습니다.");
        }

        log.info("템플릿 조회 완료 - templateId: {}", templateId);
        return StudyTemplateResponse.from(template);
    }

    /**
     * 템플릿 수정
     */
    @Transactional
    public StudyTemplateResponse updateTemplate(Long templateId, UpdateTemplateRequest request, Long userId) {
        log.info("템플릿 수정 시작 - templateId: {}, userId: {}", templateId, userId);

        // 권한 체크 (본인 템플릿만 수정 가능)
        StudyTemplate template = studyTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> {
                    log.error("템플릿을 찾을 수 없거나 권한 없음 - templateId: {}, userId: {}", templateId, userId);
                    return new IllegalArgumentException("템플릿을 찾을 수 없거나 수정 권한이 없습니다.");
                });

        // 이름 변경 시 중복 체크
        if (request.getName() != null && !request.getName().equals(template.getName())) {
            if (studyTemplateRepository.existsByUserIdAndName(userId, request.getName())) {
                log.warn("템플릿 이름 중복 - userId: {}, name: {}", userId, request.getName());
                throw new IllegalStateException("이미 같은 이름의 템플릿이 존재합니다.");
            }
            template.setName(request.getName());
        }

        // 필드 업데이트 (null이 아닌 경우만)
        if (request.getTemplateType() != null) template.setTemplateType(request.getTemplateType());
        if (request.getTopic() != null) template.setTopic(request.getTopic());
        if (request.getFormat() != null) template.setFormat(request.getFormat());
        if (request.getMeetingType() != null) template.setMeetingType(request.getMeetingType());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getTextbook() != null) template.setTextbook(request.getTextbook());
        if (request.getGoal() != null) template.setGoal(request.getGoal());
        if (request.getDifficulty() != null) template.setDifficulty(request.getDifficulty());
        if (request.getPrerequisites() != null) template.setPrerequisites(request.getPrerequisites());
        if (request.getProcessDetail() != null) template.setProcessDetail(request.getProcessDetail());
        if (request.getPenaltyPolicy() != null) template.setPenaltyPolicy(request.getPenaltyPolicy());

        StudyTemplate updated = studyTemplateRepository.save(template);
        log.info("템플릿 수정 완료 - templateId: {}", templateId);

        return StudyTemplateResponse.from(updated);
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public void deleteTemplate(Long templateId, Long userId) {
        log.info("템플릿 삭제 시작 - templateId: {}, userId: {}", templateId, userId);

        // 권한 체크 (본인 템플릿만 삭제 가능)
        StudyTemplate template = studyTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> {
                    log.error("템플릿을 찾을 수 없거나 권한 없음 - templateId: {}, userId: {}", templateId, userId);
                    return new IllegalArgumentException("템플릿을 찾을 수 없거나 삭제 권한이 없습니다.");
                });

        studyTemplateRepository.delete(template);
        log.info("템플릿 삭제 완료 - templateId: {}", templateId);
    }
}