package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

}
