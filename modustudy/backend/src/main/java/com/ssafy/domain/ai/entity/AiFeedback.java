package com.ssafy.domain.ai.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFeedback extends BaseEntity {

}
