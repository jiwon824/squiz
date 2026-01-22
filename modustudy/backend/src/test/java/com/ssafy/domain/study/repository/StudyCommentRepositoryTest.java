package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyComment;
import com.ssafy.domain.study.entity.StudyType;
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

    private Study study1;
    private Study study2;
    private StudyComment parentComment1;
    private StudyComment parentComment2;
    private StudyComment replyComment1;
    private StudyComment replyComment2;
    private StudyComment deletedComment;

    @BeforeEach
    void setUp() {
        // 먼저 Study 엔티티 생성 (외래 키 제약 조건 충족)
        study1 = studyRepository.save(Study.builder()
                .leaderId(10L)
                .name("테스트 스터디 1")
                .topic("Java")
                .studyType(StudyType.SHORT_TERM)
                .build());

        study2 = studyRepository.save(Study.builder()
                .leaderId(20L)
                .name("테스트 스터디 2")
                .topic("Spring")
                .studyType(StudyType.SHORT_TERM)
                .build());

        // 스터디 1의 최상위 댓글들
        parentComment1 = StudyComment.builder()
                .studyId(study1.getId())
                .userId(10L)
                .content("첫 번째 댓글입니다.")
                .build();
        parentComment1 = commentRepository.save(parentComment1);

        parentComment2 = StudyComment.builder()
                .studyId(study1.getId())
                .userId(11L)
                .content("두 번째 댓글입니다.")
                .imageUrl("https://example.com/image.png")
                .build();
        parentComment2 = commentRepository.save(parentComment2);

        // 첫 번째 댓글의 대댓글들
        replyComment1 = StudyComment.builder()
                .studyId(study1.getId())
                .userId(12L)
                .parentId(parentComment1.getId())
                .content("첫 번째 댓글에 대한 답글입니다.")
                .build();
        replyComment1 = commentRepository.save(replyComment1);

        replyComment2 = StudyComment.builder()
                .studyId(study1.getId())
                .userId(10L)
                .parentId(parentComment1.getId())
                .content("작성자의 답글입니다.")
                .build();
        replyComment2 = commentRepository.save(replyComment2);

        // 삭제된 댓글 (스터디 1)
        deletedComment = StudyComment.builder()
                .studyId(study1.getId())
                .userId(13L)
                .content("삭제된 댓글입니다.")
                .build();
        deletedComment.delete();
        deletedComment = commentRepository.save(deletedComment);
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
        Long userId = 10L;
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
        Long userId = 10L;

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
        Long wrongStudyId = 999L;

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
        Long userId = 10L;

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
                .userId(20L)
                .content("새로운 댓글입니다.")
                .build();

        // when
        StudyComment saved = commentRepository.save(newComment);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyId()).isEqualTo(study2.getId());
        assertThat(saved.getUserId()).isEqualTo(20L);
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
                .userId(20L)
                .parentId(parentComment1.getId())
                .content("새로운 대댓글입니다.")
                .build();

        // when
        StudyComment saved = commentRepository.save(reply);

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
                .userId(20L)
                .content("이미지 첨부 댓글입니다.")
                .imageUrl("https://example.com/new-image.png")
                .build();

        // when
        StudyComment saved = commentRepository.save(commentWithImage);

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

        // then
        Optional<StudyComment> result = commentRepository.findById(commentId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스터디별 댓글 전체 삭제")
    void deleteByStudyId_Success() {
        // given
        Long studyId = study1.getId();

        // when
        commentRepository.deleteByStudyId(studyId);
        commentRepository.flush();

        // then
        List<StudyComment> result = commentRepository.findByStudyIdAndIsDeletedFalseOrderByCreatedAtAsc(studyId);
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
        Long authorId = 10L;

        // when & then
        assertThat(comment.isAuthor(authorId)).isTrue();
    }

    @Test
    @DisplayName("작성자 본인 확인 - 타인")
    void isAuthor_False() {
        // given
        StudyComment comment = parentComment1;
        Long otherId = 999L;

        // when & then
        assertThat(comment.isAuthor(otherId)).isFalse();
    }
}