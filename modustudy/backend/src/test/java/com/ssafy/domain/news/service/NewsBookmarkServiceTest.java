package com.ssafy.domain.news.service;

import com.ssafy.domain.news.dto.response.NewsResponse;
import com.ssafy.domain.news.entity.ItNews;
import com.ssafy.domain.news.repository.ItNewsRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NewsBookmarkServiceTest {

    @Autowired
    private NewsBookmarkService newsBookmarkService;

    @Autowired
    private ItNewsRepository itNewsRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private ItNews testNews;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .userId("test@test.com")
                .password("password123")
                .nickname("테스터" + System.currentTimeMillis())  // 중복 방지
                .email("test" + System.currentTimeMillis() + "@test.com")  // 중복 방지
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        userRepository.save(testUser);

        // 테스트용 뉴스 생성
        testNews = ItNews.builder()
                .title("테스트 뉴스")
                .summary("테스트 요약")
                .sourceUrl("https://test.com/news/" + System.currentTimeMillis())  // 중복 방지
                .sourceName("테스트 언론사")
                .category("IT")
                .publishedAt(LocalDateTime.now())
                .build();
        itNewsRepository.save(testNews);
    }

    @Test
    @DisplayName("북마크 추가 성공")
    void addBookmark() {
        // when
        newsBookmarkService.addBookmark(testUser.getId(), testNews.getId());

        // then
        boolean isBookmarked = newsBookmarkService.isBookmarked(testUser.getId(), testNews.getId());
        assertThat(isBookmarked).isTrue();
    }

    @Test
    @DisplayName("중복 북마크 추가 시 예외 발생")
    void addDuplicateBookmark() {
        // given
        newsBookmarkService.addBookmark(testUser.getId(), testNews.getId());

        // when & then
        assertThatThrownBy(() -> newsBookmarkService.addBookmark(testUser.getId(), testNews.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 북마크한 뉴스입니다.");
    }

    @Test
    @DisplayName("북마크 삭제 성공")
    void removeBookmark() {
        // given
        newsBookmarkService.addBookmark(testUser.getId(), testNews.getId());

        // when
        newsBookmarkService.removeBookmark(testUser.getId(), testNews.getId());

        // then
        boolean isBookmarked = newsBookmarkService.isBookmarked(testUser.getId(), testNews.getId());
        assertThat(isBookmarked).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 북마크 삭제 시 예외 발생")
    void removeNonexistentBookmark() {
        // when & then
        assertThatThrownBy(() -> newsBookmarkService.removeBookmark(testUser.getId(), testNews.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("북마크를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("내 북마크 목록 조회")
    void getMyBookmarks() {
        // given
        newsBookmarkService.addBookmark(testUser.getId(), testNews.getId());

        // when
        List<NewsResponse> bookmarks = newsBookmarkService.getMyBookmarks(testUser.getId());

        // then
        assertThat(bookmarks).hasSize(1);
        assertThat(bookmarks.get(0).getTitle()).isEqualTo("테스트 뉴스");
    }

    @Test
    @DisplayName("북마크가 없을 때 빈 리스트 반환")
    void getEmptyBookmarks() {
        // when
        List<NewsResponse> bookmarks = newsBookmarkService.getMyBookmarks(testUser.getId());

        // then
        assertThat(bookmarks).isEmpty();
    }
}
