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
        Study study = studyRepository.findById(request.studyId())
                .orElseThrow(() -> new NotFoundException("STUDY_NOT_FOUND", "스터디를 찾을 수 없습니다."));
        if (!study.getLeaderId().equals(userId)) {
            throw new BusinessException("NOT_STUDY_LEADER", "스터디장만 모집글을 작성할 수 있습니다.");
        }
        if (!(study.getStatus() == Status.RECRUITING
                || study.getStatus() == Status.SCHEDULED
                || study.getStatus() == Status.PENDING)) {
            throw new BusinessException("STUDY_NOT_RECRUITING", "모집 예정/모집중/확정대기 상태의 스터디만 모집글을 작성할 수 있습니다.");
        }
        BoardPost post = new BoardPost(author, study, BoardCategory.FREE, request.title(), request.content());
        BoardPost saved = boardPostRepository.save(post);
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(study.getId(), MemberStatus.APPROVED);
        return BoardPostDetailResponse.from(saved, currentMembers, study.getStatus(), List.of());
    }

    @Transactional(readOnly = true)
    public Page<BoardPostSummaryResponse> getPosts(Pageable pageable) {
        return boardPostRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(post -> {
                    int currentMembers = studyMemberRepository.countByStudyIdAndStatus(
                            post.getStudy().getId(),
                            MemberStatus.APPROVED
                    );
                    return BoardPostSummaryResponse.from(post, currentMembers, post.getStudy().getStatus());
                });
    }

    @Transactional
    public BoardPostDetailResponse getPostDetail(Long postId) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집글을 찾을 수 없습니다."));
        post.increaseViewCount();
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(
                post.getStudy().getId(),
                MemberStatus.APPROVED
        );
        List<BoardCommentResponse> comments = boardCommentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(BoardCommentResponse::from)
                .toList();
        return BoardPostDetailResponse.from(post, currentMembers, post.getStudy().getStatus(), comments);
    }

    @Transactional
    public BoardPostDetailResponse updatePost(Long userId, Long postId, BoardPostUpdateRequest request) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "모집글을 찾을 수 없습니다."));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException("NO_PERMISSION", "작성자만 수정할 수 있습니다.");
        }
        post.update(request.title(), request.content());
        int currentMembers = studyMemberRepository.countByStudyIdAndStatus(
                post.getStudy().getId(),
                MemberStatus.APPROVED
        );
        List<BoardCommentResponse> comments = boardCommentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(BoardCommentResponse::from)
                .toList();
        return BoardPostDetailResponse.from(post, currentMembers, post.getStudy().getStatus(), comments);
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
                    "팀원 모집 댓글",
                    String.format("팀원 모집 %s 글에 댓글이 작성되었습니다.", post.getTitle()),
                    "RECRUITMENT_POST",
                    post.getId()
            );
        }
        return BoardCommentResponse.from(saved);
    }

    @Transactional
    public void reportPost(Long userId, Long postId, BoardReportRequest request) {
        BoardPost post = boardPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NotFoundException("BOARD_POST_NOT_FOUND", "紐⑥쭛湲??李얠쓣 ???놁뒿?덈떎."));
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
