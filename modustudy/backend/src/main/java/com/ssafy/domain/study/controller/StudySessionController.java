package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.StudySessionCreateRequest;
import com.ssafy.domain.study.dto.request.StudySessionUpdateRequest;
import com.ssafy.domain.study.dto.response.StudySessionResponse;
import com.ssafy.domain.study.entity.SessionStatus;
import com.ssafy.domain.study.service.StudySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/studies/{studyId}/sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;

    /**
     * 세션 생성 (배열로 통일)
     * POST /api/v1/studies/{studyId}/sessions
     * 단건: [{ ... }], 다건: [{ ... }, { ... }, ...]
     */
    @PostMapping
    public ResponseEntity<List<StudySessionResponse>> createSessions(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody List<StudySessionCreateRequest> requests) {

                List<StudySessionResponse> responses = studySessionService.createSessionsBulk(studyId, userId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * 세션 단건 조회
     * GET /api/v1/studies/{studyId}/sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<StudySessionResponse> getSession(
            @PathVariable Long studyId,
            @PathVariable Long sessionId) {

                StudySessionResponse response = studySessionService.getSession(studyId, sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 회차로 조회
     * GET /api/v1/studies/{studyId}/sessions/number/{sessionNumber}
     */
    @GetMapping("/number/{sessionNumber}")
    public ResponseEntity<StudySessionResponse> getSessionByNumber(
            @PathVariable Long studyId,
            @PathVariable Integer sessionNumber) {

                StudySessionResponse response = studySessionService.getSessionByNumber(studyId, sessionNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * 스터디별 세션 목록 조회
     * GET /api/v1/studies/{studyId}/sessions
     */
    @GetMapping
    public ResponseEntity<List<StudySessionResponse>> getSessions(
            @PathVariable Long studyId,
            @RequestParam(required = false) SessionStatus status) {

                List<StudySessionResponse> sessions;
        if (status != null) {
            sessions = studySessionService.getSessionsByStatus(studyId, status);
        } else {
            sessions = studySessionService.getSessionsByStudyId(studyId);
        }

        return ResponseEntity.ok(sessions);
    }

    /**
     * 다음 예정 세션 조회
     * GET /api/v1/studies/{studyId}/sessions/next
     */
    @GetMapping("/next")
    public ResponseEntity<StudySessionResponse> getNextSession(
            @PathVariable Long studyId) {

                StudySessionResponse response = studySessionService.getNextSession(studyId);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 수정
     * PUT /api/v1/studies/{studyId}/sessions/{sessionId}
     */
    @PutMapping("/{sessionId}")
    public ResponseEntity<StudySessionResponse> updateSession(
            @PathVariable Long studyId,
            @PathVariable Long sessionId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody StudySessionUpdateRequest request) {

                StudySessionResponse response = studySessionService.updateSession(studyId, sessionId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 삭제
     * DELETE /api/v1/studies/{studyId}/sessions/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long studyId,
            @PathVariable Long sessionId,
            @RequestHeader("User-Id") Long userId) {

                studySessionService.deleteSession(studyId, sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 세션 시작
     * POST /api/v1/studies/{studyId}/sessions/{sessionId}/start
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<StudySessionResponse> startSession(
            @PathVariable Long studyId,
            @PathVariable Long sessionId,
            @RequestHeader("User-Id") Long userId) {

                StudySessionResponse response = studySessionService.startSession(studyId, sessionId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 완료
     * POST /api/v1/studies/{studyId}/sessions/{sessionId}/complete
     */
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<StudySessionResponse> completeSession(
            @PathVariable Long studyId,
            @PathVariable Long sessionId,
            @RequestHeader("User-Id") Long userId) {

                StudySessionResponse response = studySessionService.completeSession(studyId, sessionId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 취소
     * POST /api/v1/studies/{studyId}/sessions/{sessionId}/cancel
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<StudySessionResponse> cancelSession(
            @PathVariable Long studyId,
            @PathVariable Long sessionId,
            @RequestHeader("User-Id") Long userId) {

                StudySessionResponse response = studySessionService.cancelSession(studyId, sessionId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 세션 통계 조회
     * GET /api/v1/studies/{studyId}/sessions/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<StudySessionService.SessionStatistics> getSessionStatistics(
            @PathVariable Long studyId) {

                StudySessionService.SessionStatistics statistics = studySessionService.getSessionStatistics(studyId);
        return ResponseEntity.ok(statistics);
    }
}
