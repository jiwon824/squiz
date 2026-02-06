package com.ssafy.domain.notification.repository;

import com.ssafy.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 사용자별 활성화된 FCM 토큰 목록 조회
    List<FcmToken> findByUserIdAndIsActiveTrue(Long userId);

    // 사용자별 FCM 토큰 전체 조회
    List<FcmToken> findByUserId(Long userId);

    // 토큰으로 조회
    Optional<FcmToken> findByToken(String token);

    // 사용자 + 토큰으로 조회
    Optional<FcmToken> findByUserIdAndToken(Long userId, String token);

    // 토큰 삭제
    void deleteByToken(String token);

    // 사용자별 토큰 전체 삭제
    void deleteByUserId(Long userId);

    // 토큰 존재 여부 확인
    boolean existsByToken(String token);
}
