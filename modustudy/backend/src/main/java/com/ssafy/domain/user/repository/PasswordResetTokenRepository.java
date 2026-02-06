package com.ssafy.domain.user.repository;

import com.ssafy.domain.user.entity.PasswordResetToken;
import com.ssafy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // 특정 사용자의 미사용 토큰 찾기
    Optional<PasswordResetToken> findByUserAndUsedFalse(User user);

    // 만료된 토큰 삭제 (배치 작업용)
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
