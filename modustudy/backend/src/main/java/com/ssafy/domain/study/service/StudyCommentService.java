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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudyCommentService {

    private final StudyCommentRepository commentRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    // ============================================================
    // 댓글 생성
    // ============================================================

    /**
     * 댓글 생성
     */
    @Transactional
    public StudyCommentResponse createComment(Long studyId, StudyCommentCreateRequest request, Long userId) {
        log.info("댓글 생성 시작 - studyId: {}, userId: {}, parentId: {}", studyId, userId, request.getParentId());

        // 1. 스터디 존재 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 - studyId: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자 - userId: {}", userId);
                    return new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
                });

        // 3. 대댓글인 경우 부모 댓글 존재 확인
        if (request.getParentId() != null) {
            validateParentComment(studyId, request.getParentId());
        }

        // 4. 댓글 생성
        StudyComment comment = StudyComment.builder()
                .studyId(studyId)
                .userId(userId)
                .parentId(request.getParentId())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .build();

        StudyComment saved = commentRepository.save(comment);

        log.info("댓글 생성 완료 - commentId: {}, isReply: {}", saved.getId(), saved.isReply());

        return StudyCommentResponse.from(saved, user);
    }

    // ============================================================
    // 댓글 조회
    // ============================================================

    /**
     * 스터디별 댓글 목록 조회 (최상위 댓글 + 대댓글 포함)
     */
    public StudyCommentPageResponse getCommentsByStudy(Long studyId, Pageable pageable) {
        log.info("스터디별 댓글 목록 조회 - studyId: {}, page: {}, size: {}",
                studyId, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 스터디 존재 확인
        if (!studyRepository.existsById(studyId)) {
            log.error("존재하지 않는 스터디 - studyId: {}", studyId);
            throw new StudyException.StudyNotFoundException(studyId);
        }

        // 2. 최상위 댓글 조회 (삭제된 것 포함 - 대댓글 있으면 표시해야 함)
        Page<StudyComment> parentComments = commentRepository.findAllParentCommentsByStudyId(studyId, pageable);

        // 3. 부모 댓글 ID 목록 추출
        List<Long> parentIds = parentComments.getContent().stream()
                .map(StudyComment::getId)
                .toList();

        // 4. 사용자 정보 일괄 조회 (N+1 방지)
        List<Long> allUserIds = parentComments.getContent().stream()
                .map(StudyComment::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 각 부모 댓글에 대댓글 조회 및 DTO 변환
        List<StudyCommentResponse> commentResponses = parentComments.getContent().stream()
                .map(parent -> {
                    User author = userMap.get(parent.getUserId());

                    // 대댓글 조회
                    List<StudyComment> replies = commentRepository.findRepliesByParentId(parent.getId());

                    // 대댓글 작성자 정보 조회
                    List<Long> replyUserIds = replies.stream()
                            .map(StudyComment::getUserId)
                            .distinct()
                            .toList();

                    Map<Long, User> replyUserMap = userRepository.findAllById(replyUserIds).stream()
                            .collect(Collectors.toMap(User::getId, user -> user));

                    // 대댓글 DTO 변환
                    List<StudyCommentResponse> replyResponses = replies.stream()
                            .map(reply -> StudyCommentResponse.from(reply, replyUserMap.get(reply.getUserId())))
                            .toList();

                    return StudyCommentResponse.from(parent, author, replyResponses);
                })
                .toList();

        log.info("댓글 목록 조회 완료 - studyId: {}, totalElements: {}", studyId, parentComments.getTotalElements());

        return StudyCommentPageResponse.of(
                commentResponses,
                parentComments.getTotalElements(),
                parentComments.getTotalPages(),
                parentComments.getNumber(),
                parentComments.hasNext(),
                parentComments.hasPrevious()
        );
    }

    /**
     * 스터디별 댓글 목록 조회 (최상위 댓글만, 대댓글 개수만)
     */
    public StudyCommentPageResponse getParentCommentsOnly(Long studyId, Pageable pageable) {
        log.info("스터디별 최상위 댓글 목록 조회 - studyId: {}", studyId);

        // 1. 스터디 존재 확인
        if (!studyRepository.existsById(studyId)) {
            throw new StudyException.StudyNotFoundException(studyId);
        }

        // 2. 최상위 댓글 조회
        Page<StudyComment> parentComments = commentRepository.findParentCommentsByStudyId(studyId, pageable);

        // 3. 사용자 정보 일괄 조회
        List<Long> userIds = parentComments.getContent().stream()
                .map(StudyComment::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. DTO 변환 (대댓글 개수만 포함)
        List<StudyCommentResponse> commentResponses = parentComments.getContent().stream()
                .map(comment -> {
                    User author = userMap.get(comment.getUserId());
                    Long replyCount = commentRepository.countRepliesByParentId(comment.getId());
                    return StudyCommentResponse.from(comment, author, replyCount);
                })
                .toList();

        return StudyCommentPageResponse.of(
                commentResponses,
                parentComments.getTotalElements(),
                parentComments.getTotalPages(),
                parentComments.getNumber(),
                parentComments.hasNext(),
                parentComments.hasPrevious()
        );
    }

    /**
     * 대댓글 목록 조회
     */
    public List<StudyCommentResponse> getReplies(Long studyId, Long parentId) {
        log.info("대댓글 목록 조회 - studyId: {}, parentId: {}", studyId, parentId);

        // 1. 부모 댓글 존재 확인
        validateParentComment(studyId, parentId);

        // 2. 대댓글 조회
        List<StudyComment> replies = commentRepository.findRepliesByParentId(parentId);

        // 3. 사용자 정보 조회
        List<Long> userIds = replies.stream()
                .map(StudyComment::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        log.info("대댓글 목록 조회 완료 - parentId: {}, count: {}", parentId, replies.size());

        // 4. DTO 변환
        return replies.stream()
                .map(reply -> StudyCommentResponse.from(reply, userMap.get(reply.getUserId())))
                .toList();
    }

    /**
     * 댓글 상세 조회
     */
    public StudyCommentResponse getComment(Long studyId, Long commentId) {
        log.info("댓글 상세 조회 - studyId: {}, commentId: {}", studyId, commentId);

        // 1. 댓글 조회
        StudyComment comment = commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 댓글 - commentId: {}, studyId: {}", commentId, studyId);
                    return new IllegalArgumentException("존재하지 않는 댓글입니다: " + commentId);
                });

        // 2. 작성자 정보 조회
        User author = userRepository.findById(comment.getUserId())
                .orElse(null);

        log.info("댓글 상세 조회 완료 - commentId: {}", commentId);

        return StudyCommentResponse.from(comment, author);
    }

    // ============================================================
    // 댓글 수정
    // ============================================================

    /**
     * 댓글 수정
     */
    @Transactional
    public StudyCommentResponse updateComment(Long studyId, Long commentId, StudyCommentUpdateRequest request, Long userId) {
        log.info("댓글 수정 시작 - studyId: {}, commentId: {}, userId: {}", studyId, commentId, userId);

        // 1. 댓글 조회
        StudyComment comment = commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 댓글 - commentId: {}, studyId: {}", commentId, studyId);
                    return new IllegalArgumentException("존재하지 않는 댓글입니다: " + commentId);
                });

        // 2. 작성자 확인
        if (!comment.isAuthor(userId)) {
            log.warn("댓글 수정 권한 없음 - commentId: {}, userId: {}", commentId, userId);
            throw new IllegalStateException("댓글을 수정할 권한이 없습니다");
        }

        // 3. 댓글 수정
        comment.updateContent(request.getContent());
        if (request.getImageUrl() != null) {
            comment.updateImageUrl(request.getImageUrl());
        }

        // 4. 작성자 정보 조회
        User author = userRepository.findById(userId)
                .orElse(null);

        log.info("댓글 수정 완료 - commentId: {}", commentId);

        return StudyCommentResponse.from(comment, author);
    }

    // ============================================================
    // 댓글 삭제
    // ============================================================

    /**
     * 댓글 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteComment(Long studyId, Long commentId, Long userId) {
        log.info("댓글 삭제 시작 - studyId: {}, commentId: {}, userId: {}", studyId, commentId, userId);

        // 1. 댓글 조회
        StudyComment comment = commentRepository.findByIdAndStudyIdAndIsDeletedFalse(commentId, studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 댓글 - commentId: {}, studyId: {}", commentId, studyId);
                    return new IllegalArgumentException("존재하지 않는 댓글입니다: " + commentId);
                });

        // 2. 권한 확인 (작성자 또는 스터디장)
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));

        boolean isAuthor = comment.isAuthor(userId);
        boolean isLeader = study.getLeaderId().equals(userId);

        if (!isAuthor && !isLeader) {
            log.warn("댓글 삭제 권한 없음 - commentId: {}, userId: {}", commentId, userId);
            throw new IllegalStateException("댓글을 삭제할 권한이 없습니다");
        }

        // 3. Soft Delete
        comment.delete();

        log.info("댓글 삭제 완료 - commentId: {}, deletedBy: {}", commentId, isAuthor ? "작성자" : "스터디장");
    }

    // ============================================================
    // 통계 조회
    // ============================================================

    /**
     * 스터디별 댓글 개수 조회
     */
    public Long getCommentCount(Long studyId) {
        log.info("스터디별 댓글 개수 조회 - studyId: {}", studyId);

        Long count = commentRepository.countByStudyIdAndIsDeletedFalse(studyId);

        log.info("댓글 개수 조회 완료 - studyId: {}, count: {}", studyId, count);

        return count;
    }

    // ============================================================
    // Private 검증 메서드
    // ============================================================

    /**
     * 부모 댓글 존재 및 유효성 확인
     */
    private void validateParentComment(Long studyId, Long parentId) {
        StudyComment parent = commentRepository.findByIdAndStudyIdAndIsDeletedFalse(parentId, studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 부모 댓글 - parentId: {}, studyId: {}", parentId, studyId);
                    return new IllegalArgumentException("존재하지 않는 부모 댓글입니다: " + parentId);
                });

        // 대댓글에 대댓글 달기 방지 (2단계까지만 허용)
        if (parent.isReply()) {
            log.warn("대댓글에 대댓글 시도 - parentId: {}", parentId);
            throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다");
        }
    }
}