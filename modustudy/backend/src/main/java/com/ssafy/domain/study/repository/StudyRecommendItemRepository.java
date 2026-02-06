package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyRecommendItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRecommendItemRepository extends JpaRepository<StudyRecommendItem, Long> {
    List<StudyRecommendItem> findByLogIdAndStudyId(Long logId, Long studyId);
}
