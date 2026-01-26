package com.ssafy.domain.news.repository;

import com.ssafy.domain.news.entity.NewsBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NewsBookmarkRepository extends JpaRepository<NewsBookmark, Long> {

    // 북마크 존재 여부 확인
    @Query("SELECT CASE WHEN COUNT(nb) > 0 THEN true ELSE false END " +
            "FROM NewsBookmark nb " +
            "WHERE nb.user.id = :userId AND nb.news.id = :newsId")
    boolean existsByUserIdAndNewsId(@Param("userId") Long userId, @Param("newsId") Long newsId);

    // 특정 사용자의 특정 뉴스 북마크 조회
    @Query("SELECT nb FROM NewsBookmark nb " +
            "WHERE nb.user.id = :userId AND nb.news.id = :newsId")
    Optional<NewsBookmark> findByUserIdAndNewsId(@Param("userId") Long userId, @Param("newsId") Long newsId);

    // 특정 사용자의 모든 북마크 조회 (최신순)
    @Query("SELECT nb FROM NewsBookmark nb " +
            "JOIN FETCH nb.news n " +
            "WHERE nb.user.id = :userId " +
            "ORDER BY nb.createdAt DESC")
    List<NewsBookmark> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}