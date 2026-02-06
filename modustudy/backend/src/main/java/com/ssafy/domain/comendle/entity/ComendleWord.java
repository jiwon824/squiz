package com.ssafy.domain.comendle.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comendle_word")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComendleWord extends BaseEntity {

}
