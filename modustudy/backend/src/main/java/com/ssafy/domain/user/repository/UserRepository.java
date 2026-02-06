package com.ssafy.domain.user.repository;

import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;



public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    /**
     * 닉네임 중복 체크
     */
    boolean existsByNickname(String nickname);

    /**
     * 전체 회원 수
     */
    long count();

    /**
     * 오늘 가입한 회원 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    long countTodayNewUsers();

    /**
     * 활성 사용자 수 (현재 온라인 또는 최근 1시간 내 활동)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isOnline = true OR u.lastSeenAt > :oneHourAgo")
    long countActiveUsers(@Param("oneHourAgo") LocalDateTime oneHourAgo);

    long countByIsActive(Boolean isActive);
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 닉네임으로 사용자 검색 (검색 허용 + 본인 제외)
     */
    List<User> findByNicknameContainingAndIsSearchableTrueAndIdNot(String nickname, Long excludeId);

    List<User> findAllByRole(Role role);

    /**
     * 사용자 온라인 상태 업데이트
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isOnline = :isOnline, u.lastSeenAt = :lastSeenAt WHERE u.id = :userId")
    void updateOnlineStatus(@Param("userId") Long userId, @Param("isOnline") Boolean isOnline, @Param("lastSeenAt") LocalDateTime lastSeenAt);
}
