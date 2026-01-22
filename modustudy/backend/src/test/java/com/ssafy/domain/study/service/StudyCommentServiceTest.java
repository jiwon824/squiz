package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.StudyCommentCreateRequest;
import com.ssafy.domain.study.dto.request.StudyCommentUpdateRequest;
import com.ssafy.domain.study.dto.response.StudyCommentPageResponse;
import com.ssafy.domain.study.dto.response.StudyCommentResponse;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyComment;
import com.ssafy.domain.study.repository.StudyCommentRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;

/**
 * StudyCommentService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class StudyCommentServiceTest {

    @Mock
    private StudyCommentRepository commentRepository;

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyCommentService commentService;

    @Mock
    private User mockUser;

    @Mock
    private User mockUser2;

    @Mock
    private Study mockStudy;

    @Mock
    private StudyComment mockParentComment;

    @Mock
    private StudyComment mockReplyComment;

    @BeforeEach
    void setUp() {
        // mockUser 설정
        lenient().when(mockUser.getId()).thenReturn(10L);
        lenient().when(mockUser.getNickname()).thenReturn("ssafy_kim");
        lenient().when(mockUser.getProfileImage()).thenReturn(null);

        // mockUser2 설정
        lenient().when(mockUser2.getId()).thenReturn(11L);
        lenient().when(mockUser2.getNickname()).thenReturn("ssafy_lee");
        lenient().when(mockUser2.getProfileImage()).thenReturn(null);

        // mockStudy 설정
        lenient().when(mockStudy.getId()).thenReturn(1L);
        lenient().when(mockStudy.getLeaderId()).thenReturn(10L);
        lenient().when(mockStudy.getName()).thenReturn("알고리즘 스터디");

        // mockParentComment 설정 (최상위 댓글)
        lenient().when(mockParentComment.getId()).thenReturn(1L);
        lenient().when(mockParentComment.getStudyId()).thenReturn(1L);
        lenient().when(mockParentComment.getUserId()).thenReturn(10L);
        lenient().when(mockParentComment.getParentId()).thenReturn(null);
        lenient().when(mockParentComment.getContent()).thenReturn("첫 번째 댓글입니다.");
        lenient().when(mockParentComment.getImageUrl()).thenReturn(null);
        lenient().when(mockParentComment.getIsDeleted()).thenReturn(false);
        lenient().when(mockParentComment.isReply()).thenReturn(false);
        lenient().when(mockParentComment.isAuthor(10L)).thenReturn(true);
        lenient().when(mockParentComment.isAuthor(999L)).thenReturn(false);

        // mockReplyComment 설정 (대댓글)
        lenient().when(mockReplyComment.getId()).thenReturn(2L);
        lenient().when(mockReplyComment.getStudyId()).thenReturn(1L);
        lenient().when(mockReplyComment.getUserId()).thenReturn(11L);
        lenient().when(mockReplyComment.getParentId()).thenReturn(1L);
        lenient().when(mockReplyComment.getContent()).thenReturn("대댓글입니다.");
        lenient().when(mockReplyComment.getIsDeleted()).thenReturn(false);
        lenient().when(mockReplyComment.isReply()).thenReturn(true);
        lenient().when(mockReplyComment.isAuthor(11L)).thenReturn(true);
    }

    // ============================================================
    // 댓글 생성 테스트
    // ============================================================

    @Nested
    @DisplayName("댓글 생성 테스트")
    class CreateCommentTest {

        @Test
        @DisplayName("댓글 생성 성공")
        void createComment_Success() {
            // given
            Long studyId = 1L;
            Long userId = 10L;
            StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                    .content("새로운 댓글입니다.")
                    .build();

            StudyComment savedComment = mock(StudyComment.class);
            when(savedComment.getId()).thenReturn(3L);
            when(savedComment.getStudyId()).thenReturn(studyId);
            when(savedComment.getUserId()).thenReturn(userId);
            when(savedComment.getContent()).thenReturn("새로운 댓글입니다.");
            when(savedComment.getIsDeleted()).thenReturn(false);
            when(savedComment.isReply()).thenReturn(false);

            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(commentRepository.save(any(StudyComment.class))).willReturn(savedComment);

            // when
            StudyCommentResponse response = commentService.createComment(studyId, request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(3L);
            assertThat(response.getUserNickname()).isEqualTo("ssafy_kim");

            verify(studyRepository, times(1)).findById(studyId);
            verify(userRepository, times(1)).findById(userId);
            verify(commentRepository, times(1)).save(any(StudyComment.class));
        }

        @Test
        @DisplayName("대댓글 생성 성공")
        void createReply_Success() {
            // given
            Long studyId = 1L;
            Long userId = 11L;
            Long parentId = 1L;
            StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                    .parentId(parentId)
                    .content("대댓글입니다.")
                    .build();

            StudyComment savedReply = mock(StudyComment.class);
            when(savedReply.getId()).thenReturn(4L);
            when(savedReply.getStudyId()).thenReturn(studyId);
            when(savedReply.getUserId()).thenReturn(userId);
            when(savedReply.getParentId()).thenReturn(parentId);
            when(savedReply.getContent()).thenReturn("대댓글입니다.");
            when(savedReply.getIsDeleted()).thenReturn(false);
            when(savedReply.isReply()).thenReturn(true);

            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser2));
            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(parentId, studyId))
                    .willReturn(Optional.of(mockParentComment));
            given(commentRepository.save(any(StudyComment.class))).willReturn(savedReply);

            // when
            StudyCommentResponse response = commentService.createComment(studyId, request, userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(4L);
            assertThat(response.getParentId()).isEqualTo(parentId);

            verify(commentRepository, times(1)).findByIdAndStudyIdAndIsDeletedFalse(parentId, studyId);
            verify(commentRepository, times(1)).save(any(StudyComment.class));
        }

        @Test
        @DisplayName("댓글 생성 실패 - 존재하지 않는 스터디")
        void createComment_StudyNotFound() {
            // given
            Long studyId = 999L;
            Long userId = 10L;
            StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                    .content("댓글입니다.")
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(studyId, request, userId))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("댓글 생성 실패 - 존재하지 않는 사용자")
        void createComment_UserNotFound() {
            // given
            Long studyId = 1L;
            Long userId = 999L;
            StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                    .content("댓글입니다.")
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(studyId, request, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 사용자");

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("대댓글 생성 실패 - 존재하지 않는 부모 댓글")
        void createReply_ParentNotFound() {
            // given
            Long studyId = 1L;
            Long userId = 11L;
            Long parentId = 999L;
            StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                    .parentId(parentId)
                    .content("대댓글입니다.")
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser2));
            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(parentId, studyId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.createComment(studyId, request, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 부모 댓글");

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("대댓글 생성 실패 - 대댓글에 대댓글 시도")
        void createReply_NestedReplyNotAllowed() {
            // given
            Long studyId = 1L;
            Long userId = 10L;
            Long parentId = 2L;

            StudyCommentCreateRequest request = StudyCommentCreateRequest.builder()
                    .parentId(parentId)
                    .content("대댓글의 대댓글입니다.")
                    .build();

            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(parentId, studyId))
                    .willReturn(Optional.of(mockReplyComment));

            // when & then
            assertThatThrownBy(() -> commentService.createComment(studyId, request, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("대댓글에는 답글을 달 수 없습니다");

            verify(commentRepository, never()).save(any());
        }
    }

    // ============================================================
    // 댓글 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("댓글 조회 테스트")
    class GetCommentsTest {

        @Test
        @DisplayName("스터디별 댓글 목록 조회 성공")
        void getCommentsByStudy_Success() {
            // given
            Long studyId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Page<StudyComment> commentPage = new PageImpl<>(List.of(mockParentComment));

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(commentRepository.findAllParentCommentsByStudyId(studyId, pageable))
                    .willReturn(commentPage);
            given(userRepository.findAllById(anyList())).willReturn(List.of(mockUser));
            given(commentRepository.findRepliesByParentId(1L)).willReturn(List.of());

            // when
            StudyCommentPageResponse response = commentService.getCommentsByStudy(studyId, pageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getComments()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);

            verify(studyRepository, times(1)).existsById(studyId);
            verify(commentRepository, times(1)).findAllParentCommentsByStudyId(studyId, pageable);
        }

        @Test
        @DisplayName("스터디별 댓글 목록 조회 - 대댓글 포함")
        void getCommentsByStudy_WithReplies() {
            // given
            Long studyId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Page<StudyComment> commentPage = new PageImpl<>(List.of(mockParentComment));

            given(studyRepository.existsById(studyId)).willReturn(true);
            given(commentRepository.findAllParentCommentsByStudyId(studyId, pageable))
                    .willReturn(commentPage);
            // 첫 번째 호출: 부모 댓글 작성자 조회
            // 두 번째 호출: 대댓글 작성자 조회
            given(userRepository.findAllById(List.of(10L))).willReturn(List.of(mockUser));
            given(userRepository.findAllById(List.of(11L))).willReturn(List.of(mockUser2));
            given(commentRepository.findRepliesByParentId(1L)).willReturn(List.of(mockReplyComment));

            // when
            StudyCommentPageResponse response = commentService.getCommentsByStudy(studyId, pageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getComments()).hasSize(1);
            assertThat(response.getComments().get(0).getReplies()).hasSize(1);
        }

        @Test
        @DisplayName("스터디별 댓글 목록 조회 실패 - 존재하지 않는 스터디")
        void getCommentsByStudy_StudyNotFound() {
            // given
            Long studyId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            given(studyRepository.existsById(studyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentService.getCommentsByStudy(studyId, pageable))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);

            verify(commentRepository, never()).findAllParentCommentsByStudyId(any(), any());
        }

        @Test
        @DisplayName("대댓글 목록 조회 성공")
        void getReplies_Success() {
            // given
            Long studyId = 1L;
            Long parentId = 1L;

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(parentId, studyId))
                    .willReturn(Optional.of(mockParentComment));
            given(commentRepository.findRepliesByParentId(parentId)).willReturn(List.of(mockReplyComment));
            given(userRepository.findAllById(anyList())).willReturn(List.of(mockUser2));

            // when
            List<StudyCommentResponse> responses = commentService.getReplies(studyId, parentId);

            // then
            assertThat(responses).hasSize(1);

            verify(commentRepository, times(1)).findRepliesByParentId(parentId);
        }

        @Test
        @DisplayName("댓글 상세 조회 성공")
        void getComment_Success() {
            // given
            Long studyId = 1L;
            Long commentId = 1L;

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.of(mockParentComment));
            given(userRepository.findById(10L)).willReturn(Optional.of(mockUser));

            // when
            StudyCommentResponse response = commentService.getComment(studyId, commentId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(commentId);
        }

        @Test
        @DisplayName("댓글 상세 조회 실패 - 존재하지 않는 댓글")
        void getComment_NotFound() {
            // given
            Long studyId = 1L;
            Long commentId = 999L;

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.getComment(studyId, commentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 댓글");
        }
    }

    // ============================================================
    // 댓글 수정 테스트
    // ============================================================

    @Nested
    @DisplayName("댓글 수정 테스트")
    class UpdateCommentTest {

        @Test
        @DisplayName("댓글 수정 성공")
        void updateComment_Success() {
            // given
            Long studyId = 1L;
            Long commentId = 1L;
            Long userId = 10L;
            StudyCommentUpdateRequest request = StudyCommentUpdateRequest.builder()
                    .content("수정된 댓글입니다.")
                    .build();

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.of(mockParentComment));
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            // when
            StudyCommentResponse response = commentService.updateComment(studyId, commentId, request, userId);

            // then
            assertThat(response).isNotNull();
            verify(mockParentComment, times(1)).updateContent("수정된 댓글입니다.");
        }

        @Test
        @DisplayName("댓글 수정 실패 - 권한 없음 (작성자 아님)")
        void updateComment_NotAuthor() {
            // given
            Long studyId = 1L;
            Long commentId = 1L;
            Long notAuthorId = 999L;
            StudyCommentUpdateRequest request = StudyCommentUpdateRequest.builder()
                    .content("수정된 댓글입니다.")
                    .build();

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.of(mockParentComment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(studyId, commentId, request, notAuthorId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("댓글을 수정할 권한이 없습니다");
        }

        @Test
        @DisplayName("댓글 수정 실패 - 존재하지 않는 댓글")
        void updateComment_NotFound() {
            // given
            Long studyId = 1L;
            Long commentId = 999L;
            Long userId = 10L;
            StudyCommentUpdateRequest request = StudyCommentUpdateRequest.builder()
                    .content("수정된 댓글입니다.")
                    .build();

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(studyId, commentId, request, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 댓글");
        }
    }

    // ============================================================
    // 댓글 삭제 테스트
    // ============================================================

    @Nested
    @DisplayName("댓글 삭제 테스트")
    class DeleteCommentTest {

        @Test
        @DisplayName("댓글 삭제 성공 - 작성자")
        void deleteComment_ByAuthor_Success() {
            // given
            Long studyId = 1L;
            Long commentId = 1L;
            Long userId = 10L;

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.of(mockParentComment));
            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));

            // when
            commentService.deleteComment(studyId, commentId, userId);

            // then
            verify(mockParentComment, times(1)).delete();
        }

        @Test
        @DisplayName("댓글 삭제 성공 - 스터디장")
        void deleteComment_ByLeader_Success() {
            // given
            Long studyId = 1L;
            Long commentId = 2L;
            Long leaderId = 10L;

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.of(mockReplyComment));
            given(studyRepository.findById(studyId)).willReturn(Optional.of(mockStudy));

            // when
            commentService.deleteComment(studyId, commentId, leaderId);

            // then
            verify(mockReplyComment, times(1)).delete();
        }

        @Test
        @DisplayName("댓글 삭제 실패 - 권한 없음")
        void deleteComment_NoPermission() {
            // given
            Long studyId = 1L;
            Long commentId = 1L;
            Long notAuthorNotLeaderId = 999L;

            Study studyWithDifferentLeader = mock(Study.class);
            when(studyWithDifferentLeader.getLeaderId()).thenReturn(100L);

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.of(mockParentComment));
            given(studyRepository.findById(studyId)).willReturn(Optional.of(studyWithDifferentLeader));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(studyId, commentId, notAuthorNotLeaderId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("댓글을 삭제할 권한이 없습니다");
        }

        @Test
        @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
        void deleteComment_NotFound() {
            // given
            Long studyId = 1L;
            Long commentId = 999L;
            Long userId = 10L;

            given(commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(studyId, commentId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 댓글");
        }
    }

    // ============================================================
    // 통계 조회 테스트
    // ============================================================

    @Nested
    @DisplayName("통계 조회 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("스터디별 댓글 개수 조회 성공")
        void getCommentCount_Success() {
            // given
            Long studyId = 1L;
            Long expectedCount = 5L;

            given(commentRepository.countByStudyIdAndIsDeletedFalse(studyId)).willReturn(expectedCount);

            // when
            Long count = commentService.getCommentCount(studyId);

            // then
            assertThat(count).isEqualTo(expectedCount);

            verify(commentRepository, times(1)).countByStudyIdAndIsDeletedFalse(studyId);
        }
    }
}