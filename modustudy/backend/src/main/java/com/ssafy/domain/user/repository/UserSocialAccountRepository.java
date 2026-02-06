package com.ssafy.domain.user.repository;

import com.ssafy.domain.user.entity.SocialProvider;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.entity.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    Optional<UserSocialAccount> findByUserAndProvider(User user, SocialProvider provider);

    Optional<UserSocialAccount> findByUserIdAndProvider(Long userId, SocialProvider provider);

    List<UserSocialAccount> findByUser(User user);

    List<UserSocialAccount> findByUserId(Long userId);

    boolean existsByUserAndProvider(User user, SocialProvider provider);

    boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    // 특정 사용자의 연동된 소셜 계정 개수
    long countByUserId(Long userId);
}
