package com.ssafy.domain.study.workspace.service;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.workspace.dto.response.WorkspaceResponse;
import com.ssafy.domain.study.workspace.entity.Message;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.repository.MessageRepository;
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

@SpringBootTest
@Transactional
@DisplayName("WorkspaceService 테스트")
class WorkspaceServiceTest {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private MessageRepository messageRepository;

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
    @DisplayName("워크스페이스 생성")
    class CreateWorkspace {

        @Test
        @DisplayName("워크스페이스 생성 성공")
        void createWorkspace_Success() {
            // when
            WorkspaceResponse response = workspaceService.createWorkspace(study.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStudyId()).isEqualTo(study.getId());
            assertThat(response.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 워크스페이스가 존재하면 예외 발생")
        void createWorkspace_AlreadyExists() {
            // when & then
            assertThatThrownBy(() -> workspaceService.createWorkspace(studyWithWorkspace.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 해당 스터디의 워크스페이스가 존재합니다");
        }
    }

    @Nested
    @DisplayName("워크스페이스 조회")
    class GetWorkspace {

        @Test
        @DisplayName("ID로 워크스페이스 조회 성공")
        void getWorkspace_Success() {
            // when
            WorkspaceResponse response = workspaceService.getWorkspace(workspace.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(workspace.getId());
            assertThat(response.getStudyId()).isEqualTo(studyWithWorkspace.getId());
        }

        @Test
        @DisplayName("존재하지 않는 워크스페이스 조회 시 예외 발생")
        void getWorkspace_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.getWorkspace(999999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("스터디 ID로 워크스페이스 조회 성공")
        void getWorkspaceByStudyId_Success() {
            // when
            WorkspaceResponse response = workspaceService.getWorkspaceByStudyId(studyWithWorkspace.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(workspace.getId());
            assertThat(response.getStudyId()).isEqualTo(studyWithWorkspace.getId());
        }

        @Test
        @DisplayName("워크스페이스가 없는 스터디 ID로 조회 시 예외 발생")
        void getWorkspaceByStudyId_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.getWorkspaceByStudyId(study.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 스터디의 워크스페이스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("워크스페이스 존재 여부 확인")
    class ExistsWorkspace {

        @Test
        @DisplayName("워크스페이스가 존재하면 true 반환")
        void existsWorkspace_True() {
            // when
            boolean exists = workspaceService.existsWorkspace(studyWithWorkspace.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("워크스페이스가 없으면 false 반환")
        void existsWorkspace_False() {
            // when
            boolean exists = workspaceService.existsWorkspace(study.getId());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("워크스페이스 삭제")
    class DeleteWorkspace {

        @Test
        @DisplayName("워크스페이스 삭제 성공")
        void deleteWorkspace_Success() {
            // given
            Study deleteStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("삭제용 스터디")
                    .topic("Delete")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            Workspace deleteWorkspace = workspaceRepository.save(Workspace.create(deleteStudy.getId()));
            workspaceRepository.flush();

            Long workspaceId = deleteWorkspace.getId();

            // when
            workspaceService.deleteWorkspace(workspaceId);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(workspaceRepository.findById(workspaceId)).isEmpty();
        }

        @Test
        @DisplayName("워크스페이스 삭제 시 메시지도 함께 삭제")
        void deleteWorkspace_WithMessages() {
            // given
            Study deleteStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("삭제용 스터디2")
                    .topic("Delete2")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            Workspace deleteWorkspace = workspaceRepository.save(Workspace.create(deleteStudy.getId()));
            workspaceRepository.flush();

            // 메시지 추가
            messageRepository.save(Message.createTextMessage(
                    deleteWorkspace.getId(), user.getId(), "테스트 메시지1"));
            messageRepository.save(Message.createTextMessage(
                    deleteWorkspace.getId(), user.getId(), "테스트 메시지2"));
            messageRepository.flush();

            Long workspaceId = deleteWorkspace.getId();

            // when
            workspaceService.deleteWorkspace(workspaceId);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(workspaceRepository.findById(workspaceId)).isEmpty();
            assertThat(messageRepository.countByWorkspaceId(workspaceId)).isEqualTo(0);
        }

        @Test
        @DisplayName("존재하지 않는 워크스페이스 삭제 시 예외 발생")
        void deleteWorkspace_NotFound() {
            // when & then
            assertThatThrownBy(() -> workspaceService.deleteWorkspace(999999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("워크스페이스를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("스터디 ID로 워크스페이스 삭제 성공")
        void deleteWorkspaceByStudyId_Success() {
            // given
            Study deleteStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("삭제용 스터디3")
                    .topic("Delete3")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            Workspace deleteWorkspace = workspaceRepository.save(Workspace.create(deleteStudy.getId()));
            workspaceRepository.flush();

            Long studyId = deleteStudy.getId();

            // when
            workspaceService.deleteWorkspaceByStudyId(studyId);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(workspaceRepository.findByStudyId(studyId)).isEmpty();
        }
    }
}