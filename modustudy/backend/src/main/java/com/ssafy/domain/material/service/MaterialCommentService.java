package com.ssafy.domain.material.service;

import com.ssafy.common.exception.MaterialException;
import com.ssafy.domain.material.dto.request.MaterialCommentCreateRequest;
import com.ssafy.domain.material.dto.response.MaterialCommentCreateResponse;
import com.ssafy.domain.material.dto.response.MaterialCommentResponse;
import com.ssafy.domain.material.dto.response.UploaderInfo;
import com.ssafy.domain.material.entity.MaterialComment;
import com.ssafy.domain.material.repository.MaterialCommentRepository;
import com.ssafy.domain.material.repository.MaterialRepository;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 자료 댓글 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialCommentService {

    private final MaterialCommentRepository commentRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 목록 조회
     */
    public List<MaterialCommentResponse> getComments(Long materialId) {
        log.info("댓글 목록 조회 - materialId: {}", materialId);

        // 자료 존재 확인
        if (!materialRepository.existsById(materialId)) {
            throw new MaterialException.MaterialNotFoundException(materialId);
        }

        List<MaterialComment> comments = commentRepository.findByMaterialId(materialId);

        return comments.stream()
                .map(comment -> {
                    UploaderInfo userInfo = getUserInfo(comment.getUserId());
                    return MaterialCommentResponse.from(comment, userInfo);
                })
                .collect(Collectors.toList());
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public MaterialCommentCreateResponse createComment(Long materialId, Long userId, MaterialCommentCreateRequest request) {
        log.info("댓글 작성 - materialId: {}, userId: {}", materialId, userId);

        // 자료 존재 확인
        if (!materialRepository.existsById(materialId)) {
            throw new MaterialException.MaterialNotFoundException(materialId);
        }

        MaterialComment comment = MaterialComment.create(
                materialId,
                userId,
                request.getContent()
        );

        MaterialComment saved = commentRepository.save(comment);
        log.info("댓글 작성 완료 - commentId: {}", saved.getId());

        return MaterialCommentCreateResponse.from(saved);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long materialId, Long commentId, Long userId) {
        log.info("댓글 삭제 - materialId: {}, commentId: {}, userId: {}", materialId, commentId, userId);

        MaterialComment comment = commentRepository.findById(commentId)
                .orElseThrow(MaterialException.MaterialCommentNotFoundException::new);

        // 해당 자료의 댓글인지 확인
        if (!comment.getMaterialId().equals(materialId)) {
            throw new MaterialException.MaterialCommentNotFoundException();
        }

        // 본인 확인
        if (!comment.isAuthor(userId)) {
            throw new MaterialException.NotCommentAuthorException();
        }

        commentRepository.delete(comment);
        log.info("댓글 삭제 완료 - commentId: {}", commentId);
    }

    /**
     * 사용자 정보 조회
     */
    private UploaderInfo getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .map(user -> UploaderInfo.of(user.getId(), user.getNickname(), user.getProfileImage()))
                .orElse(UploaderInfo.of(userId, "알 수 없음", null));
    }
}