package com.ssafy.domain.gamification.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "level_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelConfig extends BaseEntity {

}
