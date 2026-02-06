package com.ssafy.domain.notification.service;

import com.ssafy.common.exception.NotificationException;
import com.ssafy.domain.notification.dto.request.FcmTokenDeleteRequest;
import com.ssafy.domain.notification.dto.request.FcmTokenRequest;
import com.ssafy.domain.notification.entity.FcmToken;
import com.ssafy.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * FCM 토큰 등록
     * - 이미 존재하는 토큰이면 갱신
     * - 새 토큰이면 생성
     */
    @Transactional
    public void registerToken(Long userId, FcmTokenRequest request) {
// 기존 토큰 확인
        Optional<FcmToken> existingToken = fcmTokenRepository.findByToken(request.getToken());

        if (existingToken.isPresent()) {
            FcmToken token = existingToken.get();
            if (token.getUserId().equals(userId)) {
                // 같은 사용자의 토큰 -> 활성화
                token.activate();
} else {
                // 다른 사용자의 토큰 -> 기존 토큰 비활성화 후 새로 등록
                token.deactivate();
                FcmToken newToken = request.toEntity(userId);
                fcmTokenRepository.save(newToken);
}
        } else {
            // 새 토큰 등록
            FcmToken newToken = request.toEntity(userId);
            fcmTokenRepository.save(newToken);
}
    }

    /**
     * FCM 토큰 삭제
     */
    @Transactional
    public void deleteToken(Long userId, FcmTokenDeleteRequest request) {
        FcmToken token = fcmTokenRepository.findByUserIdAndToken(userId, request.getToken())
                .orElseThrow(NotificationException.FcmTokenNotFoundException::new);

        fcmTokenRepository.delete(token);

}

    /**
     * 사용자의 활성화된 FCM 토큰 목록 조회 (푸시 발송용)
     */
    public List<FcmToken> getActiveTokens(Long userId) {
        return fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * 사용자의 모든 FCM 토큰 비활성화 (로그아웃 시)
     */
    @Transactional
    public void deactivateAllTokens(Long userId) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        tokens.forEach(FcmToken::deactivate);

}

    /**
     * 사용자의 모든 FCM 토큰 삭제 (회원탈퇴 시)
     */
    @Transactional
    public void deleteAllTokens(Long userId) {
        fcmTokenRepository.deleteByUserId(userId);

}
}
