package com.ssafy.domain.user.repository;

import com.ssafy.domain.user.entity.UserSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserScheduleRepository extends JpaRepository<UserSchedule, Long> {

    List<UserSchedule> findByUserId(Long userId);

    List<UserSchedule> findByUserIdAndIsAvailableTrue(Long userId);
}
