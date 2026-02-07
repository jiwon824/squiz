package com.ssafy.domain.daily.service;

import com.ssafy.domain.daily.dto.response.DailyItemResponse;
import com.ssafy.domain.daily.dto.response.MyDailyItemsResponse;
import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.entity.DailyItem;
import com.ssafy.domain.daily.repository.DailyItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyItemService {

    private final DailyItemRepository dailyItemRepository;

    /**
     * 내 데일리 항목 조회 (카테고리별 그룹화)
     */
    public MyDailyItemsResponse getMyItems(Long dailyReportId, Long userId) {
        List<DailyItem> items = dailyItemRepository
                .findByDailyReportIdAndUserIdOrderByCreatedAtAsc(dailyReportId, userId);

        return MyDailyItemsResponse.from(dailyReportId, userId, items);
    }

    /**
     * 내 데일리 항목 조회 (특정 카테고리만)
     */
    public List<DailyItemResponse> getMyItemsByCategory(Long dailyReportId, Long userId, DailyCategory category) {
        return dailyItemRepository
                .findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc(dailyReportId, userId, category)
                .stream()
                .map(DailyItemResponse::from)
                .toList();
    }

    /**
     * 데일리 항목 단건 조회
     */
    public DailyItemResponse getItem(Long itemId) {
        DailyItem item = dailyItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 데일리 항목입니다: " + itemId));

        return DailyItemResponse.from(item);
    }

    /**
     * 데일리 항목 삭제 (본인 항목만)
     */
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        DailyItem item = dailyItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 데일리 항목입니다: " + itemId));

        if (!item.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 데일리 항목만 삭제할 수 있습니다");
        }

        dailyItemRepository.delete(item);
    }

    /**
     * 데일리 리포트의 전체 항목 조회
     */
    public List<DailyItemResponse> getAllItems(Long dailyReportId) {
        return dailyItemRepository.findByDailyReportIdOrderByCreatedAtAsc(dailyReportId)
                .stream()
                .map(DailyItemResponse::from)
                .toList();
    }

    /**
     * 데일리 리포트의 블로커 항목만 조회
     */
    public List<DailyItemResponse> getBlockers(Long dailyReportId) {
        return dailyItemRepository
                .findByDailyReportIdAndCategoryOrderByCreatedAtAsc(dailyReportId, DailyCategory.BLOCKER)
                .stream()
                .map(DailyItemResponse::from)
                .toList();
    }

    /**
     * 내 데일리 항목 개수 조회
     */
    public long countMyItems(Long dailyReportId, Long userId) {
        return dailyItemRepository.countByDailyReportIdAndUserId(dailyReportId, userId);
    }
}
