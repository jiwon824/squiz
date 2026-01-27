package com.ssafy.domain.material.repository;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialComment;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MaterialCommentRepository 통합 테스트
 *
 * 테스트 규칙:
 * - 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
 * - 부모 엔티티 저장 후 flush() 호출
 * - 벌크 삭제 후 entityManager.flush() + clear() 호출
 */
@SpringBootTest
@Transactional
class MaterialCommentRepositoryTest {

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
    private Material material1;
    private Material material2;
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
        user1 = userRepository.save(User.builder()
                .userId("commentTestUser1")
                .email("comment1@test.com")
                .nickname("댓글테스트1")
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
                .userId("commentTestUser2")
                .email("comment2@test.com")
                .nickname("댓글테스트2")
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
        material1 = materialRepository.save(Material.createLinkMaterial(
                study.getId(),
                user1.getId(),
                "DP 개념 정리",
                "다이나믹 프로그래밍 기초",
                "https://test.com/dp",
                1
        ));
        materialRepository.flush();

        material2 = materialRepository.save(Material.createLinkMaterial(
                study.getId(),
                user1.getId(),
                "그래프 탐색",
                "DFS/BFS 정리",
                "https://test.com/graph",
                2
        ));
        materialRepository.flush();
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTest {

        @Test
        @DisplayName("댓글 저장 성공")
        void save_Success() {
            // given
            MaterialComment comment = MaterialComment.create(
                    material1.getId(),
                    user1.getId(),
                    "정리 감사합니다!"
            );

            // when
            MaterialComment saved = commentRepository.save(comment);
            commentRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMaterialId()).isEqualTo(material1.getId());
            assertThat(saved.getUserId()).isEqualTo(user1.getId());
            assertThat(saved.getContent()).isEqualTo("정리 감사합니다!");
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("댓글 ID로 조회 성공")
        void findById_Success() {
            // given
            MaterialComment comment = commentRepository.save(MaterialComment.create(
                    material1.getId(),
                    user1.getId(),
                    "테스트 댓글"
            ));
            commentRepository.flush();

            // when
            Optional<MaterialComment> found = commentRepository.findById(comment.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getContent()).isEqualTo("테스트 댓글");
        }

        @Test
        @DisplayName("댓글 내용 수정 성공")
        void updateContent_Success() {
            // given
            MaterialComment comment = commentRepository.save(MaterialComment.create(
                    material1.getId(),
                    user1.getId(),
                    "원래 내용"
            ));
            commentRepository.flush();

            // when
            comment.updateContent("수정된 내용");
            entityManager.flush();
            entityManager.clear();

            // then
            MaterialComment found = commentRepository.findById(comment.getId()).orElseThrow();
            assertThat(found.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("댓글 삭제 성공")
        void delete_Success() {
            // given
            MaterialComment comment = commentRepository.save(MaterialComment.create(
                    material1.getId(),
                    user1.getId(),
                    "삭제할 댓글"
            ));
            commentRepository.flush();
            Long commentId = comment.getId();

            // when
            commentRepository.delete(comment);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<MaterialComment> found = commentRepository.findById(commentId);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("자료 ID 기반 조회 테스트")
    class FindByMaterialIdTest {

        @Test
        @DisplayName("자료 ID로 댓글 목록 조회")
        void findByMaterialId_Success() {
            // given
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "댓글 1"));
            commentRepository.save(MaterialComment.create(material1.getId(), user2.getId(), "댓글 2"));
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "댓글 3"));
            commentRepository.save(MaterialComment.create(material2.getId(), user1.getId(), "다른 자료 댓글"));
            commentRepository.flush();

            // when
            List<MaterialComment> comments = commentRepository.findByMaterialId(material1.getId());

            // then
            assertThat(comments).hasSize(3);
            assertThat(comments).allMatch(c -> c.getMaterialId().equals(material1.getId()));
        }

        @Test
        @DisplayName("자료 ID로 댓글 개수 조회")
        void countByMaterialId_Success() {
            // given
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "댓글 1"));
            commentRepository.save(MaterialComment.create(material1.getId(), user2.getId(), "댓글 2"));
            commentRepository.save(MaterialComment.create(material2.getId(), user1.getId(), "다른 자료 댓글"));
            commentRepository.flush();

            // when
            long count = commentRepository.countByMaterialId(material1.getId());

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("자료 ID + 댓글 ID로 존재 여부 확인")
        void existsByIdAndMaterialId_Success() {
            // given
            MaterialComment comment = commentRepository.save(MaterialComment.create(
                    material1.getId(), user1.getId(), "테스트 댓글"));
            commentRepository.flush();

            // when
            boolean exists = commentRepository.existsByIdAndMaterialId(comment.getId(), material1.getId());
            boolean notExists = commentRepository.existsByIdAndMaterialId(comment.getId(), material2.getId());

            // then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    @Nested
    @DisplayName("사용자 ID 기반 조회 테스트")
    class FindByUserIdTest {

        @Test
        @DisplayName("사용자 ID로 댓글 목록 조회")
        void findByUserId_Success() {
            // given
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "user1 댓글 1"));
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "user1 댓글 2"));
            commentRepository.save(MaterialComment.create(material2.getId(), user1.getId(), "user1 댓글 3"));
            commentRepository.save(MaterialComment.create(material1.getId(), user2.getId(), "user2 댓글"));
            commentRepository.flush();

