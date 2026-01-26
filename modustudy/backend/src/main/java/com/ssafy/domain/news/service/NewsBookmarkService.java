package com.ssafy.domain.news.service;

import com.ssafy.domain.news.dto.response.NewsResponse;
import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.entity.NewsBookmark;
import com.ssafy.domain.news.repository.ItNewsRepository;
import com.ssafy.domain.news.repository.NewsBookmarkRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsBookmarkService {

    private final NewsBookmarkRepository newsBookmarkRepository;
    private final ItNewsRepository itNewsRepository;
    private final UserRepository userRepository;

    /**
     * 북마크 추가
     */
    @Transactional
    public void addBookmark(Long userId, Long newsId) {
        // 이미 북마크 했는지 확인
        if (newsBookmarkRepository.existsByUserIdAndNewsId(userId, newsId)) {
            throw new IllegalArgumentException("이미 북마크한 뉴스입니다.");
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 뉴스 존재 여부 확인
        ItNews news = itNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다."));

        // 북마크 생성
        NewsBookmark bookmark = NewsBookmark.builder()
                .user(user)
                .news(news)
                .build();

        newsBookmarkRepository.save(bookmark);
    }

    /**
     * 북마크 삭제
     */
    @Transactional
    public void removeBookmark(Long userId, Long newsId) {
        NewsBookmark bookmark = newsBookmarkRepository.findByUserIdAndNewsId(userId, newsId)
                .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다."));

        newsBookmarkRepository.delete(bookmark);
    }

    /**
     * 내 북마크 목록 조회
     */
    public List<NewsResponse> getMyBookmarks(Long userId) {
        List<NewsBookmark> bookmarks = newsBookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return bookmarks.stream()
                .map(bookmark -> NewsResponse.from(bookmark.getNews()))
                .collect(Collectors.toList());
    }

    /**
     * 북마크 여부 확인
     */
    public boolean isBookmarked(Long userId, Long newsId) {
        return newsBookmarkRepository.existsByUserIdAndNewsId(userId, newsId);
    }
}