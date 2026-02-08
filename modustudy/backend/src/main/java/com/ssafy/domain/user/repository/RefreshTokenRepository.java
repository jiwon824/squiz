package com.ssafy.domain.user.repository;

import com.ssafy.domain.user.entity.RefreshToken;
import com.ssafy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    List<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
