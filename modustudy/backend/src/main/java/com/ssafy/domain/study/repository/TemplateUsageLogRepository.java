package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.TemplateUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateUsageLogRepository extends JpaRepository<TemplateUsageLog, Long> {

    List<TemplateUsageLog> findByUserId(Long userId);

    List<TemplateUsageLog> findByTemplateId(Long templateId);

    long countByTemplateId(Long templateId);

    @Query("SELECT COUNT(t) FROM TemplateUsageLog t WHERE t.templateId = :templateId AND t.usedAsIs = true")
    long countByTemplateIdAndUsedAsIsTrue(@Param("templateId") Long templateId);
}
