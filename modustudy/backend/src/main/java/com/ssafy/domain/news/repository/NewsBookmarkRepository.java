package com.ssafy.domain.news.repository;

import com.ssafy.domain.news.entity.NewsBookmark;
import com.ssafy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsBookmarkRepository extends JpaRepository<NewsBookmark, Long> {

    // 사용자의 북마크 목록
    List<NewsBookmark> findByUserOrderByCreatedAtDesc(User user);

    // 특정 뉴스에 대한 북마크 찾기
    Optional<NewsBookmark> findByUserAndNewsId(User user, Long newsId);

    // 북마크 여부 확인
    boolean existsByUserAndNewsId(User user, Long newsId);

    // 사용자의 북마크 삭제
    void deleteByUserAndNewsId(User user, Long newsId);
}