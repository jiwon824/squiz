package com.ssafy.domain.news.controller;

import com.ssafy.domain.news.dto.response.NewsResponse;
import com.ssafy.domain.news.service.NewsBookmarkService;
import com.ssafy.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "뉴스 북마크", description = "뉴스 북마크 관리 API")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsBookmarkController {

    private final NewsBookmarkService newsBookmarkService;

    @Operation(summary = "북마크 추가", description = "뉴스를 북마크에 추가합니다.")
    @PostMapping("/{newsId}/bookmark")
    public ResponseEntity<Map<String, String>> addBookmark(
            @PathVariable Long newsId,
            @AuthenticationPrincipal User user
    ) {
        newsBookmarkService.addBookmark(user.getId(), newsId);
        return ResponseEntity.ok(Map.of("message", "북마크가 추가되었습니다."));
    }

    @Operation(summary = "북마크 삭제", description = "북마크를 삭제합니다.")
    @DeleteMapping("/{newsId}/bookmark")
    public ResponseEntity<Map<String, String>> removeBookmark(
            @PathVariable Long newsId,
            @AuthenticationPrincipal User user
    ) {
        newsBookmarkService.removeBookmark(user.getId(), newsId);
        return ResponseEntity.ok(Map.of("message", "북마크가 삭제되었습니다."));
    }

    @Operation(summary = "내 북마크 목록", description = "내가 북마크한 뉴스 목록을 조회합니다.")
    @GetMapping("/bookmarks")
    public ResponseEntity<List<NewsResponse>> getMyBookmarks(
            @AuthenticationPrincipal User user
    ) {
        List<NewsResponse> bookmarks = newsBookmarkService.getMyBookmarks(user.getId());
        return ResponseEntity.ok(bookmarks);
    }

    @Operation(summary = "북마크 여부 확인", description = "특정 뉴스의 북마크 여부를 확인합니다.")
    @GetMapping("/{newsId}/bookmark/check")
    public ResponseEntity<Map<String, Boolean>> checkBookmark(
            @PathVariable Long newsId,
            @AuthenticationPrincipal User user
    ) {
        boolean isBookmarked = newsBookmarkService.isBookmarked(user.getId(), newsId);
        return ResponseEntity.ok(Map.of("isBookmarked", isBookmarked));
    }
}