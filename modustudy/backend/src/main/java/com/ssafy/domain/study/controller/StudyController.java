package com.ssafy.domain.study.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.study.dto.request.StudyCreateRequest;
import com.ssafy.domain.study.dto.request.StudyReportRequest;
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
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping
    public ResponseEntity<Page<StudyResponse>> getAllStudies(
            @PageableDefault(size = 20) Pageable pageable) {

                Page<StudyResponse> studies = studyService.getAllStudies(pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 모집중인 스터디 목록 조회
     * GET /api/v1/study/recruiting?page=0&size=20
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping("/recruiting")
    public ResponseEntity<Page<StudyResponse>> getRecruitingStudies(
            @PageableDefault(size = 20) Pageable pageable) {
                Page<StudyResponse> studies = studyService.getRecruitingStudies(pageable);

        return ResponseEntity.ok(studies);
    }
    /**
     * 내 스터디 목록 조회 (참여 중인 스터디)
     * GET /api/v1/study/my?page=0&size=20
     * GET /api/v1/study/my?status=COMPLETED&page=0&size=20
     * - status 파라미터로 특정 상태의 스터디만 필터링 가능
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping("/my")
    public ResponseEntity<Page<StudyResponse>> getMyStudies(
            @RequestHeader("user-id") Long userId,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = 20) Pageable pageable) {

                Page<StudyResponse> studies = studyService.getMyStudies(userId, status, pageable);

                return ResponseEntity.ok(studies);
    }
    /**
     * 스터디 검색/필터링
     * GET /api/v1/study/search?keyword=알고리즘&meetingType=ONLINE&page=0&size=20
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping("/search")
    public ResponseEntity<Page<StudyResponse>> searchStudies(
            @ModelAttribute StudySearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable) {

                Page<StudyResponse> studies = studyService.searchStudies(condition, pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디장별 스터디 목록 조회
     * GET /api/v1/study/leader/{leaderId}?page=0&size=20
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping("/leader/{leaderId}")
    public ResponseEntity<Page<StudyResponse>> getStudiesByLeader(
            @PathVariable Long leaderId,
            @PageableDefault(size = 20) Pageable pageable) {

                Page<StudyResponse> studies = studyService.getStudiesByLeader(leaderId, pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디장의 특정 상태 스터디 목록 조회
     * GET /api/v1/study/leader/{leaderId}/status/{status}?page=0&size=20
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping("/leader/{leaderId}/status/{status}")
    public ResponseEntity<Page<StudyResponse>> getStudiesByLeaderAndStatus(
            @PathVariable Long leaderId,
            @PathVariable Status status,
            @PageableDefault(size = 20) Pageable pageable) {

                Page<StudyResponse> studies = studyService.getStudiesByLeaderAndStatus(leaderId, status, pageable);

        return ResponseEntity.ok(studies);
    }

    /**
     * 스터디 상세 조회
     * GET /api/v1/study/{studyId}
     * - 순환참조 방지를 위해 StudyResponse DTO로 반환
     */
    @GetMapping("/{studyId}")
    public ResponseEntity<StudyResponse> getStudyDetail(
            @PathVariable Long studyId,
            @RequestHeader(value = "User-Id", required = false) Long userId) {

                StudyResponse study = studyService.getStudyById(studyId);

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

                studyService.deleteStudy(studyId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자용 스터디 삭제 (상태 무관)
     * DELETE /api/v1/study/{studyId}/admin
     */
    @DeleteMapping("/{studyId}/admin")
    public ResponseEntity<Void> adminDeleteStudy(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

                studyService.adminDeleteStudy(studyId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 스터디 신고
     * POST /api/v1/study/{studyId}/report
     */
    @PostMapping("/{studyId}/report")
    public ResponseEntity<ApiResponse<Void>> reportStudy(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody StudyReportRequest request) {

        studyService.reportStudy(userId, studyId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신고가 접수되었습니다."));
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

                StudyResponse response = studyService.extendRecruitment(studyId, request.getNewEndDate(), userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 스터디 시작하기
     * POST /api/v1/study/{studyId}/start
     * - 모집완료/시작대기 또는 확정대기 상태에서 스터디장이 호출
     * - 워크스페이스 생성 및 상태를 진행중으로 변경
     */
    @PostMapping("/{studyId}/start")
    public ResponseEntity<StudyResponse> startStudy(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

                StudyResponse response = studyService.startStudy(studyId, userId);

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

                List<StudyRecommendDto> recommendations = studyRecommendService.getRecommendedStudies(userId, limit);

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


