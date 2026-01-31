package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.StudyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyQuizRepository extends JpaRepository<StudyQuiz, Long> {

    /**
     * 스터디별 퀴즈 목록 조회
     */
    List<StudyQuiz> findByStudyIdOrderByCreatedAtDesc(Long studyId);

    /**
     * 미팅 기반 퀴즈 조회 (중복 생성 방지용)
     */
    Optional<StudyQuiz> findBySourceTypeAndSourceId(StudyQuiz.SourceType sourceType, Long sourceId);

    /**
     * 특정 미팅의 퀴즈 존재 여부 확인
     */
    boolean existsBySourceTypeAndSourceId(StudyQuiz.SourceType sourceType, Long sourceId);
}
