package com.ssafy.domain.study.workspace.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 워크스페이스 WebSocket 연결 해제 리스너
 * 사용자가 비정상적으로 연결이 끊어졌을 때 세션 정리
 */
 @Component
 public class WorkspaceDisconnectListener {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceDisconnectListener.class);

    private final WorkspaceSessionService sessionService;

    public WorkspaceDisconnectListener(WorkspaceSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        // 워크스페이스 세션 정리
        Long workspaceId = sessionService.getWorkspaceId(sessionId);
        Long userId = sessionService.getUserId(sessionId);

        if (workspaceId != null && userId != null) {
            sessionService.leaveWorkspace(sessionId);
}
    }
}

