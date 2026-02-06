package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_practice_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizPracticeRecord extends BaseEntity {

}
