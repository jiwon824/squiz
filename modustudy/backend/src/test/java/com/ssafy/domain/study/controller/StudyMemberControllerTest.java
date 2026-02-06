package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.StudyMemberResponse;
import com.ssafy.domain.study.entity.MemberRole;
import com.ssafy.domain.study.service.StudyMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.common.exception.StudyException;
import com.ssafy.common.exception.handler.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
class StudyMemberControllerTest {

    @Mock
    private StudyMemberService studyMemberService;

    @InjectMocks
    private StudyMemberController studyMemberController;

    private MockMvc mockMvc;

    private StudyMemberResponse leaderResponse;
    private StudyMemberResponse memberResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(studyMemberController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())  // 예외 핸들러 추가
                .build();

        // 스터디장 응답
        leaderResponse = StudyMemberResponse.builder()
                .memberId(1L)  // id -> memberId
                .userId(1L)
                .userName("김싸피")
                .userNickname("ssafy_kim")
                .role(MemberRole.LEADER)
                .isProbation(false)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .build();

        // 일반 멤버 응답
        memberResponse = StudyMemberResponse.builder()
                .memberId(2L)  // id -> memberId
                .userId(2L)
                .userName("이싸피")
                .userNickname("ssafy_lee")
                .role(MemberRole.MEMBER)
                .isProbation(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("스터디 멤버 목록 조회 성공")
    void getStudyMembers_Success() throws Exception {
        // given
        Long studyId = 1L;
        List<StudyMemberResponse> members = List.of(leaderResponse, memberResponse);
        Page<StudyMemberResponse> page = new PageImpl<>(members, PageRequest.of(0, 10), 2);

        given(studyMemberService.getStudyMembers(eq(studyId), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/members", studyId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("스터디 멤버 수 조회 성공")
    void countStudyMembers_Success() throws Exception {
        // given
        Long studyId = 1L;
        given(studyMemberService.countStudyMembers(studyId)).willReturn(2);

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/members/count", studyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    @DisplayName("멤버 여부 확인 - true")
    void isMember_True() throws Exception {
        // given
        Long studyId = 1L;
        Long userId = 2L;
        given(studyMemberService.isMember(studyId, userId)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/members/{userId}/check", studyId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("멤버 여부 확인 - false")
    void isMember_False() throws Exception {
        // given
        Long studyId = 1L;
        Long userId = 999L;
        given(studyMemberService.isMember(studyId, userId)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/members/{userId}/check", studyId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    @DisplayName("멤버 목록 페이징 - 첫 페이지")
    void getStudyMembers_Paging_FirstPage() throws Exception {
        // given
        Long studyId = 1L;
        List<StudyMemberResponse> members = List.of(leaderResponse);
        Page<StudyMemberResponse> page = new PageImpl<>(members, PageRequest.of(0, 1), 2);

        given(studyMemberService.getStudyMembers(eq(studyId), any(Pageable.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/members", studyId)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @DisplayName("스터디 탈퇴 성공")
    void leaveStudy_Success() throws Exception {
        // given
        Long studyId = 1L;
        Long userId = 2L;
        willDoNothing().given(studyMemberService).leaveStudy(studyId, userId);

        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/members/leave", studyId)
                        .header("User-Id", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("스터디 탈퇴 실패 - 스터디장은 탈퇴 불가")
    void leaveStudy_Fail_LeaderCannotLeave() throws Exception {
        // given
        Long studyId = 1L;
        Long userId = 1L;  // 스터디장 ID
        willThrow(new StudyException.InvalidStudyRequestException("스터디장은 탈퇴할 수 없습니다."))
                .given(studyMemberService).leaveStudy(studyId, userId);

        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/members/leave", studyId)
                        .header("User-Id", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("스터디 탈퇴 실패 - 존재하지 않는 스터디")
    void leaveStudy_Fail_StudyNotFound() throws Exception {
        // given
        Long studyId = 999L;
        Long userId = 2L;
        willThrow(new StudyException.StudyNotFoundException(studyId))
                .given(studyMemberService).leaveStudy(studyId, userId);

        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/members/leave", studyId)
                        .header("User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("스터디 탈퇴 실패 - 멤버가 아닌 사용자")
    void leaveStudy_Fail_NotAMember() throws Exception {
        // given
        Long studyId = 1L;
        Long userId = 999L;
        willThrow(new StudyException.InvalidStudyRequestException("스터디 멤버가 아닙니다."))
                .given(studyMemberService).leaveStudy(studyId, userId);

        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/members/leave", studyId)
                        .header("User-Id", userId))
                .andExpect(status().isBadRequest());
    }
}
