package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.QuizCourseSection;
import com.ssafy.domain.quiz.entity.QuizCourseSectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Quiz course section repository.
 *
 * Uses composite PK: (quizCourseId, sectionNumber)
 */
public interface QuizCourseSectionRepository extends JpaRepository<QuizCourseSection, QuizCourseSectionId> {

        /**
         * Find section by course ID and section number with course and questions.
         * Uses fetch join to prevent N+1.
         */
        @Query("SELECT s FROM QuizCourseSection s " +
                        "JOIN FETCH s.course " +
                        "LEFT JOIN FETCH s.questions " +
                        "WHERE s.quizCourseId = :quizCourseId AND s.sectionNumber = :sectionNumber")
        Optional<QuizCourseSection> findByIdWithCourseAndQuestions(
                        @Param("quizCourseId") Long quizCourseId,
                        @Param("sectionNumber") Integer sectionNumber);

        /**
         * Find section by course ID and section number with course only.
         */
        @Query("SELECT s FROM QuizCourseSection s " +
                        "JOIN FETCH s.course " +
                        "WHERE s.quizCourseId = :quizCourseId AND s.sectionNumber = :sectionNumber")
        Optional<QuizCourseSection> findByIdWithCourse(
                        @Param("quizCourseId") Long quizCourseId,
                        @Param("sectionNumber") Integer sectionNumber);

        /**
         * Find section by course ID and section number.
         */
        Optional<QuizCourseSection> findByQuizCourseIdAndSectionNumber(Long quizCourseId, Integer sectionNumber);

        /**
         * Find maximum section number for a course.
         */
        @Query("SELECT MAX(s.sectionNumber) FROM QuizCourseSection s WHERE s.quizCourseId = :quizCourseId")
        Optional<Integer> findMaxSectionNumberByQuizCourseId(@Param("quizCourseId") Long quizCourseId);

        /**
         * Find all sections for a course ordered by section number.
         */
        @Query("SELECT s FROM QuizCourseSection s WHERE s.quizCourseId = :quizCourseId ORDER BY s.sectionNumber")
        List<QuizCourseSection> findAllByQuizCourseIdOrderBySectionNumber(@Param("quizCourseId") Long quizCourseId);
}
