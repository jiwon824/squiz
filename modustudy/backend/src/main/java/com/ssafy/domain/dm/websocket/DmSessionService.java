package com.ssafy.domain.dm.websocket;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DM WebSocket 세션 관리 서비스
 * 사용자 ID와 WebSocket 세션 ID 매핑 관리
 */
@Service
public class DmSessionService {

    // userId -> Set<sessionId> (한 유저가 여러 기기로 접속 가능)
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    // sessionId -> userId
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();

    /**
     * 사용자 세션 등록
     */
    public void registerSession(Long userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        sessionUsers.put(sessionId, userId);
    }

    /**
     * 사용자 세션 해제
     */
    public Long unregisterSession(String sessionId) {
        Long userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        return userId;
    }

    /**
     * 사용자의 모든 세션 ID 조회
     */
    public Set<String> getSessionIds(Long userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    /**
     * 세션 ID로 사용자 ID 조회
     */
    public Long getUserId(String sessionId) {
        return sessionUsers.get(sessionId);
    }

    /**
     * 사용자 온라인 여부 확인
     */
    public boolean isUserOnline(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 온라인 사용자 수
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
}
