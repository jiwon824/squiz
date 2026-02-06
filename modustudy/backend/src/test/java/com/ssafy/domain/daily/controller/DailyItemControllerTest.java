package com.ssafy.domain.daily.controller;

import com.ssafy.domain.daily.dto.response.DailyItemResponse;
import com.ssafy.domain.daily.dto.response.MyDailyItemsResponse;
import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.service.DailyItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class DailyItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyItemService dailyItemService;

    private Long dailyId;
    private Long userId;
    private DailyItemResponse yesterdayItem;
    private DailyItemResponse todayItem;
    private DailyItemResponse blockerItem;
    private MyDailyItemsResponse myItemsResponse;

    @BeforeEach
    void setUp() {
        dailyId = 1L;
        userId = 10L;

        yesterdayItem = DailyItemResponse.builder()
                .id(1L)
                .dailyReportId(dailyId)
                .userId(userId)
                .category(DailyCategory.YESTERDAY)
                .content("어제 API 설계 완료했습니다")
                .createdAt(LocalDateTime.now())
                .build();

        todayItem = DailyItemResponse.builder()
                .id(2L)
                .dailyReportId(dailyId)
                .userId(userId)
                .category(DailyCategory.TODAY)
                .content("오늘 구현 시작할 예정입니다")
                .createdAt(LocalDateTime.now())
                .build();

        blockerItem = DailyItemResponse.builder()
                .id(3L)
                .dailyReportId(dailyId)
                .userId(userId)
                .category(DailyCategory.BLOCKER)
                .content("DB 연결 이슈가 있습니다")
                .createdAt(LocalDateTime.now())
                .build();

        myItemsResponse = MyDailyItemsResponse.builder()
                .dailyReportId(dailyId)
                .userId(userId)
                .yesterday(List.of(yesterdayItem))
                .today(List.of(todayItem))
                .blocker(List.of(blockerItem))
                .totalCount(3)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/dailies/{dailyId}/items/my - 내 데일리 항목 조회")
    class GetMyItemsTest {

        @Test
        @DisplayName("성공 - 카테고리별 그룹화된 항목 반환")
        void success() throws Exception {
            // given
            given(dailyItemService.getMyItems(dailyId, userId))
                    .willReturn(myItemsResponse);

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/my", dailyId)
                            .header("user-id", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyReportId").value(dailyId))
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.yesterday", hasSize(1)))
                    .andExpect(jsonPath("$.today", hasSize(1)))
                    .andExpect(jsonPath("$.blocker", hasSize(1)))
                    .andExpect(jsonPath("$.totalCount").value(3));
        }

        @Test
        @DisplayName("성공 - 항목 없음")
        void success_Empty() throws Exception {
            // given
            MyDailyItemsResponse emptyResponse = MyDailyItemsResponse.builder()
                    .dailyReportId(dailyId)
                    .userId(userId)
                    .yesterday(List.of())
                    .today(List.of())
                    .blocker(List.of())
                    .totalCount(0)
                    .build();

            given(dailyItemService.getMyItems(dailyId, userId))
                    .willReturn(emptyResponse);

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/my", dailyId)
                            .header("user-id", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.yesterday", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dailies/{dailyId}/items/my/category - 특정 카테고리 항목 조회")
    class GetMyItemsByCategoryTest {

        @Test
        @DisplayName("성공 - YESTERDAY 카테고리 조회")
        void success_Yesterday() throws Exception {
            // given
            given(dailyItemService.getMyItemsByCategory(dailyId, userId, DailyCategory.YESTERDAY))
                    .willReturn(List.of(yesterdayItem));

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/my/category", dailyId)
                            .header("user-id", userId)
                            .param("category", "YESTERDAY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].category").value("YESTERDAY"))
                    .andExpect(jsonPath("$[0].content").value("어제 API 설계 완료했습니다"));
        }

        @Test
        @DisplayName("성공 - BLOCKER 카테고리 조회")
        void success_Blocker() throws Exception {
            // given
            given(dailyItemService.getMyItemsByCategory(dailyId, userId, DailyCategory.BLOCKER))
                    .willReturn(List.of(blockerItem));

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/my/category", dailyId)
                            .header("user-id", userId)
                            .param("category", "BLOCKER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].category").value("BLOCKER"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dailies/{dailyId}/items/{itemId} - 항목 단건 조회")
    class GetItemTest {

        @Test
        @DisplayName("성공 - 항목 반환")
        void success() throws Exception {
            // given
            Long itemId = 1L;
            given(dailyItemService.getItem(itemId))
                    .willReturn(yesterdayItem);

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/{itemId}", dailyId, itemId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.category").value("YESTERDAY"))
                    .andExpect(jsonPath("$.content").value("어제 API 설계 완료했습니다"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 항목")
        void fail_NotFound() throws Exception {
            // given
            Long itemId = 999L;
            given(dailyItemService.getItem(itemId))
                    .willThrow(new IllegalArgumentException("존재하지 않는 데일리 항목입니다: " + itemId));

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/{itemId}", dailyId, itemId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/dailies/{dailyId}/items/{itemId} - 항목 삭제")
    class DeleteItemTest {

        @Test
        @DisplayName("성공 - 본인 항목 삭제")
        void success() throws Exception {
            // given
            Long itemId = 1L;
            doNothing().when(dailyItemService).deleteItem(itemId, userId);

            // when & then
            mockMvc.perform(delete("/api/v1/dailies/{dailyId}/items/{itemId}", dailyId, itemId)
                            .header("user-id", userId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 항목")
        void fail_NotFound() throws Exception {
            // given
            Long itemId = 999L;
            doThrow(new IllegalArgumentException("존재하지 않는 데일리 항목입니다: " + itemId))
                    .when(dailyItemService).deleteItem(itemId, userId);

            // when & then
            mockMvc.perform(delete("/api/v1/dailies/{dailyId}/items/{itemId}", dailyId, itemId)
                            .header("user-id", userId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 타인의 항목 삭제 시도")
        void fail_NotOwner() throws Exception {
            // given
            Long itemId = 1L;
            doThrow(new IllegalStateException("본인의 데일리 항목만 삭제할 수 있습니다"))
                    .when(dailyItemService).deleteItem(itemId, userId);

            // when & then
            mockMvc.perform(delete("/api/v1/dailies/{dailyId}/items/{itemId}", dailyId, itemId)
                            .header("user-id", userId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dailies/{dailyId}/items - 전체 항목 조회")
    class GetAllItemsTest {

        @Test
        @DisplayName("성공 - 전체 항목 반환")
        void success() throws Exception {
            // given
            given(dailyItemService.getAllItems(dailyId))
                    .willReturn(List.of(yesterdayItem, todayItem, blockerItem));

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items", dailyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dailies/{dailyId}/items/blockers - 블로커 조회")
    class GetBlockersTest {

        @Test
        @DisplayName("성공 - 블로커만 반환")
        void success() throws Exception {
            // given
            given(dailyItemService.getBlockers(dailyId))
                    .willReturn(List.of(blockerItem));

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/blockers", dailyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].category").value("BLOCKER"))
                    .andExpect(jsonPath("$[0].content").value("DB 연결 이슈가 있습니다"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dailies/{dailyId}/items/my/count - 내 항목 개수 조회")
    class CountMyItemsTest {

        @Test
        @DisplayName("성공 - 항목 개수 반환")
        void success() throws Exception {
            // given
            given(dailyItemService.countMyItems(dailyId, userId))
                    .willReturn(3L);

            // when & then
            mockMvc.perform(get("/api/v1/dailies/{dailyId}/items/my/count", dailyId)
                            .header("user-id", userId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
        }
    }
}
