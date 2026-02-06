package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * StudyCommentRepository 테스트
 */
 @SpringBootTest
 @Transactional
 class StudyCommentRepositoryTest {

    @Autowired
    private StudyCommentRepository commentRepository;

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
    private User user3;
    private User user4;
    private Study study1;
    private Study study2;
    private Topic topic1;
    private Topic topic2;
    private Format format;
    private StudyComment parentComment1;
    private StudyComment parentComment2;
    private StudyComment replyComment1;
    private StudyComment replyComment2;
    private StudyComment deletedComment;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic1 = topicRepository.save(Topic.builder()
                .name("Java")
                .sortOrder(1)
                .build());
        topic2 = topicRepository.save(Topic.builder()
                .name("Spring")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("코드 리뷰")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. User 엔티티 생성
        user1 = userRepository.save(User.builder()
                .userId("testuser1")
                .email("user1@test.com")
                .nickname("테스트유저1")
                .name("유저1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user2 = userRepository.save(User.builder()
                .userId("testuser2")
                .email("user2@test.com")
                .nickname("테스트유저2")
                .name("유저2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user3 = userRepository.save(User.builder()
                .userId("testuser3")
                .email("user3@test.com")
                .nickname("테스트유저3")
                .name("유저3")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user4 = userRepository.save(User.builder()
                .userId("testuser4")
                .email("user4@test.com")
                .nickname("테스트유저4")
                .name("유저4")
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

        // 4. Study 엔티티 생성
        study1 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("테스트 스터디 1")
                .topic(topic1)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());

        study2 = studyRepository.save(Study.builder()
                .leaderId(user2.getId())
                .name("테스트 스터디 2")
                .topic(topic2)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());

        studyRepository.flush();

        // 5. 스터디 1의 최상위 댓글들
        parentComment1 = commentRepository.save(StudyComment.builder()
                .studyId(study1.getId())
                .userId(user1.getId())
                .content("첫 번째 댓글입니다.")
                .build());

        parentComment2 = commentRepository.save(StudyComment.builder()
                .studyId(study1.getId())
                .userId(user2.getId())
                .content("두 번째 댓글입니다.")
                .imageUrl("https://example.com/image.png")
                .build());

        commentRepository.flush();

        // 6. 첫 번째 댓글의 대댓글들
        replyComment1 = commentRepository.save(StudyComment.builder()
                .studyId(study1.getId())
                .userId(user3.getId())
                .parentId(parentComment1.getId())
                .content("첫 번째 댓글에 대한 답글입니다.")
                .build());

        replyComment2 = commentRepository.save(StudyComment.builder()
                .studyId(study1.getId())
                .userId(user1.getId())
                .parentId(parentComment1.getId())
                .content("작성자의 답글입니다.")
                .build());

        commentRepository.flush();

        // 7. 삭제된 댓글 (스터디 1)
        deletedComment = StudyComment.builder()
                .studyId(study1.getId())
                .userId(user4.getId())
                .content("삭제된 댓글입니다.")
                .build();
        deletedComment.delete();
        deletedComment = commentRepository.save(deletedComment);
        commentRepository.flush();
    }

    // ============================================================
    // 댓글 조회 테스트 (스터디별)
    // ============================================================

    @Test
    @DisplayName("스터디별 최상위 댓글 조회 - 삭제되지 않은 것만")
    void findParentCommentsByStudyId_Success() {
        // given
        Long studyId = study1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyComment> result = commentRepository.findParentCommentsByStudyId(studyId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(comment -> comment.getParentId() == null);
        assertThat(result.getContent()).allMatch(comment -> !comment.getIsDeleted());
    }

    @Test
    @DisplayName("스터디별 최상위 댓글 조회 - 삭제된 것 포함")
    void findAllParentCommentsByStudyId_Success() {
        // given
        Long studyId = study1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyComment> result = commentRepository.findAllParentCommentsByStudyId(studyId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 삭제된 댓글 포함
        assertThat(result.getContent()).allMatch(comment -> comment.getParentId() == null);
    }

    @Test
    @DisplayName("스터디별 전체 댓글 조회 - 리스트")
    void findByStudyIdAndIsDeletedFalse_Success() {
        // given
        Long studyId = study1.getId();

        // when
        List<StudyComment> result = commentRepository.findByStudyIdAndIsDeletedFalseOrderByCreatedAtAsc(studyId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4); // 삭제되지 않은 댓글만
        assertThat(result).allMatch(comment -> !comment.getIsDeleted());
    }

    // ============================================================
    // 대댓글 조회 테스트
    // ============================================================

    @Test
    @DisplayName("부모 댓글의 대댓글 조회 - 삭제되지 않은 것만")
    void findRepliesByParentId_Success() {
        // given
        Long parentId = parentComment1.getId();

        // when
        List<StudyComment> result = commentRepository.findRepliesByParentId(parentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(comment -> comment.getParentId().equals(parentId));
        assertThat(result).allMatch(comment -> !comment.getIsDeleted());
    }

    @Test
    @DisplayName("부모 댓글의 대댓글 조회 - 삭제된 것 포함")
    void findByParentIdOrderByCreatedAtAsc_Success() {
        // given
        Long parentId = parentComment1.getId();

        // when
        List<StudyComment> result = commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(comment -> comment.getParentId().equals(parentId));
    }

    @Test
    @DisplayName("대댓글 개수 조회")
    void countRepliesByParentId_Success() {
        // given
        Long parentId = parentComment1.getId();

        // when
        Long count = commentRepository.countRepliesByParentId(parentId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("대댓글 존재 여부 확인 - 존재함")
    void existsByParentIdAndIsDeletedFalse_Exists() {
        // given
        Long parentId = parentComment1.getId();

        // when
        boolean exists = commentRepository.existsByParentIdAndIsDeletedFalse(parentId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("대댓글 존재 여부 확인 - 존재하지 않음")
    void existsByParentIdAndIsDeletedFalse_NotExists() {
        // given
        Long parentId = parentComment2.getId(); // 대댓글 없는 댓글

        // when
        boolean exists = commentRepository.existsByParentIdAndIsDeletedFalse(parentId);

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // 사용자별 댓글 조회 테스트
    // ============================================================

    @Test
    @DisplayName("사용자별 댓글 조회 - 페이징")
    void findByUserIdAndIsDeletedFalse_Paging_Success() {
        // given
        Long userId = user1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<StudyComment> result = commentRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // parentComment1, replyComment2
        assertThat(result.getContent()).allMatch(comment -> comment.getUserId().equals(userId));
    }

    @Test
    @DisplayName("사용자별 댓글 조회 - 리스트")
    void findByUserIdAndIsDeletedFalse_List_Success() {
        // given
        Long userId = user1.getId();

        // when
        List<StudyComment> result = commentRepository.findByUserIdAndIsDeletedFalse(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(comment -> comment.getUserId().equals(userId));
    }

    // ============================================================
    // 개별 댓글 조회 테스트
    // ============================================================

    @Test
    @DisplayName("삭제되지 않은 댓글 조회 - 존재함")
    void findByIdAndIsDeletedFalse_Exists() {
        // given
        Long commentId = parentComment1.getId();

        // when
        Optional<StudyComment> result = commentRepository.findByIdAndIsDeletedFalse(commentId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(commentId);
    }

    @Test
    @DisplayName("삭제되지 않은 댓글 조회 - 삭제된 댓글")
    void findByIdAndIsDeletedFalse_Deleted() {
        // given
        Long commentId = deletedComment.getId();

        // when
        Optional<StudyComment> result = commentRepository.findByIdAndIsDeletedFalse(commentId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스터디 + 댓글 ID로 조회")
    void findByIdAndStudyIdAndIsDeletedFalse_Success() {
        // given
        Long commentId = parentComment1.getId();
        Long studyId = study1.getId();

        // when
        Optional<StudyComment> result = commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStudyId()).isEqualTo(studyId);
    }

    @Test
    @DisplayName("스터디 + 댓글 ID로 조회 - 다른 스터디")
    void findByIdAndStudyIdAndIsDeletedFalse_WrongStudy() {
        // given
        Long commentId = parentComment1.getId();
        Long wrongStudyId = study2.getId();

        // when
        Optional<StudyComment> result = commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, wrongStudyId);

        // then
        assertThat(result).isEmpty();
    }

    // ============================================================
    // 통계 테스트
    // ============================================================

    @Test
    @DisplayName("스터디별 전체 댓글 개수")
    void countByStudyIdAndIsDeletedFalse_Success() {
        // given
        Long studyId = study1.getId();

        // when
        Long count = commentRepository.countByStudyIdAndIsDeletedFalse(studyId);

        // then
        assertThat(count).isEqualTo(4); // 삭제되지 않은 댓글만
    }

    @Test
    @DisplayName("스터디별 최상위 댓글 개수")
    void countParentCommentsByStudyId_Success() {
        // given
        Long studyId = study1.getId();

        // when
        Long count = commentRepository.countParentCommentsByStudyId(studyId);

        // then
        assertThat(count).isEqualTo(2); // 삭제되지 않은 최상위 댓글만
    }

    @Test
    @DisplayName("사용자별 댓글 개수")
    void countByUserIdAndIsDeletedFalse_Success() {
        // given
        Long userId = user1.getId();

        // when
        Long count = commentRepository.countByUserIdAndIsDeletedFalse(userId);

        // then
        assertThat(count).isEqualTo(2);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("댓글 생성")
    void save_Success() {
        // given
        StudyComment newComment = StudyComment.builder()
                .studyId(study2.getId())
                .userId(user2.getId())
                .content("새로운 댓글입니다.")
                .build();

        // when
        StudyComment saved = commentRepository.save(newComment);
        commentRepository.flush();

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyId()).isEqualTo(study2.getId());
        assertThat(saved.getUserId()).isEqualTo(user2.getId());
        assertThat(saved.getContent()).isEqualTo("새로운 댓글입니다.");
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("대댓글 생성")
    void save_Reply_Success() {
        // given
        StudyComment reply = StudyComment.builder()
                .studyId(study1.getId())
                .userId(user2.getId())
                .parentId(parentComment1.getId())
                .content("새로운 대댓글입니다.")
                .build();

        // when
        StudyComment saved = commentRepository.save(reply);
        commentRepository.flush();

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getParentId()).isEqualTo(parentComment1.getId());
        assertThat(saved.isReply()).isTrue();
    }

    @Test
    @DisplayName("이미지 첨부 댓글 생성")
    void save_WithImage_Success() {
        // given
        StudyComment commentWithImage = StudyComment.builder()
                .studyId(study1.getId())
                .userId(user2.getId())
                .content("이미지 첨부 댓글입니다.")
                .imageUrl("https://example.com/new-image.png")
                .build();

        // when
        StudyComment saved = commentRepository.save(commentWithImage);
        commentRepository.flush();

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getImageUrl()).isEqualTo("https://example.com/new-image.png");
    }

    @Test
    @DisplayName("댓글 수정")
    void update_Success() {
        // given
        StudyComment comment = parentComment1;
        String newContent = "수정된 댓글 내용입니다.";

        // when
        comment.updateContent(newContent);
        StudyComment updated = commentRepository.save(comment);
        commentRepository.flush();

        // then
        assertThat(updated.getContent()).isEqualTo(newContent);
    }

    @Test
    @DisplayName("댓글 Soft Delete")
    void softDelete_Success() {
        // given
        StudyComment comment = parentComment1;

        // when
        comment.delete();
        StudyComment deleted = commentRepository.save(comment);
        commentRepository.flush();

        // then
        assertThat(deleted.getIsDeleted()).isTrue();

        // 삭제된 댓글은 조회되지 않아야 함
        Optional<StudyComment> result = commentRepository.findByIdAndIsDeletedFalse(comment.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("댓글 Hard Delete")
    void hardDelete_Success() {
        // given
        Long commentId = parentComment2.getId();

        // when
        commentRepository.delete(parentComment2);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<StudyComment> result = commentRepository.findById(commentId);
        assertThat(result).isEmpty();
    }

    // ============================================================
    // 비즈니스 로직 테스트
    // ============================================================

    @Test
    @DisplayName("대댓글 여부 확인 - 대댓글")
    void isReply_True() {
        // given
        StudyComment reply = replyComment1;

        // when & then
        assertThat(reply.isReply()).isTrue();
    }

    @Test
    @DisplayName("대댓글 여부 확인 - 최상위 댓글")
    void isReply_False() {
        // given
        StudyComment parent = parentComment1;

        // when & then
        assertThat(parent.isReply()).isFalse();
    }

    @Test
    @DisplayName("작성자 본인 확인 - 본인")
    void isAuthor_True() {
        // given
        StudyComment comment = parentComment1;
        Long authorId = user1.getId();

        // when & then
        assertThat(comment.isAuthor(authorId)).isTrue();
    }

    @Test
    @DisplayName("작성자 본인 확인 - 타인")
    void isAuthor_False() {
        // given
        StudyComment comment = parentComment1;
        Long otherId = user4.getId();

        // when & then
        assertThat(comment.isAuthor(otherId)).isFalse();
    }
}
