package com.ssafy.domain.board.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.board.dto.request.BoardCommentCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostCreateRequest;
import com.ssafy.domain.board.dto.response.BoardCommentResponse;
import com.ssafy.domain.board.dto.response.BoardPostDetailResponse;
import com.ssafy.domain.board.dto.response.BoardPostSummaryResponse;
import com.ssafy.domain.board.dto.response.BoardRecruitingStudyResponse;
import com.ssafy.domain.board.service.BoardService;
import com.ssafy.domain.board.entity.RecruitmentStatus;
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
    @DisplayName("紐⑥쭛以??ㅽ꽣??紐⑸줉 議고쉶")
    void getRecruitingStudies() throws Exception {
        BoardRecruitingStudyResponse response = BoardRecruitingStudyResponse.builder()
                .id(1L)
                .name("紐⑥쭛以??ㅽ꽣??)
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
    @DisplayName("紐⑥쭛湲 ?묒꽦")
    void createPost() throws Exception {
        BoardPostDetailResponse response = BoardPostDetailResponse.builder()
                .id(10L)
                .meetingType(MeetingType.ONLINE)
                .recruitmentField("백엔드")
                .targetMembers(6)
                .recruitmentStatus(RecruitmentStatus.RECRUITING)
                .title("紐⑥쭛湲 ?쒕ぉ")
                .content("紐⑥쭛湲 ?댁슜")
                .authorId(1L)
                .authorName("leader")
                .authorProfileImage(null)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .comments(List.of())
                .build();
        when(boardService.createPost(eq(1L), any(BoardPostCreateRequest.class))).thenReturn(response);

        BoardPostCreateRequest request = new BoardPostCreateRequest("紐⑥쭛湲 ?쒕ぉ", "紐⑥쭛湲 ?댁슜", "백엔드", MeetingType.ONLINE, 6);

        mockMvc.perform(post("/api/v1/boards/recruitments")
                        .header("User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.title").value("紐⑥쭛湲 ?쒕ぉ"));
    }

    @Test
    @DisplayName("紐⑥쭛湲 紐⑸줉 議고쉶")
    void getPosts() throws Exception {
        BoardPostSummaryResponse summary = BoardPostSummaryResponse.builder()
                .id(10L)
                .title("紐⑥쭛湲 ?쒕ぉ")
                .authorId(1L)
                .authorName("leader")
                .authorProfileImage(null)
                .recruitmentField("백엔드")
                .meetingType(MeetingType.ONLINE)
                .targetMembers(6)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .recruitmentStatus(RecruitmentStatus.RECRUITING)
                .build();
        Page<BoardPostSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        when(boardService.getPosts(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/boards/recruitments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10L));
    }

    @Test
    @DisplayName("紐⑥쭛湲 ?볤? ?묒꽦")
    void addComment() throws Exception {
        BoardCommentResponse response = BoardCommentResponse.builder()
                .id(100L)
                .postId(10L)
                .authorId(2L)
                .authorName("member")
                .authorProfileImage(null)
                .parentId(null)
                .content("?볤? ?댁슜")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(boardService.addComment(eq(2L), eq(10L), any(BoardCommentCreateRequest.class))).thenReturn(response);

        BoardCommentCreateRequest request = new BoardCommentCreateRequest(null, "?볤? ?댁슜");

        mockMvc.perform(post("/api/v1/boards/recruitments/10/comments")
                        .header("User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L));
    }

    @Test
    @DisplayName("紐⑥쭛湲 ?볤? ??젣")
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/api/v1/boards/recruitments/10/comments/100")
                        .header("User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}


