package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.workspace.entity.Workspace;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("WorkspaceRepository 테스트")
class WorkspaceRepositoryTest {

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

        // 2. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("테스트 스터디")
                .topic("Java")
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 3. Workspace 생성
        workspace = workspaceRepository.save(Workspace.create(study.getId()));
        workspaceRepository.flush();
    }

    @Nested
    @DisplayName("워크스페이스 생성")
    class CreateWorkspace {

        @Test
        @DisplayName("워크스페이스 생성 성공")
        void createWorkspace_Success() {
            // given
            Study newStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("새로운 스터디")
                    .topic("Spring")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            // when
            Workspace newWorkspace = workspaceRepository.save(Workspace.create(newStudy.getId()));
            workspaceRepository.flush();

            // then
            assertThat(newWorkspace.getId()).isNotNull();
            assertThat(newWorkspace.getStudyId()).isEqualTo(newStudy.getId());
            assertThat(newWorkspace.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("정적 팩토리 메서드로 워크스페이스 생성")
        void createWorkspace_WithFactoryMethod() {
            // given
            Study newStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("팩토리 메서드 테스트")
                    .topic("Kotlin")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            // when
            Workspace newWorkspace = Workspace.create(newStudy.getId());
            Workspace savedWorkspace = workspaceRepository.save(newWorkspace);
            workspaceRepository.flush();

            // then
            assertThat(savedWorkspace.getStudyId()).isEqualTo(newStudy.getId());
        }
    }

    @Nested
    @DisplayName("워크스페이스 조회")
    class FindWorkspace {

        @Test
        @DisplayName("ID로 워크스페이스 조회 성공")
        void findById_Success() {
            // when
            Optional<Workspace> found = workspaceRepository.findById(workspace.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getStudyId()).isEqualTo(study.getId());
        }

        @Test
        @DisplayName("스터디 ID로 워크스페이스 조회 성공")
        void findByStudyId_Success() {
            // when
            Optional<Workspace> found = workspaceRepository.findByStudyId(study.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(workspace.getId());
        }

        @Test
        @DisplayName("존재하지 않는 스터디 ID로 조회 시 빈 Optional 반환")
        void findByStudyId_NotFound() {
            // when
            Optional<Workspace> found = workspaceRepository.findByStudyId(999999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("워크스페이스 존재 여부 확인")
    class ExistsWorkspace {

        @Test
        @DisplayName("스터디 ID로 존재 여부 확인 - 존재하는 경우")
        void existsByStudyId_True() {
            // when
            boolean exists = workspaceRepository.existsByStudyId(study.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("스터디 ID로 존재 여부 확인 - 존재하지 않는 경우")
        void existsByStudyId_False() {
            // when
            boolean exists = workspaceRepository.existsByStudyId(999999L);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("워크스페이스 삭제")
    class DeleteWorkspace {

        @Test
        @DisplayName("ID로 워크스페이스 삭제 성공")
        void deleteById_Success() {
            // given - 삭제 테스트용 새로운 Study와 Workspace 생성
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
            workspaceRepository.deleteById(workspaceId);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<Workspace> found = workspaceRepository.findById(workspaceId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("스터디 ID로 워크스페이스 삭제 성공")
        void deleteByStudyId_Success() {
            // given - 삭제 테스트용 새로운 Study와 Workspace 생성
            Study deleteStudy = studyRepository.save(Study.builder()
                    .leaderId(user.getId())
                    .name("삭제용 스터디2")
                    .topic("Delete2")
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            Workspace deleteWorkspace = workspaceRepository.save(Workspace.create(deleteStudy.getId()));
            workspaceRepository.flush();

            Long studyId = deleteStudy.getId();

            // when
            workspaceRepository.deleteByStudyId(studyId);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<Workspace> found = workspaceRepository.findByStudyId(studyId);
            assertThat(found).isEmpty();
        }
    }
}