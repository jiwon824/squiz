package com.ssafy.domain.board.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.board.dto.request.BoardCommentCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostCreateRequest;
import com.ssafy.domain.board.dto.response.BoardCommentResponse;
import com.ssafy.domain.board.dto.response.BoardPostDetailResponse;
import com.ssafy.domain.board.dto.response.BoardPostSummaryResponse;
import com.ssafy.domain.board.dto.response.BoardRecruitingStudyResponse;
import com.ssafy.domain.board.service.BoardService;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.StudyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("모집중 스터디 목록 조회")
    void getRecruitingStudies() throws Exception {
        BoardRecruitingStudyResponse response = BoardRecruitingStudyResponse.builder()
                .id(1L)
                .name("모집중 스터디")
                .topicName("Java")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .maxMembers(6)
                .currentMembers(2)
                .status(Status.RECRUITING)
                .build();
        when(boardService.getRecruitingStudies(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/boards/recruitments/studies")
                        .header("User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L));
    }

    @Test
    @DisplayName("모집글 작성")
    void createPost() throws Exception {
        BoardPostDetailResponse response = BoardPostDetailResponse.builder()
                .id(10L)
                .studyId(1L)
                .studyName("스터디")
                .topicName("Java")
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .maxMembers(6)
                .currentMembers(2)
                .studyStatus(Status.RECRUITING)
                .title("모집글 제목")
                .content("모집글 내용")
                .authorId(1L)
                .authorName("leader")
                .authorProfileImage(null)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .comments(List.of())
                .build();
        when(boardService.createPost(eq(1L), any(BoardPostCreateRequest.class))).thenReturn(response);

        BoardPostCreateRequest request = new BoardPostCreateRequest(1L, "모집글 제목", "모집글 내용");

        mockMvc.perform(post("/api/v1/boards/recruitments")
                        .header("User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.title").value("모집글 제목"));
    }

    @Test
    @DisplayName("모집글 목록 조회")
    void getPosts() throws Exception {
        BoardPostSummaryResponse summary = BoardPostSummaryResponse.builder()
                .id(10L)
                .studyId(1L)
                .studyName("스터디")
                .topicName("Java")
                .maxMembers(6)
                .currentMembers(2)
                .studyStatus(Status.RECRUITING)
                .title("모집글 제목")
                .authorId(1L)
                .authorName("leader")
                .authorProfileImage(null)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        Page<BoardPostSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        when(boardService.getPosts(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/boards/recruitments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10L));
    }

    @Test
    @DisplayName("모집글 댓글 작성")
    void addComment() throws Exception {
        BoardCommentResponse response = BoardCommentResponse.builder()
                .id(100L)
                .postId(10L)
                .authorId(2L)
                .authorName("member")
                .authorProfileImage(null)
                .parentId(null)
                .content("댓글 내용")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(boardService.addComment(eq(2L), eq(10L), any(BoardCommentCreateRequest.class))).thenReturn(response);

        BoardCommentCreateRequest request = new BoardCommentCreateRequest(null, "댓글 내용");

        mockMvc.perform(post("/api/v1/boards/recruitments/10/comments")
                        .header("User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L));
    }

    @Test
    @DisplayName("모집글 댓글 삭제")
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/api/v1/boards/recruitments/10/comments/100")
                        .header("User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
