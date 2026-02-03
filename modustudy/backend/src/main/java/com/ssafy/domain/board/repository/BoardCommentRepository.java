package com.ssafy.domain.board.repository;

import com.ssafy.domain.board.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    List<BoardComment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
}
