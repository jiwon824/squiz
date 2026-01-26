package com.ssafy.domain.study.workspace.controller;

import com.ssafy.domain.study.workspace.dto.response.WorkspaceResponse;
import com.ssafy.domain.study.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 워크스페이스 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 워크스페이스 생성 (스터디 시작 시 호출)
     */
    @PostMapping("/study/{studyId}")
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 워크스페이스 생성: studyId={}, userId={}", studyId, userId);

        WorkspaceResponse response = workspaceService.createWorkspace(studyId);

        log.info("API 응답 - 워크스페이스 생성 완료: workspaceId={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 워크스페이스 조회 (ID)
     */
    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable Long workspaceId) {

        log.info("API 호출 - 워크스페이스 조회: workspaceId={}", workspaceId);

        WorkspaceResponse response = workspaceService.getWorkspace(workspaceId);

        log.info("API 응답 - 워크스페이스 조회 완료: studyId={}", response.getStudyId());

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디 ID로 워크스페이스 조회
     */
    @GetMapping("/study/{studyId}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceByStudyId(
            @PathVariable Long studyId) {

        log.info("API 호출 - 스터디 ID로 워크스페이스 조회: studyId={}", studyId);

        WorkspaceResponse response = workspaceService.getWorkspaceByStudyId(studyId);

        log.info("API 응답 - 워크스페이스 조회 완료: workspaceId={}", response.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * 워크스페이스 존재 여부 확인
     */
    @GetMapping("/study/{studyId}/exists")
    public ResponseEntity<Boolean> existsWorkspace(
            @PathVariable Long studyId) {

        log.info("API 호출 - 워크스페이스 존재 여부 확인: studyId={}", studyId);

        boolean exists = workspaceService.existsWorkspace(studyId);

        log.info("API 응답 - 워크스페이스 존재 여부: {}", exists);

        return ResponseEntity.ok(exists);
    }

    /**
     * 워크스페이스 삭제
     */
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 워크스페이스 삭제: workspaceId={}, userId={}", workspaceId, userId);

        workspaceService.deleteWorkspace(workspaceId);

        log.info("API 응답 - 워크스페이스 삭제 완료");

        return ResponseEntity.noContent().build();
    }

    /**
     * 스터디 ID로 워크스페이스 삭제
     */
    @DeleteMapping("/study/{studyId}")
    public ResponseEntity<Void> deleteWorkspaceByStudyId(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 스터디 ID로 워크스페이스 삭제: studyId={}, userId={}", studyId, userId);

        workspaceService.deleteWorkspaceByStudyId(studyId);

        log.info("API 응답 - 워크스페이스 삭제 완료");

        return ResponseEntity.noContent().build();
    }
}