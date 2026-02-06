package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_question_pool")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestionPool extends BaseEntity {
}
