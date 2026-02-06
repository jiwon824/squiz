package com.ssafy.domain.board.repository;

import com.ssafy.domain.board.entity.BoardPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    Page<BoardPost> findAllByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    java.util.Optional<BoardPost> findByIdAndIsDeletedFalse(Long id);
}
