package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.StudyRecommendAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyRecommendActionRepository extends JpaRepository<StudyRecommendAction, Long> {
}
