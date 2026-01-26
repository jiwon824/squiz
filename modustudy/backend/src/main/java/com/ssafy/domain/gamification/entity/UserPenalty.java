package com.ssafy.domain.gamification.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_penalty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPenalty extends BaseEntity {
}
