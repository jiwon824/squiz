package com.ssafy.domain.material.repository;

import com.ssafy.domain.material.entity.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
}
