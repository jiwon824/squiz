package com.ssafy.domain.material.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.material.dto.request.MaterialCommentCreateRequest;
import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialComment;
import com.ssafy.domain.material.repository.MaterialCommentRepository;
import com.ssafy.domain.material.repository.MaterialRepository;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MaterialCommentController 통합 테스트
 *
 * 테스트 규칙:
 * - 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
 * - 부모 엔티티 저장 후 flush() 호출
 * - 벌크 삭제 후 entityManager.flush() + clear() 호출
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 class MaterialCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialCommentRepository commentRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Study study;
    private Material material;
    private Topic topic;
    private Format format;

    private static final String BASE_URL = "/api/v1/studies/{studyId}/materials/{materialId}/comments";

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. User 생성
        user1 = userRepository.save(User.builder()
                .userId("commentCtrlUser1")
                .email("commentCtrl1@test.com")
                .nickname("댓글컨트롤러1")
                .name("테스트1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        user2 = userRepository.save(User.builder()
                .userId("commentCtrlUser2")
                .email("commentCtrl2@test.com")
                .nickname("댓글컨트롤러2")
                .name("테스트2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // 4. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .topic(topic)
                .format(format)
                .name("알고리즘 스터디")
                .description("알고리즘 문제 풀이 스터디")
                .maxMembers(6)
                .studyType(StudyType.PLANNED)
                .status(Status.IN_PROGRESS)
                .recruitStartDate(LocalDate.now().minusDays(7))
                .recruitEndDate(LocalDate.now().minusDays(1))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
        studyRepository.flush();

        // 5. Material 생성
        material = materialRepository.save(Material.createLinkMaterial(
                study.getId(),
                user1.getId(),
                "DP 개념 정리",
                "다이나믹 프로그래밍 기초",
                "https://test.com/dp",
                1
        ));
        materialRepository.flush();
    }

    @Nested
    @DisplayName("댓글 목록 조회 API")
    class GetCommentsTest {

        @Test
        @DisplayName("댓글 목록 조회 성공")
        void getComments_Success() throws Exception {
            // given
            commentRepository.save(MaterialComment.create(material.getId(), user1.getId(), "댓글 1"));
            commentRepository.save(MaterialComment.create(material.getId(), user2.getId(), "댓글 2"));
            commentRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId(), material.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("댓글 없는 경우 빈 배열 반환")
        void getComments_Empty_Success() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL, study.getId(), material.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("댓글 목록 조회 - 사용자 정보 포함")
        void getComments_ContainsUserInfo_Success() throws Exception {
            // given
            commentRepository.save(MaterialComment.create(material.getId(), user1.getId(), "테스트 댓글"));
            commentRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId(), material.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].user").exists())
                    .andExpect(jsonPath("$[0].user.nickname").value("댓글컨트롤러1"));
        }

        @Test
        @DisplayName("존재하지 않는 자료의 댓글 조회 시 404")
        void getComments_MaterialNotFound() throws Exception {
            // given
            Long nonExistentMaterialId = 99999L;

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId(), nonExistentMaterialId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("댓글 작성 API")
    class CreateCommentTest {

        @Test
        @DisplayName("댓글 작성 성공")
        void createComment_Success() throws Exception {
            // given
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .content("좋은 자료 감사합니다!")
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId(), material.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.content").value("좋은 자료 감사합니다!"))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("다른 사용자도 댓글 작성 가능")
        void createComment_ByOtherUser_Success() throws Exception {
            // given
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .content("저도 도움 받았어요!")
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId(), material.getId())
                            .header("User-Id", user2.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("댓글 내용 없이 작성 시 400")
        void createComment_NoContent_BadRequest() throws Exception {
            // given
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId(), material.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 자료에 댓글 작성 시 404")
        void createComment_MaterialNotFound() throws Exception {
            // given
            Long nonExistentMaterialId = 99999L;
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .content("댓글 내용")
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId(), nonExistentMaterialId)
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("댓글 삭제 API")
    class DeleteCommentTest {

        @Test
        @DisplayName("본인 댓글 삭제 성공")
        void deleteComment_ByAuthor_Success() throws Exception {
            // given
            MaterialComment comment = commentRepository.save(
                    MaterialComment.create(material.getId(), user1.getId(), "삭제할 댓글"));
            commentRepository.flush();

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{commentId}", study.getId(), material.getId(), comment.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 확인
            entityManager.flush();
            entityManager.clear();
            assert commentRepository.findById(comment.getId()).isEmpty();
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 댓글 삭제 시 403")
        void deleteComment_NotAuthor_Forbidden() throws Exception {
            // given
            MaterialComment comment = commentRepository.save(
                    MaterialComment.create(material.getId(), user1.getId(), "user1 댓글"));
            commentRepository.flush();

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{commentId}", study.getId(), material.getId(), comment.getId())
                            .header("User-Id", user2.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시 404")
        void deleteComment_NotFound() throws Exception {
            // given
            Long nonExistentCommentId = 99999L;

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{commentId}", study.getId(), material.getId(), nonExistentCommentId)
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("다른 자료의 댓글 삭제 시 404")
        void deleteComment_WrongMaterial_NotFound() throws Exception {
            // given
            Material anotherMaterial = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "다른 자료", "설명", "https://test.com/other", 1));
            materialRepository.flush();

            MaterialComment comment = commentRepository.save(
                    MaterialComment.create(anotherMaterial.getId(), user1.getId(), "다른 자료 댓글"));
            commentRepository.flush();

            // when & then - material.getId()로 요청하지만 댓글은 anotherMaterial에 속함
            mockMvc.perform(delete(BASE_URL + "/{commentId}", study.getId(), material.getId(), comment.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}
