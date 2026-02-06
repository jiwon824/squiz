package com.ssafy.domain.study.workspace.controller;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.repository.WorkspaceRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WorkspaceController 통합 테스트
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 @WithMockUser(username = "testuser", roles = {"USER"})
 class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkspaceRepository workspaceRepository;

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

    private User user;
    private Study study1;
    private Study study2;
    private Workspace workspace;
    private Topic topic;
    private Format format;

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
        user = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스트유저")
                .name("테스트")
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
        study1 = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("알고리즘 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        study2 = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("CS 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 5. Workspace 생성 (study1만)
        workspace = workspaceRepository.save(Workspace.create(study1.getId()));
        workspaceRepository.flush();
    }

    @Nested
    @DisplayName("워크스페이스 생성")
    class CreateWorkspace {

        @Test
        @DisplayName("성공 - 워크스페이스 생성")
        void createWorkspace_Success() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/workspaces/study/{studyId}", study2.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.studyId").value(study2.getId()))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("실패 - 이미 워크스페이스가 존재")
        void createWorkspace_AlreadyExists() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/workspaces/study/{studyId}", study1.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("이미")));
        }
    }

    @Nested
    @DisplayName("워크스페이스 조회")
    class GetWorkspace {

        @Test
        @DisplayName("성공 - ID로 조회")
        void getWorkspace_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(workspace.getId()))
                    .andExpect(jsonPath("$.studyId").value(study1.getId()));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스")
        void getWorkspace_NotFound() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 99999L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("찾을 수 없습니다")));
        }

        @Test
        @DisplayName("성공 - 스터디 ID로 조회")
        void getWorkspaceByStudyId_Success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}", study1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(workspace.getId()))
                    .andExpect(jsonPath("$.studyId").value(study1.getId()));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void getWorkspaceByStudyId_NotFound() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}", 99999L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("찾을 수 없습니다")));
        }
    }

    @Nested
    @DisplayName("워크스페이스 존재 여부 확인")
    class ExistsWorkspace {

        @Test
        @DisplayName("성공 - 존재하는 경우")
        void existsWorkspace_True() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}/exists", study1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("성공 - 존재하지 않는 경우")
        void existsWorkspace_False() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}/exists", study2.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("워크스페이스 삭제")
    class DeleteWorkspace {

        @Test
        @DisplayName("성공 - ID로 삭제")
        void deleteWorkspace_Success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", workspace.getId())
                            .header("User-Id", user.getId()))
                    .andExpect(status().isNoContent());

            // 삭제 확인
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스")
        void deleteWorkspace_NotFound() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", 99999L)
                            .header("User-Id", user.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("찾을 수 없습니다")));
        }

        @Test
        @DisplayName("성공 - 스터디 ID로 삭제")
        void deleteWorkspaceByStudyId_Success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/study/{studyId}", study1.getId())
                            .header("User-Id", user.getId()))
                    .andExpect(status().isNoContent());

            // 삭제 확인
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}/exists", study1.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void deleteWorkspaceByStudyId_NotFound() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/study/{studyId}", 99999L)
                            .header("User-Id", user.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("찾을 수 없습니다")));
        }
    }
}
