package com.ssafy.domain.material.service;

import com.ssafy.common.exception.MaterialException;
import com.ssafy.domain.material.dto.request.MaterialCommentCreateRequest;
import com.ssafy.domain.material.dto.response.MaterialCommentCreateResponse;
import com.ssafy.domain.material.dto.response.MaterialCommentResponse;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MaterialCommentService 통합 테스트
 *
 * 테스트 규칙:
 * - 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
 * - 부모 엔티티 저장 후 flush() 호출
 * - 벌크 삭제 후 entityManager.flush() + clear() 호출
 */
 @SpringBootTest
 @Transactional
 class MaterialCommentServiceTest {

    @Autowired
    private MaterialCommentService commentService;

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
                .userId("commentServiceUser1")
                .email("commentService1@test.com")
                .nickname("댓글서비스1")
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
                .userId("commentServiceUser2")
                .email("commentService2@test.com")
                .nickname("댓글서비스2")
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
    @DisplayName("댓글 목록 조회 테스트")
    class GetCommentsTest {

        @Test
        @DisplayName("댓글 목록 조회 성공")
        void getComments_Success() {
            // given
            commentRepository.save(MaterialComment.create(material.getId(), user1.getId(), "댓글 1"));
            commentRepository.save(MaterialComment.create(material.getId(), user2.getId(), "댓글 2"));
            commentRepository.flush();

            // when
            List<MaterialCommentResponse> result = commentService.getComments(material.getId());

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("댓글 목록 조회 - 사용자 정보 포함")
        void getComments_ContainsUserInfo_Success() {
            // given
            commentRepository.save(MaterialComment.create(material.getId(), user1.getId(), "테스트 댓글"));
            commentRepository.flush();

            // when
            List<MaterialCommentResponse> result = commentService.getComments(material.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUser()).isNotNull();
            assertThat(result.get(0).getUser().getNickname()).isEqualTo("댓글서비스1");
        }

        @Test
        @DisplayName("댓글 없는 자료 조회 - 빈 목록 반환")
        void getComments_NoComments_ReturnsEmptyList() {
            // when
            List<MaterialCommentResponse> result = commentService.getComments(material.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 자료의 댓글 조회 시 예외 발생")
        void getComments_MaterialNotFound_ThrowsException() {
            // given
            Long nonExistentMaterialId = 99999L;

            // when & then
            assertThatThrownBy(() -> commentService.getComments(nonExistentMaterialId))
                    .isInstanceOf(MaterialException.MaterialNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 작성 테스트")
    class CreateCommentTest {

        @Test
        @DisplayName("댓글 작성 성공")
        void createComment_Success() {
            // given
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .content("좋은 자료 감사합니다!")
                    .build();

            // when
            MaterialCommentCreateResponse result = commentService.createComment(
                    material.getId(), user1.getId(), request);

            // then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getContent()).isEqualTo("좋은 자료 감사합니다!");
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("다른 사용자도 댓글 작성 가능")
        void createComment_ByOtherUser_Success() {
            // given
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .content("저도 도움 받았어요!")
                    .build();

            // when
            MaterialCommentCreateResponse result = commentService.createComment(
                    material.getId(), user2.getId(), request);

            // then
            assertThat(result.getId()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 자료에 댓글 작성 시 예외 발생")
        void createComment_MaterialNotFound_ThrowsException() {
            // given
            Long nonExistentMaterialId = 99999L;
            MaterialCommentCreateRequest request = MaterialCommentCreateRequest.builder()
                    .content("댓글 내용")
                    .build();

            // when & then
            assertThatThrownBy(() ->
                    commentService.createComment(nonExistentMaterialId, user1.getId(), request))
                    .isInstanceOf(MaterialException.MaterialNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 삭제 테스트")
    class DeleteCommentTest {

        @Test
        @DisplayName("본인 댓글 삭제 성공")
        void deleteComment_ByAuthor_Success() {
            // given
            MaterialComment comment = commentRepository.save(
                    MaterialComment.create(material.getId(), user1.getId(), "삭제할 댓글"));
            commentRepository.flush();
            Long commentId = comment.getId();

            // when
            commentService.deleteComment(material.getId(), commentId, user1.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(commentRepository.findById(commentId)).isEmpty();
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 댓글 삭제 시 예외 발생")
        void deleteComment_NotAuthor_ThrowsException() {
            // given
            MaterialComment comment = commentRepository.save(
                    MaterialComment.create(material.getId(), user1.getId(), "user1 댓글"));
            commentRepository.flush();

            // when & then
            assertThatThrownBy(() ->
                    commentService.deleteComment(material.getId(), comment.getId(), user2.getId()))
                    .isInstanceOf(MaterialException.NotCommentAuthorException.class);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시 예외 발생")
        void deleteComment_CommentNotFound_ThrowsException() {
            // given
            Long nonExistentCommentId = 99999L;

            // when & then
            assertThatThrownBy(() ->
                    commentService.deleteComment(material.getId(), nonExistentCommentId, user1.getId()))
                    .isInstanceOf(MaterialException.MaterialCommentNotFoundException.class);
        }

        @Test
        @DisplayName("다른 자료의 댓글 삭제 시 예외 발생")
        void deleteComment_WrongMaterial_ThrowsException() {
            // given
            Material anotherMaterial = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "다른 자료", "설명", "https://test.com/other", 1));
            materialRepository.flush();

            MaterialComment comment = commentRepository.save(
                    MaterialComment.create(anotherMaterial.getId(), user1.getId(), "다른 자료 댓글"));
            commentRepository.flush();

            // when & then - material.getId()로 요청하지만 댓글은 anotherMaterial에 속함
            assertThatThrownBy(() ->
                    commentService.deleteComment(material.getId(), comment.getId(), user1.getId()))
                    .isInstanceOf(MaterialException.MaterialCommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("여러 댓글 시나리오 테스트")
    class MultipleCommentsTest {

        @Test
        @DisplayName("여러 사용자가 댓글 작성 후 목록 조회")
        void multipleUsers_CreateAndGetComments_Success() {
            // given
            commentService.createComment(material.getId(), user1.getId(),
                    MaterialCommentCreateRequest.builder().content("user1 댓글").build());
            commentService.createComment(material.getId(), user2.getId(),
                    MaterialCommentCreateRequest.builder().content("user2 댓글").build());
            commentService.createComment(material.getId(), user1.getId(),
                    MaterialCommentCreateRequest.builder().content("user1 두번째 댓글").build());

            // when
            List<MaterialCommentResponse> result = commentService.getComments(material.getId());

            // then
            assertThat(result).hasSize(3);

            long user1CommentCount = result.stream()
                    .filter(c -> c.getUser().getId().equals(user1.getId()))
                    .count();
            long user2CommentCount = result.stream()
                    .filter(c -> c.getUser().getId().equals(user2.getId()))
                    .count();

            assertThat(user1CommentCount).isEqualTo(2);
            assertThat(user2CommentCount).isEqualTo(1);
        }
    }
}
