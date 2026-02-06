package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

}