            // when
            List<MaterialComment> user1Comments = commentRepository.findByUserId(user1.getId());

            // then
            assertThat(user1Comments).hasSize(3);
            assertThat(user1Comments).allMatch(c -> c.getUserId().equals(user1.getId()));
        }

        @Test
        @DisplayName("작성자 확인 - 본인 여부")
        void isAuthor_Test() {
            // given
            MaterialComment comment = commentRepository.save(MaterialComment.create(
                    material1.getId(), user1.getId(), "테스트 댓글"));
            commentRepository.flush();

            // when & then
            assertThat(comment.isAuthor(user1.getId())).isTrue();
            assertThat(comment.isAuthor(user2.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("벌크 삭제 테스트")
    class BulkDeleteTest {

        @Test
        @DisplayName("자료 ID로 댓글 전체 삭제")
        void deleteByMaterialId_Success() {
            // given
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "댓글 1"));
            commentRepository.save(MaterialComment.create(material1.getId(), user2.getId(), "댓글 2"));
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "댓글 3"));
            commentRepository.save(MaterialComment.create(material2.getId(), user1.getId(), "다른 자료 댓글"));
            commentRepository.flush();

            // when
            commentRepository.deleteByMaterialId(material1.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            List<MaterialComment> material1Comments = commentRepository.findByMaterialId(material1.getId());
            List<MaterialComment> material2Comments = commentRepository.findByMaterialId(material2.getId());

            assertThat(material1Comments).isEmpty();
            assertThat(material2Comments).hasSize(1);
        }
    }

    @Nested
    @DisplayName("여러 사용자 댓글 테스트")
    class MultiUserCommentTest {

        @Test
        @DisplayName("여러 사용자가 댓글 작성")
        void multipleUsersComment_Success() {
            // given
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "user1 첫 번째 댓글"));
            commentRepository.save(MaterialComment.create(material1.getId(), user2.getId(), "user2 댓글"));
            commentRepository.save(MaterialComment.create(material1.getId(), user1.getId(), "user1 두 번째 댓글"));
            commentRepository.flush();

            // when
            List<MaterialComment> allComments = commentRepository.findByMaterialId(material1.getId());
            long totalCount = commentRepository.countByMaterialId(material1.getId());

            // then
            assertThat(allComments).hasSize(3);
            assertThat(totalCount).isEqualTo(3);

            // user1 댓글 2개, user2 댓글 1개 확인
            long user1CommentCount = allComments.stream()
                    .filter(c -> c.getUserId().equals(user1.getId()))
                    .count();
            long user2CommentCount = allComments.stream()
                    .filter(c -> c.getUserId().equals(user2.getId()))
                    .count();

            assertThat(user1CommentCount).isEqualTo(2);
            assertThat(user2CommentCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("댓글 없는 경우 테스트")
    class EmptyCommentTest {

        @Test
        @DisplayName("댓글 없는 자료 - 빈 목록 반환")
        void findByMaterialId_NoComments_ReturnsEmptyList() {
            // when
            List<MaterialComment> comments = commentRepository.findByMaterialId(material1.getId());

            // then
            assertThat(comments).isEmpty();
        }

        @Test
        @DisplayName("댓글 없는 자료 - 개수 0 반환")
        void countByMaterialId_NoComments_ReturnsZero() {
            // when
            long count = commentRepository.countByMaterialId(material1.getId());

            // then
            assertThat(count).isEqualTo(0);
        }
    }
}