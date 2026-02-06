package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.QuizContest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizContestRepository extends JpaRepository<QuizContest, Long> {
}
