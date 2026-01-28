package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyRecommendDto;
import com.ssafy.domain.study.dto.response.StudyResponse;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyRecommendAction;
import com.ssafy.domain.study.repository.StudySearchCondition;
import com.ssafy.domain.study.service.StudyRecommendService;
import com.ssafy.domain.study.service.StudyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
@Slf4j
public class StudyController {
    private final StudyService studyService;
    private final StudyRecommendService studyRecommendService;

    /**
     * 전체 스터디 목록 조회
     * GET /api/v1/study?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<Study>> getAllStudies(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 전체 스터디 목록 조회: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Study> studies = studyService.getAllStudies(pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 모집중인 스터디 목록 조회
     * GET /api/v1/study/recruiting?page=0&size=20
     */
    @GetMapping("/recruiting")
    public ResponseEntity<Page<Study>> getRecruitingStudies(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("API 호출 - 모집중인 스터디 목록 조회");

        Page<Study> studies = studyService.getRecruitingStudies(pageable);

        return ResponseEntity.ok(studies);
    }
    /**
     * 내 스터디 목록 조회 (참여 중인 모든 스터디)
     * GET /api/v1/study/my?page=0&size=20
     */
    @GetMapping("/my")
    public ResponseEntity<Page<Study>> getMyStudies(
            @RequestHeader("user-id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 내 스터디 목록 조회: userId={}", userId);

        Page<Study> studies = studyService.getMyStudies(userId, pageable);

        log.info("API 응답 - 내 스터디 목록: userId={}, count={}",
                userId, studies.getTotalElements());

        return ResponseEntity.ok(studies);
    }
    /**
     * 스터디 검색/필터링
     * GET /api/v1/study/search?keyword=알고리즘&meetingType=ONLINE&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Study>> searchStudies(
            @ModelAttribute StudySearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 스터디 검색 : keyword={}, topic={}",
                condition.getKeyword(), condition.getTopicId());

        Page<Study> studies = studyService.searchStudies(condition, pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디장별 스터디 목록 조회
     * GET /api/v1/study/leader/{leaderId}?page=0&size=20
     */
    @GetMapping("/leader/{leaderId}")
    public ResponseEntity<Page<Study>> getStudiesByLeader(
            @PathVariable Long leaderId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 스터디장별 조회: leaderId={}", leaderId);

        Page<Study> studies = studyService.getStudiesByLeader(leaderId, pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디장의 특정 상태 스터디 목록 조회
     * GET /api/v1/study/leader/{leaderId}/status/{status}?page=0&size=20
     */
    @GetMapping("/leader/{leaderId}/status/{status}")
    public ResponseEntity<Page<Study>> getStudiesByLeaderAndStatus(
            @PathVariable Long leaderId,
            @PathVariable Status status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("API 호출 - 스터디장 + 상태 조회: leaderId={}, status={}", leaderId, status);

        Page<Study> studies = studyService.getStudiesByLeaderAndStatus(leaderId, status, pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디 상세 조회
     * GET /api/v1/study/{studyId}
     */
    @GetMapping("/{studyId}")
    public ResponseEntity<Study> getStudyDetail(
            @PathVariable Long studyId,
            @RequestHeader(value = "User-Id", required = false) Long userId) {

        log.info("API 호출 - 스터디 상세 조회: studyId={}", studyId);

        Study study = studyService.getStudyById(studyId);

        // 추천 반응 자동 기록 (로그인 사용자가 추천에서 클릭한 경우)
        if (userId != null) {
            studyRecommendService.tryLogAction(userId, studyId, StudyRecommendAction.ActionType.CLICK);
        }

        return ResponseEntity.ok(study);
    }

    /**
     * 특정 상태의 스터디 개수 조회
     * GET /api/v1/study/count?status=RECRUITING
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countStudies(
            @RequestParam Status status) {

        log.info("API 호출 - 스터디 개수 조회: status={}", status);

        Long count = studyService.countStudiesByStatus(status);

        return ResponseEntity.ok(count);
    }

    /**
     * 스터디 존재 여부 확인
     * GET /api/v1/study/{studyId}/exists
     */
    @GetMapping("/{studyId}/exists")
    public ResponseEntity<Boolean> existsStudy(
            @PathVariable Long studyId) {

        log.info("API 호출 - 스터디 존재 확인: studyId={}", studyId);

        boolean exists = studyService.existsStudy(studyId);

        return ResponseEntity.ok(exists);
    }

    /**
     * 스터디 생성
     * POST /api/v1/study
     */
    @PostMapping
    public ResponseEntity<StudyResponse> createStudy(
            @Valid @RequestBody StudyCreateRequest request,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 스터디 생성: userId={}, studyName={}", userId, request.getName());

        StudyResponse response = studyService.createStudy(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    /**
     * 스터디 수정
     * PUT /api/v1/study/{studyId}
     */
    @PutMapping("/{studyId}")
    public ResponseEntity<StudyResponse> updateStudy(
            @PathVariable Long studyId,
            @Valid @RequestBody StudyUpdateRequest request,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 스터디 수정: studyId={}, userId={}", studyId, userId);

        StudyResponse response = studyService.updateStudy(studyId, request, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디 삭제
     * DELETE /api/v1/study/{studyId}
     */
    @DeleteMapping("/{studyId}")
    public ResponseEntity<Void> deleteStudy(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 스터디 삭제: studyId={}, userId={}", studyId, userId);

        studyService.deleteStudy(studyId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 스터디 상태 변경
     * PATCH /api/v1/study/{studyId}/status
     */
    @PatchMapping("/{studyId}/status")
    public ResponseEntity<StudyResponse> updateStudyStatus(
            @PathVariable Long studyId,
            @RequestBody StatusUpdateRequest request,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 스터디 상태 변경: studyId={}, newStatus={}, userId={}",
                studyId, request.getStatus(), userId);

        StudyResponse response = studyService.updateStudyStatus(studyId, request.getStatus(), userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 모집 기간 연장
     * PATCH /api/v1/study/{studyId}/extend-recruitment
     */
    @PatchMapping("/{studyId}/extend-recruitment")
    public ResponseEntity<StudyResponse> extendRecruitment(
            @PathVariable Long studyId,
            @RequestBody RecruitmentExtensionRequest request,
            @RequestHeader("User-Id") Long userId) {

        log.info("API 호출 - 모집 기간 연장: studyId={}, newEndDate={}, userId={}",
                studyId, request.getNewEndDate(), userId);

        StudyResponse response = studyService.extendRecruitment(studyId, request.getNewEndDate(), userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 맞춤 스터디 참여 추천 (규칙 기반)
     * GET /api/v1/study/recommend?limit=10
     * 기술스택 + 일정 + 토픽 계층(연관 기술) + 스터디장 평점 기반 매칭
     */
    @GetMapping("/recommend")
    public ResponseEntity<List<StudyRecommendDto>> getRecommendedStudies(
            @RequestHeader("User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        log.info("API 호출 - 스터디 참여 추천: userId={}, limit={}", userId, limit);

        List<StudyRecommendDto> recommendations = studyRecommendService.getRecommendedStudies(userId, limit);

        log.info("API 응답 - 스터디 참여 추천: userId={}, count={}", userId, recommendations.size());

        return ResponseEntity.ok(recommendations);
    }

    /**
     * 특정 토픽 기반 스터디 참여 추천
     * GET /api/v1/study/recommend/topic/{topicId}?limit=10
     */
    @GetMapping("/recommend/topic/{topicId}")
    public ResponseEntity<List<StudyRecommendDto>> getRecommendedStudiesByTopic(
            @RequestHeader("User-Id") Long userId,
            @PathVariable Long topicId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        log.info("API 호출 - 토픽 기반 스터디 추천: userId={}, topicId={}, limit={}", userId, topicId, limit);

        List<StudyRecommendDto> recommendations = studyRecommendService.getRecommendedStudiesByTopic(userId, topicId, limit);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * 스터디 상태 변경 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        @NotNull(message = "상태는 필수입니다")
        private Status status;
    }

    /**
     * 모집 기간 연장 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruitmentExtensionRequest {
        @NotNull(message = "새로운 모집 종료일은 필수입니다")
        private LocalDate newEndDate;
    }
}
