package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizCategory extends BaseEntity {
}
