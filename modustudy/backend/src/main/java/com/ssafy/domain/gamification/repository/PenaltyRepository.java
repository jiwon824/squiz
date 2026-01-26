package com.ssafy.domain.gamification.repository;

import com.ssafy.domain.gamification.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    Optional<Penalty> findByCode(String code);
}