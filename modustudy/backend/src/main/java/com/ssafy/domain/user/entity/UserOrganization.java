package com.ssafy.domain.user.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_organization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOrganization extends BaseEntity {
}
