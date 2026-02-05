package com.ssafy.domain.board.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.domain.board.dto.request.BoardCommentCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostUpdateRequest;
import com.ssafy.domain.board.dto.request.BoardReportRequest;
import com.ssafy.domain.board.dto.response.BoardCommentResponse;
import com.ssafy.domain.board.dto.response.BoardPostDetailResponse;
import com.ssafy.domain.board.dto.response.BoardPostSummaryResponse;
import com.ssafy.domain.board.dto.response.BoardRecruitingStudyResponse;
import com.ssafy.domain.board.entity.BoardCategory;
import com.ssafy.domain.board.entity.BoardComment;
import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.board.entity.RecruitmentStatus;
import com.ssafy.domain.board.repository.BoardCommentRepository;
import com.ssafy.domain.board.repository.BoardPostRepository;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardPostRepository boardPostRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<BoardRecruitingStudyResponse> getRecruitingStudies(Long leaderId) {
        List<Study> studies = studyRepository.findByLeaderIdAndStatusIn(
                leaderId,
                List.of(Status.SCHEDULED, Status.RECRUITING, Status.PENDING)
        );
        return studies.stream()
                .map(study -> BoardRecruitingStudyResponse.from(
                        study,
                        studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED)
                ))
                .toList();
    }

    @Transactional
    public BoardPostDetailResponse createPost(Long userId, BoardPostCreateRequest request) {
        User author = userRepository.findById(userId).orElseThrow(NotFoundException::user);
        BoardPost post = new BoardPost(
                author,
                null,
                BoardCategory.FREE,
                request.title(),
                request.content(),
                request.recruitmentField(),
                request.meetingType(),
                request.targetMembers(),
                RecruitmentStatus.RECRUITING
        );
        BoardPost saved = boardPostRepository.save(post);
        return BoardPostDetailResponse.from(saved, List.of());
    }
    @Transactional(readOnly = true)
    public Page<BoardPostSummaryResponse> getPosts(Pageable pageable) {
        return boardPostRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(BoardPostSummaryResponse::from);
    }

    @Transactional
    public BoardPostDetailResponse getPostDetail(Long postId) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집글을 찾을 수 없습니다."));
        post.increaseViewCount();
        List<BoardCommentResponse> comments = boardCommentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(BoardCommentResponse::from)
                .toList();
        return BoardPostDetailResponse.from(post, comments);
    }

    @Transactional
    public BoardPostDetailResponse updatePost(Long userId, Long postId, BoardPostUpdateRequest request) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집글을 찾을 수 없습니다."));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException("NO_PERMISSION", "작성자만 수정할 수 있습니다.");
        }
        post.update(
                request.title(),
                request.content(),
                request.recruitmentField(),
                request.meetingType(),
                request.targetMembers(),
                request.recruitmentStatus()
        );
        List<BoardCommentResponse> comments = boardCommentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(BoardCommentResponse::from)
                .toList();
        return BoardPostDetailResponse.from(post, comments);
    }
    @Transactional
    public void deletePost(Long userId, Long postId) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집글을 찾을 수 없습니다."));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException("NO_PERMISSION", "작성자만 삭제할 수 있습니다.");
        }
        post.delete();
    }

    @Transactional
    public BoardCommentResponse addComment(Long userId, Long postId, BoardCommentCreateRequest request) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집글을 찾을 수 없습니다."));
        User author = userRepository.findById(userId).orElseThrow(NotFoundException::user);
        BoardComment parent = null;
        if (request.parentId() != null) {
            parent = boardCommentRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("BOARD_COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));
            if (!parent.getPost().getId().equals(postId)) {
                throw new BusinessException("INVALID_PARENT_COMMENT", "잘못된 댓글입니다.");
            }
        }
        BoardComment comment = new BoardComment(post, author, parent, request.content());
        BoardComment saved = boardCommentRepository.save(comment);
        post.increaseCommentCount();
        if (!post.getAuthor().getId().equals(userId)) {
            notificationService.createNotification(
                    post.getAuthor().getId(),
                    NotificationType.STUDY_UPDATE,
                    "스터디 모집 댓글",
                    String.format("스터디 모집 %s 글에 댓글이 작성되었습니다.", post.getTitle()),
                    "RECRUITMENT_POST",
                    post.getId()
            );
        }
        return BoardCommentResponse.from(saved);
    }

    @Transactional
    public void reportPost(Long userId, Long postId, BoardReportRequest request) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집 게시글을 찾을 수 없습니다."));
        User reporter = userRepository.findById(userId).orElseThrow(NotFoundException::user);

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        if (admins.isEmpty()) {
            return;
        }

        String reporterName = reporter.getNickname() != null ? reporter.getNickname() : reporter.getName();
        String title = "모집 게시글 신고";
        String content = String.format("'%s' 게시글이 신고되었습니다. 신고자: %s, 사유: %s",
                post.getTitle(),
                reporterName == null ? "익명" : reporterName,
                request.reason());

        for (User admin : admins) {
            if (admin.getId().equals(userId)) {
                continue;
            }
            notificationService.createNotification(
                    admin.getId(),
                    NotificationType.REPORT,
                    title,
                    content,
                    "RECRUITMENT_POST",
                    post.getId()
            );
        }
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("BOARD_COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException("NO_PERMISSION", "작성자만 삭제할 수 있습니다.");
        }
        comment.delete();
        comment.getPost().decreaseCommentCount();
    }
}




