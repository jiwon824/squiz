package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.StudyMemberResponse;
import com.ssafy.domain.study.service.StudyMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
@Slf4j
public class StudyMemberController {

    private final StudyMemberService studyMemberService;

    /**
     * 스터디 멤버 목록 조회
     * GET /api/v1/study/{studyId}/members
     */
    @GetMapping("/{studyId}/members")
    public ResponseEntity<Page<StudyMemberResponse>> getStudyMembers(
            @PathVariable Long studyId,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {

                Page<StudyMemberResponse> members = studyMemberService.getStudyMembers(studyId, pageable);

                return ResponseEntity.ok(members);
    }

    /**
     * 스터디 멤버 수 조회
     * GET /api/v1/study/{studyId}/members/count
     */
    @GetMapping("/{studyId}/members/count")
    public ResponseEntity<Integer> countStudyMembers(@PathVariable Long studyId) {

        int count = studyMemberService.countStudyMembers(studyId);

        return ResponseEntity.ok(count);
    }

    /**
     * 특정 사용자가 스터디 멤버인지 확인
     * GET /api/v1/study/{studyId}/members/{userId}/check
     */
    @GetMapping("/{studyId}/members/{userId}/check")
    public ResponseEntity<Boolean> isMember(
            @PathVariable Long studyId,
            @PathVariable Long userId) {

                boolean isMember = studyMemberService.isMember(studyId, userId);

        return ResponseEntity.ok(isMember);
    }

    /**
     * 스터디 탈퇴 (본인만 가능)
     * DELETE /api/v1/study/{studyId}/members/leave
     */
    @DeleteMapping("/{studyId}/members/leave")
    public ResponseEntity<Void> leaveStudy(
            @PathVariable Long studyId,
            @RequestHeader("User-Id") Long userId) {

                studyMemberService.leaveStudy(studyId, userId);

                return ResponseEntity.noContent().build();
    }
}
