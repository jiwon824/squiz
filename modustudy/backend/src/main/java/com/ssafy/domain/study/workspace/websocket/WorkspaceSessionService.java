package com.ssafy.domain.study.workspace.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 워크스페이스 WebSocket 세션 관리 서비스
 * workspaceId -> 접속 중인 사용자 목록 관리
 */
 @Service
 public class WorkspaceSessionService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSessionService.class);

    // workspaceId -> Set<userId> (워크스페이스에 접속 중인 사용자 목록)
    private final Map<Long, Set<Long>> workspaceUsers = new ConcurrentHashMap<>();

    // userId -> Set<workspaceId> (사용자가 접속 중인 워크스페이스 목록)
    private final Map<Long, Set<Long>> userWorkspaces = new ConcurrentHashMap<>();

    // sessionId -> userId (세션 ID와 사용자 ID 매핑)
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();

    // sessionId -> workspaceId (세션이 연결된 워크스페이스)
    private final Map<String, Long> sessionWorkspaces = new ConcurrentHashMap<>();

    /**
     * 워크스페이스에 사용자 세션 등록
     */
    public void joinWorkspace(Long workspaceId, Long userId, String sessionId) {
        // 워크스페이스에 사용자 추가
        workspaceUsers.computeIfAbsent(workspaceId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);

        // 사용자의 워크스페이스 목록에 추가
        userWorkspaces.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(workspaceId);

        // 세션 매핑 저장
        sessionUsers.put(sessionId, userId);
        sessionWorkspaces.put(sessionId, workspaceId);

}

    /**
     * 워크스페이스에서 사용자 세션 해제
     */
    public void leaveWorkspace(String sessionId) {
        Long userId = sessionUsers.remove(sessionId);
        Long workspaceId = sessionWorkspaces.remove(sessionId);

        if (userId != null && workspaceId != null) {
            // 워크스페이스에서 사용자 제거
            Set<Long> users = workspaceUsers.get(workspaceId);
            if (users != null) {
                users.remove(userId);
                if (users.isEmpty()) {
                    workspaceUsers.remove(workspaceId);
                }
            }

            // 사용자의 워크스페이스 목록에서 제거
            Set<Long> workspaces = userWorkspaces.get(userId);
            if (workspaces != null) {
                workspaces.remove(workspaceId);
                if (workspaces.isEmpty()) {
                    userWorkspaces.remove(userId);
                }
            }

}
    }

    /**
     * 특정 워크스페이스에 접속 중인 모든 사용자 ID 조회
     */
    public Set<Long> getWorkspaceUsers(Long workspaceId) {
        return workspaceUsers.getOrDefault(workspaceId, Set.of());
    }

    /**
     * 사용자가 접속 중인 모든 워크스페이스 ID 조회
     */
    public Set<Long> getUserWorkspaces(Long userId) {
        return userWorkspaces.getOrDefault(userId, Set.of());
    }

    /**
     * 세션 ID로 사용자 ID 조회
     */
    public Long getUserId(String sessionId) {
        return sessionUsers.get(sessionId);
    }

    /**
     * 세션 ID로 워크스페이스 ID 조회
     */
    public Long getWorkspaceId(String sessionId) {
        return sessionWorkspaces.get(sessionId);
    }

    /**
     * 워크스페이스에 사용자가 접속 중인지 확인
     */
    public boolean isUserInWorkspace(Long workspaceId, Long userId) {
        Set<Long> users = workspaceUsers.get(workspaceId);
        return users != null && users.contains(userId);
    }

    /**
     * 워크스페이스 접속자 수 조회
     */
    public int getWorkspaceUserCount(Long workspaceId) {
        Set<Long> users = workspaceUsers.get(workspaceId);
        return users != null ? users.size() : 0;
    }

    /**
     * 전체 활성 워크스페이스 수
     */
    public int getActiveWorkspaceCount() {
        return workspaceUsers.size();
    }
}

