package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.StudyQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyQuizQuestionRepository extends JpaRepository<StudyQuizQuestion, Long> {
}
