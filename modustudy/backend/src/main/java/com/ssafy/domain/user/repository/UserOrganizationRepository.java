package com.ssafy.domain.user.repository;

import com.ssafy.domain.user.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> {

}
