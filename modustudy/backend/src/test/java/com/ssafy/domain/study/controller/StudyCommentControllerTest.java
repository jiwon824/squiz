package com.ssafy.domain.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.study.dto.request.StudyCommentCreateRequest;
import com.ssafy.domain.study.dto.request.StudyCommentUpdateRequest;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyCommentRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudyCommentController 통합 테스트
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 @WithMockUser(username = "testuser", roles = {"USER"})
 class StudyCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyCommentRepository commentRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    private User leader;
    private User commenter1;
    private User commenter2;
    private Study testStudy;
    private StudyComment parentComment;
    private StudyComment replyComment;
    private Topic topic;
    private Format format;

    @BeforeEach
    void setUp() {
        // Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 스터디장 생성
        leader = User.builder()
                .userId("leader123")
                .email("leader@test.com")
                .nickname("스터디장")
                .name("김리더")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(5)
                .levelName("Gold")
                .build();
        leader = userRepository.save(leader);

        // 댓글 작성자 1
        commenter1 = User.builder()
                .userId("commenter1")
                .email("commenter1@test.com")
                .nickname("댓글러1")
                .name("이댓글")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        commenter1 = userRepository.save(commenter1);

        // 댓글 작성자 2
        commenter2 = User.builder()
                .userId("commenter2")
                .email("commenter2@test.com")
                .nickname("댓글러2")
                .name("박댓글")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        commenter2 = userRepository.save(commenter2);

        userRepository.flush();

        // 테스트 스터디 생성
        testStudy = Study.builder()
                .leaderId(leader.getId())
                .name("알고리즘 스터디")
                .description("알고리즘 문제 풀이")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.RECRUITING)
                .maxMembers(10)
                .isPublic(true)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .recruitStartDate(LocalDate.of(2025, 1, 15))
                .recruitEndDate(LocalDate.of(2025, 1, 31))
                .extensionCount(0)
                .build();
        testStudy = studyRepository.save(testStudy);
        studyRepository.flush();

        // 테스트용 댓글 생성
        parentComment = StudyComment.builder()
                .studyId(testStudy.getId())
                .userId(commenter1.getId())
                .content("첫 번째 댓글입니다. 스터디 좋아보여요!")
                .build();
        parentComment = commentRepository.save(parentComment);

        // 테스트용 대댓글 생성
        replyComment = StudyComment.builder()
                .studyId(testStudy.getId())
                .userId(commenter2.getId())
                .parentId(parentComment.getId())
                .content("대댓글입니다. 저도 관심있어요!")
                .build();
        replyComment = commentRepository.save(replyComment);
        commentRepository.flush();
    }

    // ============================================================
    // 댓글 생성 테스트
    // ============================================================

    @Test
    @DisplayName("댓글 생성 성공")
    void createComment_Success() throws Exception {
        // given
        StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                .content("새로운 댓글입니다. 스터디 참여하고 싶어요!")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/comments", testStudy.getId())
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studyId").value(testStudy.getId()))
                .andExpect(jsonPath("$.userId").value(commenter1.getId()))
                .andExpect(jsonPath("$.content").value(request.getContent()))
                .andExpect(jsonPath("$.userNickname").value("댓글러1"));
    }

    @Test
    @DisplayName("대댓글 생성 성공")
    void createReply_Success() throws Exception {
        // given
        StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                .parentId(parentComment.getId())
                .content("대댓글 작성합니다. 저도 동의해요!")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/comments", testStudy.getId())
                        .header("user-id", commenter2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.parentId").value(parentComment.getId()))
                .andExpect(jsonPath("$.content").value(request.getContent()));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 이미지 포함")
    void createComment_WithImage_Success() throws Exception {
        // given
        StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                .content("이미지가 포함된 댓글입니다.")
                .imageUrl("https://example.com/image.png")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/comments", testStudy.getId())
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.png"));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 스터디")
    void createComment_StudyNotFound() throws Exception {
        // given
        StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                .content("존재하지 않는 스터디에 댓글 작성")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/comments", 99999L)
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 존재하지 않는 부모 댓글")
    void createReply_ParentNotFound() throws Exception {
        // given
        StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                .parentId(99999L)
                .content("존재하지 않는 부모 댓글에 대댓글")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/comments", testStudy.getId())
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 부모 댓글")));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 대댓글에 대댓글 시도")
    void createReply_ToReply_Fail() throws Exception {
        // given - 대댓글에 대댓글 시도
        StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                .parentId(replyComment.getId())  // 대댓글의 ID
                .content("대댓글에 대댓글 시도")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/study/{studyId}/comments", testStudy.getId())
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("대댓글에는 답글을 달 수 없습니다")));
    }

    // ============================================================
    // 댓글 목록 조회 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 댓글 목록 조회 성공")
    void getCommentsByStudy_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments", testStudy.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments", hasSize(1)))  // 최상위 댓글 1개
                .andExpect(jsonPath("$.comments[0].replies", hasSize(1)))  // 대댓글 1개
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("최상위 댓글만 조회 성공")
    void getParentCommentsOnly_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments/parents", testStudy.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.comments[0].replyCount").value(1));
    }

    @Test
    @DisplayName("대댓글 목록 조회 성공")
    void getReplies_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments/{commentId}/replies",
                        testStudy.getId(), parentComment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("대댓글입니다. 저도 관심있어요!"));
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 존재하지 않는 스터디")
    void getCommentsByStudy_StudyNotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 스터디")));
    }

    // ============================================================
    // 댓글 상세 조회 테스트
    // ============================================================

    @Test
    @DisplayName("댓글 상세 조회 성공")
    void getComment_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parentComment.getId()))
                .andExpect(jsonPath("$.content").value("첫 번째 댓글입니다. 스터디 좋아보여요!"))
                .andExpect(jsonPath("$.userNickname").value("댓글러1"));
    }

    @Test
    @DisplayName("댓글 상세 조회 실패 - 존재하지 않는 댓글")
    void getComment_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), 99999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 댓글")));
    }

    // ============================================================
    // 댓글 수정 테스트
    // ============================================================

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() throws Exception {
        // given
        StudyCommentUpdateRequest request = StudyCommentUpdateRequest.builder()
                .content("수정된 댓글 내용입니다.")
                .build();

        // when & then
        mockMvc.perform(put("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId())
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parentComment.getId()))
                .andExpect(jsonPath("$.content").value("수정된 댓글 내용입니다."));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 권한 없음 (작성자 아님)")
    void updateComment_NotAuthor() throws Exception {
        // given
        StudyCommentUpdateRequest request = StudyCommentUpdateRequest.builder()
                .content("다른 사람이 수정 시도")
                .build();

        // when & then
        mockMvc.perform(put("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId())
                        .header("user-id", commenter2.getId())  // 작성자가 아님
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("권한")));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 존재하지 않는 댓글")
    void updateComment_NotFound() throws Exception {
        // given
        StudyCommentUpdateRequest request = StudyCommentUpdateRequest.builder()
                .content("존재하지 않는 댓글 수정")
                .build();

        // when & then
        mockMvc.perform(put("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), 99999L)
                        .header("user-id", commenter1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 댓글")));
    }

    // ============================================================
    // 댓글 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("댓글 삭제 성공 - 작성자가 삭제")
    void deleteComment_ByAuthor_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId())
                        .header("user-id", commenter1.getId()))
                .andExpect(status().isNoContent());

        // 삭제 확인
        mockMvc.perform(get("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId()))
                .andExpect(status().isBadRequest());  // 삭제되어서 조회 불가
    }

    @Test
    @DisplayName("댓글 삭제 성공 - 스터디장이 삭제")
    void deleteComment_ByLeader_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId())
                        .header("user-id", leader.getId()))  // 스터디장
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void deleteComment_NoPermission() throws Exception {
        // when & then - 작성자도 스터디장도 아닌 사람
        mockMvc.perform(delete("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), parentComment.getId())
                        .header("user-id", commenter2.getId()))  // 권한 없음
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("권한")));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteComment_NotFound() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/study/{studyId}/comments/{commentId}",
                        testStudy.getId(), 99999L)
                        .header("user-id", commenter1.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 댓글")));
    }

    // ============================================================
    // 댓글 개수 조회 테스트
    // ============================================================

    @Test
    @DisplayName("댓글 개수 조회 성공")
    void getCommentCount_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/study/{studyId}/comments/count", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));  // 댓글 1개 + 대댓글 1개
    }

    // ============================================================
    // 삭제된 댓글 처리 테스트
    // ============================================================

    @Test
    @DisplayName("삭제된 댓글은 '삭제된 댓글입니다'로 표시")
    void deletedComment_ShowsDeletedMessage() throws Exception {
        // given - 댓글 삭제
        parentComment.delete();
        commentRepository.save(parentComment);
        commentRepository.flush();

        // when & then - 목록에서는 삭제된 댓글도 표시 (대댓글 때문에)
        mockMvc.perform(get("/api/v1/study/{studyId}/comments", testStudy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].content").value("삭제된 댓글입니다."))
                .andExpect(jsonPath("$.comments[0].isDeleted").value(true));
    }
}
