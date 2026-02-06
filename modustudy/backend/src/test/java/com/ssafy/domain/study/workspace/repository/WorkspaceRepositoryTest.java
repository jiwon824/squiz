package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
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

/**
 * WorkspaceRepository 통합 테스트
 */
 @SpringBootTest
 @Transactional
 class WorkspaceRepositoryTest {

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

        // 5. Workspace 생성
        workspace = workspaceRepository.save(Workspace.create(study1.getId()));
        workspaceRepository.flush();
    }

    @Nested
    @DisplayName("워크스페이스 생성")
    class CreateWorkspace {

        @Test
        @DisplayName("성공 - 워크스페이스 생성")
        void createWorkspace_Success() {
            // given
            Workspace newWorkspace = Workspace.create(study2.getId());

            // when
            Workspace saved = workspaceRepository.save(newWorkspace);
            workspaceRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStudyId()).isEqualTo(study2.getId());
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 정적 팩토리 메서드로 생성")
        void createWorkspace_WithFactoryMethod() {
            // when
            Workspace created = Workspace.create(study2.getId());
            Workspace saved = workspaceRepository.save(created);
            workspaceRepository.flush();

            // then
            assertThat(saved.getStudyId()).isEqualTo(study2.getId());
        }
    }

    @Nested
    @DisplayName("워크스페이스 조회")
    class FindWorkspace {

        @Test
        @DisplayName("성공 - ID로 조회")
        void findById_Success() {
            // when
            Optional<Workspace> found = workspaceRepository.findById(workspace.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getStudyId()).isEqualTo(study1.getId());
        }

        @Test
        @DisplayName("성공 - 스터디 ID로 조회")
        void findByStudyId_Success() {
            // when
            Optional<Workspace> found = workspaceRepository.findByStudyId(study1.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(workspace.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디 ID")
        void findByStudyId_NotFound() {
            // when
            Optional<Workspace> found = workspaceRepository.findByStudyId(99999L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("워크스페이스 존재 여부 확인")
    class ExistsWorkspace {

        @Test
        @DisplayName("성공 - 존재하는 경우")
        void existsByStudyId_True() {
            // when
            boolean exists = workspaceRepository.existsByStudyId(study1.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공 - 존재하지 않는 경우")
        void existsByStudyId_False() {
            // when
            boolean exists = workspaceRepository.existsByStudyId(study2.getId());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("워크스페이스 삭제")
    class DeleteWorkspace {

        @Test
        @DisplayName("성공 - ID로 삭제")
        void deleteById_Success() {
            // given
            Long workspaceId = workspace.getId();

            // when
            workspaceRepository.deleteById(workspaceId);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<Workspace> found = workspaceRepository.findById(workspaceId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("성공 - 스터디 ID로 삭제")
        void deleteByStudyId_Success() {
            // given
            Long studyId = study1.getId();

            // when
            workspaceRepository.deleteByStudyId(studyId);
            entityManager.flush();
            entityManager.clear();

            // then
            boolean exists = workspaceRepository.existsByStudyId(studyId);
            assertThat(exists).isFalse();
        }
    }
}
