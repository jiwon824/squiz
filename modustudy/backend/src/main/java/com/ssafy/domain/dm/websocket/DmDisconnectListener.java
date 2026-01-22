package com.ssafy.domain.dm.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * DM WebSocket 연결 해제 리스너
 * 세션 종료 시 DmSessionService에서 세션 정보 제거
 */
@Component
public class DmDisconnectListener {

    private static final Logger log = LoggerFactory.getLogger(DmDisconnectListener.class);

    private final DmSessionService dmSessionService;

    public DmDisconnectListener(DmSessionService dmSessionService) {
        this.dmSessionService = dmSessionService;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = dmSessionService.unregisterSession(sessionId);

        if (userId != null) {
            log.info("DM disconnected: userId={}, sessionId={}", userId, sessionId);

            // 해당 유저의 모든 세션이 종료되면 오프라인 상태
            if (!dmSessionService.isUserOnline(userId)) {
                log.debug("User {} is now offline", userId);
                // 필요시 친구들에게 오프라인 상태 브로드캐스트 가능
            }
        }
    }
}
