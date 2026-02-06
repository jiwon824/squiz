package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.request.TemplateUsageLogRequest;
import com.ssafy.domain.study.dto.response.TemplateUsageLogResponse;
import com.ssafy.domain.study.entity.TemplateUsageLog;
import com.ssafy.domain.study.repository.StudyTemplateRepository;
import com.ssafy.domain.study.repository.TemplateUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateUsageLogService {

    private final TemplateUsageLogRepository templateUsageLogRepository;
    private final StudyTemplateRepository studyTemplateRepository;

    /**
     * 템플릿 사용 로그 저장
     */
    @Transactional
    public TemplateUsageLogResponse logUsage(TemplateUsageLogRequest request, Long userId,
                                              Map<String, Object> userTechStack,
                                              Map<String, Object> userSchedule) {
// 템플릿 존재 확인
        if (!studyTemplateRepository.existsById(request.getTemplateId())) {
            throw new IllegalArgumentException("존재하지 않는 템플릿입니다.");
        }

        TemplateUsageLog usageLog = TemplateUsageLog.builder()
                .userId(userId)
                .templateId(request.getTemplateId())
                .studyId(request.getStudyId())
                .usedAsIs(request.isUsedAsIs())
                .modifications(request.getModifications())
                .userTechStack(userTechStack)
                .userSchedule(userSchedule)
                .build();

        TemplateUsageLog saved = templateUsageLogRepository.save(usageLog);
        return TemplateUsageLogResponse.from(saved);
    }

    /**
     * 템플릿별 사용 통계
     */
    public Map<String, Object> getTemplateStats(Long templateId) {
        long totalUsage = templateUsageLogRepository.countByTemplateId(templateId);
        long usedAsIsCount = templateUsageLogRepository.countByTemplateIdAndUsedAsIsTrue(templateId);

        return Map.of(
                "templateId", templateId,
                "totalUsage", totalUsage,
                "usedAsIsCount", usedAsIsCount,
                "modifiedCount", totalUsage - usedAsIsCount,
                "usedAsIsRate", totalUsage > 0 ? (double) usedAsIsCount / totalUsage : 0.0
        );
    }

    /**
     * 사용자별 사용 로그 조회
     */
    public List<TemplateUsageLogResponse> getUserLogs(Long userId) {
        return templateUsageLogRepository.findByUserId(userId).stream()
                .map(TemplateUsageLogResponse::from)
                .collect(Collectors.toList());
    }
}

