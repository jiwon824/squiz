package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizParticipant extends BaseEntity {
}
