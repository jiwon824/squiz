package com.ssafy.domain.study.entity;

import com.ssafy.domain.study.converter.JsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 템플릿 사용 로그 엔티티
 * 사용자의 템플릿 사용 패턴을 수집하여 AI 파인튜닝에 활용
 */
@Entity
@Table(name = "template_usage_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "study_id")
    private Long studyId;

    /**
     * 템플릿을 수정 없이 그대로 사용했는지 여부
     */
    @Column(name = "used_as_is")
    @Builder.Default
    private boolean usedAsIs = false;

    /**
     * 수정된 필드 목록 (JSON)
     * 예: {"difficulty": "ADVANCED", "textbook": "백준 골드"}
     */
    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> modifications;

    /**
     * 추천 시점의 사용자 기술스택 스냅샷 (JSON)
     */
    @Convert(converter = JsonConverter.class)
    @Column(name = "user_tech_stack", columnDefinition = "JSON")
    private Map<String, Object> userTechStack;

    /**
     * 추천 시점의 사용자 일정 스냅샷 (JSON)
     */
    @Convert(converter = JsonConverter.class)
    @Column(name = "user_schedule", columnDefinition = "JSON")
    private Map<String, Object> userSchedule;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
