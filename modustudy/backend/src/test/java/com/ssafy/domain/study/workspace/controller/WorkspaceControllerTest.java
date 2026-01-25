package com.ssafy.domain.study.workspace.controller;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("WorkspaceController 테스트")
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
    private EntityManager entityManager;

    private User user;
    private Study study;
    private Study studyWithWorkspace;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        // 1. User 생성
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

        // 2. Study 생성 (워크스페이스 없음)
        study = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("테스트 스터디")
                .topic("Java")
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 3. Study 생성 (워크스페이스 있음)
        studyWithWorkspace = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("워크스페이스 있는 스터디")
                .topic("Spring")
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 4. Workspace 생성
        workspace = workspaceRepository.save(Workspace.create(studyWithWorkspace.getId()));
        workspaceRepository.flush();
    }

    @Nested
    @DisplayName("워크스페이스 생성 API")
    class CreateWorkspace {

        @Test
        @DisplayName("워크스페이스 생성 성공 - 201 Created")
        void createWorkspace_Success() throws Exception {
            mockMvc.perform(post("/api/v1/workspaces/study/{studyId}", study.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.studyId").value(study.getId()))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("이미 워크스페이스가 존재하면 400 에러")
        void createWorkspace_AlreadyExists() throws Exception {
            mockMvc.perform(post("/api/v1/workspaces/study/{studyId}", studyWithWorkspace.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("워크스페이스 조회 API")
    class GetWorkspace {

        @Test
        @DisplayName("ID로 워크스페이스 조회 성공 - 200 OK")
        void getWorkspace_Success() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspace.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(workspace.getId()))
                    .andExpect(jsonPath("$.studyId").value(studyWithWorkspace.getId()));
        }

        @Test
        @DisplayName("존재하지 않는 워크스페이스 조회 시 400 에러")
        void getWorkspace_NotFound() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", 999999L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("스터디 ID로 워크스페이스 조회 성공 - 200 OK")
        void getWorkspaceByStudyId_Success() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}", studyWithWorkspace.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(workspace.getId()))
                    .andExpect(jsonPath("$.studyId").value(studyWithWorkspace.getId()));
        }

        @Test
        @DisplayName("워크스페이스가 없는 스터디 조회 시 400 에러")
        void getWorkspaceByStudyId_NotFound() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}", study.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("워크스페이스 존재 여부 확인 API")
    class ExistsWorkspace {

        @Test
        @DisplayName("워크스페이스가 존재하면 true 반환")
        void existsWorkspace_True() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}/exists", studyWithWorkspace.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("워크스페이스가 없으면 false 반환")
        void existsWorkspace_False() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/study/{studyId}/exists", study.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("워크스페이스 삭제 API")
    class DeleteWorkspace {

        @Test
        @DisplayName("워크스페이스 삭제 성공 - 204 No Content")
        void deleteWorkspace_Success() throws Exception {
            // given - 삭제용 워크스페이스 생성
            Study deleteStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("삭제용 스터디")
                    .topic("Delete")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            Workspace deleteWorkspace = workspaceRepository.save(Workspace.create(deleteStudy.getId()));
            workspaceRepository.flush();

            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", deleteWorkspace.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 워크스페이스 삭제 시 400 에러")
        void deleteWorkspace_NotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", 999999L)
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("스터디 ID로 워크스페이스 삭제 성공 - 204 No Content")
        void deleteWorkspaceByStudyId_Success() throws Exception {
            // given - 삭제용 워크스페이스 생성
            Study deleteStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("삭제용 스터디2")
                    .topic("Delete2")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            Workspace deleteWorkspace = workspaceRepository.save(Workspace.create(deleteStudy.getId()));
            workspaceRepository.flush();

            // when & then
            mockMvc.perform(delete("/api/v1/workspaces/study/{studyId}", deleteStudy.getId())
                            .header("User-Id", user.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }
    }
}