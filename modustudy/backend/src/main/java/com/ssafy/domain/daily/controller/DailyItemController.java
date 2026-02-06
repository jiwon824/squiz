package com.ssafy.domain.daily.controller;

import com.ssafy.domain.daily.dto.response.DailyItemResponse;
import com.ssafy.domain.daily.dto.response.MyDailyItemsResponse;
import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.service.DailyItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dailies/{dailyId}/items")
@RequiredArgsConstructor
public class DailyItemController {

    private final DailyItemService dailyItemService;

    /**
     * 내 데일리 항목 조회 (카테고리별 그룹화)
     * GET /api/v1/dailies/{dailyId}/items/my
     */
    @GetMapping("/my")
    public ResponseEntity<MyDailyItemsResponse> getMyItems(
            @PathVariable("dailyId") Long dailyId,
            @RequestHeader("user-id") Long userId) {

        MyDailyItemsResponse response = dailyItemService.getMyItems(dailyId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 데일리 항목 조회 (특정 카테고리만)
     * GET /api/v1/dailies/{dailyId}/items/my?category=YESTERDAY
     */
    @GetMapping("/my/category")
    public ResponseEntity<List<DailyItemResponse>> getMyItemsByCategory(
            @PathVariable("dailyId") Long dailyId,
            @RequestHeader("user-id") Long userId,
            @RequestParam("category") DailyCategory category) {

        List<DailyItemResponse> response = dailyItemService.getMyItemsByCategory(dailyId, userId, category);
        return ResponseEntity.ok(response);
    }

    /**
     * 데일리 항목 단건 조회
     * GET /api/v1/dailies/{dailyId}/items/{itemId}
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<DailyItemResponse> getItem(
            @PathVariable("dailyId") Long dailyId,
            @PathVariable("itemId") Long itemId) {

        DailyItemResponse response = dailyItemService.getItem(itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * 데일리 항목 삭제 (본인 항목만)
     * DELETE /api/v1/dailies/{dailyId}/items/{itemId}
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable("dailyId") Long dailyId,
            @PathVariable("itemId") Long itemId,
            @RequestHeader("user-id") Long userId) {

        dailyItemService.deleteItem(itemId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 데일리 리포트의 전체 항목 조회
     * GET /api/v1/dailies/{dailyId}/items
     */
    @GetMapping
    public ResponseEntity<List<DailyItemResponse>> getAllItems(
            @PathVariable("dailyId") Long dailyId) {

        List<DailyItemResponse> response = dailyItemService.getAllItems(dailyId);
        return ResponseEntity.ok(response);
    }

    /**
     * 데일리 리포트의 블로커 항목만 조회
     * GET /api/v1/dailies/{dailyId}/items/blockers
     */
    @GetMapping("/blockers")
    public ResponseEntity<List<DailyItemResponse>> getBlockers(
            @PathVariable("dailyId") Long dailyId) {

        List<DailyItemResponse> response = dailyItemService.getBlockers(dailyId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 데일리 항목 개수 조회
     * GET /api/v1/dailies/{dailyId}/items/my/count
     */
    @GetMapping("/my/count")
    public ResponseEntity<Long> countMyItems(
            @PathVariable("dailyId") Long dailyId,
            @RequestHeader("user-id") Long userId) {

        long count = dailyItemService.countMyItems(dailyId, userId);
        return ResponseEntity.ok(count);
    }
}
