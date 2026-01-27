package com.ssafy.domain.study.workspace.service;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.study.workspace.dto.response.WorkspaceResponse;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WorkspaceService 통합 테스트
 */
@SpringBootTest
@Transactional
class WorkspaceServiceTest {

    @Autowired
    private WorkspaceService workspaceService;

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
        void createWorkspace_Success() {
            // when
            WorkspaceResponse response = workspaceService.createWorkspace(study2.getId());

            // then
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStudyId()).isEqualTo(study2.getId());
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 이미 워크스페이스가 존재")
        void createWorkspace_AlreadyExists() {
            // when & then
            assertThatThrownBy(() -> workspaceService.createWorkspace(study1.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 해당 스터디의 워크스페이스가 존재합니다");
        }
    }

    @Nested
    @DisplayName("워크스페이스 조회")
    class GetWorkspace {

        @Test
        @DisplayName("성공 - ID로 조회")
        void getWorkspace_Success() {
            // when
            WorkspaceResponse response = workspaceService.getWorkspace(workspace.getId());

            // then
            assertThat(response.getId()).isEqualTo(workspace.getId());
            assertThat(response.getStudyId()).isEqualTo(study1.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스 ID")
        void getWorkspace_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.getWorkspace(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("성공 - 스터디 ID로 조회")
        void getWorkspaceByStudyId_Success() {
            // when
            WorkspaceResponse response = workspaceService.getWorkspaceByStudyId(study1.getId());

            // then
            assertThat(response.getId()).isEqualTo(workspace.getId());
            assertThat(response.getStudyId()).isEqualTo(study1.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디 ID")
        void getWorkspaceByStudyId_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.getWorkspaceByStudyId(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 스터디의 워크스페이스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("워크스페이스 존재 여부 확인")
    class ExistsWorkspace {

        @Test
        @DisplayName("성공 - 존재하는 경우")
        void existsWorkspace_True() {
            // when
            boolean exists = workspaceService.existsWorkspace(study1.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공 - 존재하지 않는 경우")
        void existsWorkspace_False() {
            // when
            boolean exists = workspaceService.existsWorkspace(study2.getId());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("워크스페이스 삭제")
    class DeleteWorkspace {

        @Test
        @DisplayName("성공 - ID로 삭제")
        void deleteWorkspace_Success() {
            // given
            Long workspaceId = workspace.getId();

            // when
            workspaceService.deleteWorkspace(workspaceId);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(workspaceRepository.findById(workspaceId)).isEmpty();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 워크스페이스 ID")
        void deleteWorkspace_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.deleteWorkspace(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("성공 - 스터디 ID로 삭제")
        void deleteWorkspaceByStudyId_Success() {
            // given
            Long studyId = study1.getId();

            // when
            workspaceService.deleteWorkspaceByStudyId(studyId);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(workspaceRepository.existsByStudyId(studyId)).isFalse();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디 ID로 삭제")
        void deleteWorkspaceByStudyId_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.deleteWorkspaceByStudyId(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 스터디의 워크스페이스를 찾을 수 없습니다");
        }
    }
}