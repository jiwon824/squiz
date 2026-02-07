package com.ssafy.domain.daily.service;

import com.ssafy.domain.daily.dto.response.DailyItemResponse;
import com.ssafy.domain.daily.dto.response.MyDailyItemsResponse;
import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.entity.DailyItem;
import com.ssafy.domain.daily.repository.DailyItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyItemServiceTest {

    @Mock
    private DailyItemRepository dailyItemRepository;

    @InjectMocks
    private DailyItemService dailyItemService;

    private DailyItem yesterdayItem;
    private DailyItem todayItem;
    private DailyItem blockerItem;
    private Long dailyReportId;
    private Long userId;

    @BeforeEach
    void setUp() {
        dailyReportId = 1L;
        userId = 10L;

        yesterdayItem = DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(userId)
                .category(DailyCategory.YESTERDAY)
                .content("어제 API 설계 완료했습니다")
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(yesterdayItem, "id", 1L);

        todayItem = DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(userId)
                .category(DailyCategory.TODAY)
                .content("오늘 구현 시작할 예정입니다")
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(todayItem, "id", 2L);

        blockerItem = DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(userId)
                .category(DailyCategory.BLOCKER)
                .content("DB 연결 이슈가 있습니다")
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(blockerItem, "id", 3L);
    }

    @Nested
    @DisplayName("getMyItems - 내 데일리 항목 조회")
    class GetMyItemsTest {

        @Test
        @DisplayName("성공 - 카테고리별 그룹화된 항목 반환")
        void success() {
            // given
            List<DailyItem> items = List.of(yesterdayItem, todayItem, blockerItem);
            given(dailyItemRepository.findByDailyReportIdAndUserIdOrderByCreatedAtAsc(dailyReportId, userId))
                    .willReturn(items);

            // when
            MyDailyItemsResponse result = dailyItemService.getMyItems(dailyReportId, userId);

            // then
            assertThat(result.getDailyReportId()).isEqualTo(dailyReportId);
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getYesterday()).hasSize(1);
            assertThat(result.getToday()).hasSize(1);
            assertThat(result.getBlocker()).hasSize(1);
            assertThat(result.getTotalCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - 항목이 없는 경우 빈 목록 반환")
        void success_Empty() {
            // given
            given(dailyItemRepository.findByDailyReportIdAndUserIdOrderByCreatedAtAsc(dailyReportId, userId))
                    .willReturn(Collections.emptyList());

            // when
            MyDailyItemsResponse result = dailyItemService.getMyItems(dailyReportId, userId);

            // then
            assertThat(result.getYesterday()).isEmpty();
            assertThat(result.getToday()).isEmpty();
            assertThat(result.getBlocker()).isEmpty();
            assertThat(result.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getMyItemsByCategory - 특정 카테고리 항목 조회")
    class GetMyItemsByCategoryTest {

        @Test
        @DisplayName("성공 - YESTERDAY 카테고리 항목 반환")
        void success_Yesterday() {
            // given
            given(dailyItemRepository.findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc(
                    dailyReportId, userId, DailyCategory.YESTERDAY))
                    .willReturn(List.of(yesterdayItem));

            // when
            List<DailyItemResponse> result = dailyItemService
                    .getMyItemsByCategory(dailyReportId, userId, DailyCategory.YESTERDAY);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(DailyCategory.YESTERDAY);
            assertThat(result.get(0).getContent()).contains("API 설계");
        }

        @Test
        @DisplayName("성공 - BLOCKER 카테고리 항목 반환")
        void success_Blocker() {
            // given
            given(dailyItemRepository.findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc(
                    dailyReportId, userId, DailyCategory.BLOCKER))
                    .willReturn(List.of(blockerItem));

            // when
            List<DailyItemResponse> result = dailyItemService
                    .getMyItemsByCategory(dailyReportId, userId, DailyCategory.BLOCKER);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(DailyCategory.BLOCKER);
        }
    }

    @Nested
    @DisplayName("getItem - 데일리 항목 단건 조회")
    class GetItemTest {

        @Test
        @DisplayName("성공 - 항목 반환")
        void success() {
            // given
            Long itemId = 1L;
            given(dailyItemRepository.findById(itemId))
                    .willReturn(Optional.of(yesterdayItem));

            // when
            DailyItemResponse result = dailyItemService.getItem(itemId);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getContent()).contains("API 설계");
            assertThat(result.getCategory()).isEqualTo(DailyCategory.YESTERDAY);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 항목")
        void fail_NotFound() {
            // given
            Long itemId = 999L;
            given(dailyItemRepository.findById(itemId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> dailyItemService.getItem(itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 데일리 항목");
        }
    }

    @Nested
    @DisplayName("deleteItem - 데일리 항목 삭제")
    class DeleteItemTest {

        @Test
        @DisplayName("성공 - 본인 항목 삭제")
        void success() {
            // given
            Long itemId = 1L;
            given(dailyItemRepository.findById(itemId))
                    .willReturn(Optional.of(yesterdayItem));

            // when
            dailyItemService.deleteItem(itemId, userId);

            // then
            verify(dailyItemRepository).delete(yesterdayItem);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 항목")
        void fail_NotFound() {
            // given
            Long itemId = 999L;
            given(dailyItemRepository.findById(itemId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> dailyItemService.deleteItem(itemId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 데일리 항목");
        }

        @Test
        @DisplayName("실패 - 타인의 항목 삭제 시도")
        void fail_NotOwner() {
            // given
            Long itemId = 1L;
            Long otherUserId = 999L;
            given(dailyItemRepository.findById(itemId))
                    .willReturn(Optional.of(yesterdayItem));

            // when & then
            assertThatThrownBy(() -> dailyItemService.deleteItem(itemId, otherUserId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인의 데일리 항목만 삭제");
        }
    }

    @Nested
    @DisplayName("getAllItems - 전체 항목 조회")
    class GetAllItemsTest {

        @Test
        @DisplayName("성공 - 전체 항목 반환")
        void success() {
            // given
            List<DailyItem> items = List.of(yesterdayItem, todayItem, blockerItem);
            given(dailyItemRepository.findByDailyReportIdOrderByCreatedAtAsc(dailyReportId))
                    .willReturn(items);

            // when
            List<DailyItemResponse> result = dailyItemService.getAllItems(dailyReportId);

            // then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("getBlockers - 블로커 항목 조회")
    class GetBlockersTest {

        @Test
        @DisplayName("성공 - 블로커 항목만 반환")
        void success() {
            // given
            given(dailyItemRepository.findByDailyReportIdAndCategoryOrderByCreatedAtAsc(
                    dailyReportId, DailyCategory.BLOCKER))
                    .willReturn(List.of(blockerItem));

            // when
            List<DailyItemResponse> result = dailyItemService.getBlockers(dailyReportId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(DailyCategory.BLOCKER);
            assertThat(result.get(0).getContent()).contains("DB 연결");
        }
    }

    @Nested
    @DisplayName("countMyItems - 내 항목 개수 조회")
    class CountMyItemsTest {

        @Test
        @DisplayName("성공 - 항목 개수 반환")
        void success() {
            // given
            given(dailyItemRepository.countByDailyReportIdAndUserId(dailyReportId, userId))
                    .willReturn(3L);

            // when
            long count = dailyItemService.countMyItems(dailyReportId, userId);

            // then
            assertThat(count).isEqualTo(3);
        }
    }
}
