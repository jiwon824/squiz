package com.ssafy.domain.study.workspace.controller;

import com.ssafy.domain.study.workspace.dto.response.WorkspaceResponse;
import com.ssafy.domain.study.workspace.service.WorkspaceService;
import com.ssafy.domain.study.workspace.websocket.WorkspaceSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 워크스페이스 컨트롤러
 */
 @Slf4j
 @RestController
 @RequestMapping("/api/v1/workspaces")
 @RequiredArgsConstructor
 public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceSessionService workspaceSessionService;

    /**
     * 워크스페이스 생성 (스터디 시작 시 호출)
     */
    @PostMapping("/study/{studyId}")
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

                WorkspaceResponse response = workspaceService.createWorkspace(studyId);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 워크스페이스 조회 (ID)
     */
    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable Long workspaceId) {

                WorkspaceResponse response = workspaceService.getWorkspace(workspaceId);

                return ResponseEntity.ok(response);
    }

    /**
     * 스터디 ID로 워크스페이스 조회
     */
    @GetMapping("/study/{studyId}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceByStudyId(
            @PathVariable Long studyId) {

                WorkspaceResponse response = workspaceService.getWorkspaceByStudyId(studyId);

                return ResponseEntity.ok(response);
    }

    /**
     * 워크스페이스 존재 여부 확인
     */
    @GetMapping("/study/{studyId}/exists")
    public ResponseEntity<Boolean> existsWorkspace(
            @PathVariable Long studyId) {

                boolean exists = workspaceService.existsWorkspace(studyId);

                return ResponseEntity.ok(exists);
    }

    /**
     * 워크스페이스 접속 중인 사용자 목록 조회
     */
    @GetMapping("/{workspaceId}/presence")
    public ResponseEntity<Set<Long>> getWorkspacePresence(
            @PathVariable Long workspaceId) {

                Set<Long> users = workspaceSessionService.getWorkspaceUsers(workspaceId);

                return ResponseEntity.ok(users);
    }

    /**
     * 워크스페이스 삭제
     */
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader("User-Id") Long userId) {

                workspaceService.deleteWorkspace(workspaceId);

                return ResponseEntity.noContent().build();
    }

    /**
     * 스터디 ID로 워크스페이스 삭제
     */
    @DeleteMapping("/study/{studyId}")
    public ResponseEntity<Void> deleteWorkspaceByStudyId(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

                workspaceService.deleteWorkspaceByStudyId(studyId);

                return ResponseEntity.noContent().build();
    }
}

