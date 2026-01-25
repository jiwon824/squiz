package com.ssafy.domain.study.workspace.repository;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.entity.WorkspaceType;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
    private EntityManager entityManager;

    private User user;
    private Study study;
    private Study study2;

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

        study2 = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("테스트 스터디2")
                .topic("Spring")
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();
    }

    // ============================================================
    // 스터디별 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("스터디별 조회 테스트")
    class FindByStudyIdTest {

        @Test
        @DisplayName("스터디의 모든 워크스페이스 조회")
        void findByStudyId_Success() {
            // given
            Workspace workspace1 = workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());

            Workspace workspace2 = workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("음성채팅")
                    .type(WorkspaceType.VOICE)
                    .build());

            // 다른 스터디의 워크스페이스
            workspaceRepository.save(Workspace.builder()
                    .studyId(study2.getId())
                    .name("다른스터디")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            List<Workspace> result = workspaceRepository.findByStudyId(study.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("name")
                    .containsExactlyInAnyOrder("일반", "음성채팅");
        }

        @Test
        @DisplayName("스터디에 워크스페이스가 없으면 빈 리스트 반환")
        void findByStudyId_Empty() {
            // when
            List<Workspace> result = workspaceRepository.findByStudyId(study.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("스터디의 특정 타입 워크스페이스 조회")
        void findByStudyIdAndType_Success() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());

            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("공지")
                    .type(WorkspaceType.TEXT)
                    .build());

            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("음성채팅")
                    .type(WorkspaceType.VOICE)
                    .build());
            workspaceRepository.flush();

            // when
            List<Workspace> textChannels = workspaceRepository.findByStudyIdAndType(
                    study.getId(), WorkspaceType.TEXT);
            List<Workspace> voiceChannels = workspaceRepository.findByStudyIdAndType(
                    study.getId(), WorkspaceType.VOICE);

            // then
            assertThat(textChannels).hasSize(2);
            assertThat(voiceChannels).hasSize(1);
        }
    }

    // ============================================================
    // 개별 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("개별 조회 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("워크스페이스 ID와 스터디 ID로 조회")
        void findByIdAndStudyId_Success() {
            // given
            Workspace workspace = workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            Optional<Workspace> result = workspaceRepository.findByIdAndStudyId(
                    workspace.getId(), study.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("일반");
        }

        @Test
        @DisplayName("다른 스터디 ID로 조회하면 빈 Optional 반환")
        void findByIdAndStudyId_WrongStudyId() {
            // given
            Workspace workspace = workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            Optional<Workspace> result = workspaceRepository.findByIdAndStudyId(
                    workspace.getId(), study2.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("스터디 ID와 이름으로 조회")
        void findByStudyIdAndName_Success() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            Optional<Workspace> result = workspaceRepository.findByStudyIdAndName(
                    study.getId(), "일반");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(WorkspaceType.TEXT);
        }

        @Test
        @DisplayName("존재하지 않는 이름으로 조회하면 빈 Optional 반환")
        void findByStudyIdAndName_NotFound() {
            // when
            Optional<Workspace> result = workspaceRepository.findByStudyIdAndName(
                    study.getId(), "존재하지않는채널");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // 존재 여부 테스트
    // ============================================================

    @Nested
    @DisplayName("존재 여부 테스트")
    class ExistsTest {

        @Test
        @DisplayName("스터디에 워크스페이스 존재 여부 확인 - 존재함")
        void existsByStudyId_True() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            boolean result = workspaceRepository.existsByStudyId(study.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("스터디에 워크스페이스 존재 여부 확인 - 존재하지 않음")
        void existsByStudyId_False() {
            // when
            boolean result = workspaceRepository.existsByStudyId(study.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("스터디에 같은 이름의 워크스페이스 존재 여부 확인")
        void existsByStudyIdAndName_True() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            boolean exists = workspaceRepository.existsByStudyIdAndName(study.getId(), "일반");
            boolean notExists = workspaceRepository.existsByStudyIdAndName(study.getId(), "공지");

            // then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    // ============================================================
    // 통계 테스트
    // ============================================================

    @Nested
    @DisplayName("통계 테스트")
    class CountTest {

        @Test
        @DisplayName("스터디의 워크스페이스 개수 조회")
        void countByStudyId_Success() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("음성채팅")
                    .type(WorkspaceType.VOICE)
                    .build());
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("화상회의")
                    .type(WorkspaceType.VIDEO)
                    .build());
            workspaceRepository.flush();

            // when
            long count = workspaceRepository.countByStudyId(study.getId());

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("스터디의 특정 타입 워크스페이스 개수 조회")
        void countByStudyIdAndType_Success() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("공지")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("음성채팅")
                    .type(WorkspaceType.VOICE)
                    .build());
            workspaceRepository.flush();

            // when
            long textCount = workspaceRepository.countByStudyIdAndType(
                    study.getId(), WorkspaceType.TEXT);
            long voiceCount = workspaceRepository.countByStudyIdAndType(
                    study.getId(), WorkspaceType.VOICE);
            long videoCount = workspaceRepository.countByStudyIdAndType(
                    study.getId(), WorkspaceType.VIDEO);

            // then
            assertThat(textCount).isEqualTo(2);
            assertThat(voiceCount).isEqualTo(1);
            assertThat(videoCount).isEqualTo(0);
        }
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Nested
    @DisplayName("삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("스터디의 모든 워크스페이스 삭제")
        void deleteByStudyId_Success() {
            // given
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("음성채팅")
                    .type(WorkspaceType.VOICE)
                    .build());

            // 다른 스터디 워크스페이스
            workspaceRepository.save(Workspace.builder()
                    .studyId(study2.getId())
                    .name("다른스터디")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            workspaceRepository.deleteByStudyId(study.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            List<Workspace> study1Workspaces = workspaceRepository.findByStudyId(study.getId());
            List<Workspace> study2Workspaces = workspaceRepository.findByStudyId(study2.getId());

            assertThat(study1Workspaces).isEmpty();
            assertThat(study2Workspaces).hasSize(1);
        }
    }

    // ============================================================
    // CRUD 테스트
    // ============================================================

    @Nested
    @DisplayName("CRUD 테스트")
    class CrudTest {

        @Test
        @DisplayName("워크스페이스 저장")
        void save_Success() {
            // given
            Workspace workspace = Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .description("일반 채팅방입니다.")
                    .build();

            // when
            Workspace saved = workspaceRepository.save(workspace);
            workspaceRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStudyId()).isEqualTo(study.getId());
            assertThat(saved.getName()).isEqualTo("일반");
            assertThat(saved.getType()).isEqualTo(WorkspaceType.TEXT);
            assertThat(saved.getDescription()).isEqualTo("일반 채팅방입니다.");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("워크스페이스 수정")
        void update_Success() {
            // given
            Workspace workspace = workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();

            // when
            workspace.updateName("공지사항");
            workspace.updateDescription("공지사항 채널입니다.");
            entityManager.flush();
            entityManager.clear();

            // then
            Workspace found = workspaceRepository.findById(workspace.getId()).orElseThrow();
            assertThat(found.getName()).isEqualTo("공지사항");
            assertThat(found.getDescription()).isEqualTo("공지사항 채널입니다.");
        }

        @Test
        @DisplayName("워크스페이스 삭제")
        void delete_Success() {
            // given
            Workspace workspace = workspaceRepository.save(Workspace.builder()
                    .studyId(study.getId())
                    .name("일반")
                    .type(WorkspaceType.TEXT)
                    .build());
            workspaceRepository.flush();
            Long workspaceId = workspace.getId();

            // when
            workspaceRepository.delete(workspace);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<Workspace> found = workspaceRepository.findById(workspaceId);
            assertThat(found).isEmpty();
        }
    }
}