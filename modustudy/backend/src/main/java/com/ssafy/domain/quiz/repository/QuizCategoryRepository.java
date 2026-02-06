package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.QuizCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizCategoryRepository extends JpaRepository<QuizCategory, Long> {

}
