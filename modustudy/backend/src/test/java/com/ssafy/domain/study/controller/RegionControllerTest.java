package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.service.RegionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RegionController 통합 테스트
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 @WithMockUser(username = "testuser", roles = {"USER"})
 class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegionService regionService;

    private RegionResponse seoul;
    private RegionResponse busan;
    private RegionResponse gangnam;
    private RegionResponse songpa;
    private RegionResponse.Option seoulOption;
    private RegionResponse.Option busanOption;

    @BeforeEach
    void setUp() {
        seoul = RegionResponse.builder()
                .id(1L)
                .code("SEOUL")
                .name("서울특별시")
                .fullName("서울특별시")
                .level(1)
                .parentId(null)
                .build();

        busan = RegionResponse.builder()
                .id(2L)
                .code("BUSAN")
                .name("부산광역시")
                .fullName("부산광역시")
                .level(1)
                .parentId(null)
                .build();

        gangnam = RegionResponse.builder()
                .id(18L)
                .code("SEOUL_GANGNAM")
                .name("강남구")
                .fullName("서울특별시 강남구")
                .level(2)
                .parentId(1L)
                .build();

        songpa = RegionResponse.builder()
                .id(19L)
                .code("SEOUL_SONGPA")
                .name("송파구")
                .fullName("서울특별시 송파구")
                .level(2)
                .parentId(1L)
                .build();

        seoulOption = RegionResponse.Option.builder()
                .id(1L)
                .code("SEOUL")
                .label("서울특별시")
                .build();

        busanOption = RegionResponse.Option.builder()
                .id(2L)
                .code("BUSAN")
                .label("부산광역시")
                .build();
    }

    // ============================================================
    // GET /api/regions/provinces - 시/도 목록 조회
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/provinces - 시/도 목록 조회")
    class GetProvincesTest {

        @Test
        @DisplayName("성공 - 시/도 목록 반환")
        void success() throws Exception {
            // given
            given(regionService.getProvinces())
                    .willReturn(Arrays.asList(seoul, busan));

            // when & then
            mockMvc.perform(get("/api/regions/provinces"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("서울특별시"))
                    .andExpect(jsonPath("$[0].code").value("SEOUL"))
                    .andExpect(jsonPath("$[0].level").value(1))
                    .andExpect(jsonPath("$[0].parentId").isEmpty())
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("부산광역시"));
        }

        @Test
        @DisplayName("성공 - 빈 목록")
        void success_EmptyList() throws Exception {
            // given
            given(regionService.getProvinces())
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/regions/provinces"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ============================================================
    // GET /api/regions/provinces/options - 시/도 드롭다운 옵션
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/provinces/options - 시/도 드롭다운 옵션")
    class GetProvinceOptionsTest {

        @Test
        @DisplayName("성공 - 드롭다운 옵션 반환")
        void success() throws Exception {
            // given
            given(regionService.getProvinceOptions())
                    .willReturn(Arrays.asList(seoulOption, busanOption));

            // when & then
            mockMvc.perform(get("/api/regions/provinces/options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].code").value("SEOUL"))
                    .andExpect(jsonPath("$[0].label").value("서울특별시"));
        }
    }

    // ============================================================
    // GET /api/regions/provinces/{provinceId}/districts
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/provinces/{id}/districts - 시/군/구 목록")
    class GetDistrictsTest {

        @Test
        @DisplayName("성공 - 서울 시/군/구 반환")
        void success() throws Exception {
            // given
            Long seoulId = 1L;
            given(regionService.getDistricts(seoulId))
                    .willReturn(Arrays.asList(gangnam, songpa));

            // when & then
            mockMvc.perform(get("/api/regions/provinces/{provinceId}/districts", seoulId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(18))
                    .andExpect(jsonPath("$[0].name").value("강남구"))
                    .andExpect(jsonPath("$[0].fullName").value("서울특별시 강남구"))
                    .andExpect(jsonPath("$[0].level").value(2))
                    .andExpect(jsonPath("$[0].parentId").value(1))
                    .andExpect(jsonPath("$[1].name").value("송파구"));
        }

        @Test
        @DisplayName("성공 - 하위 지역 없음")
        void success_Empty() throws Exception {
            // given
            Long unknownId = 999L;
            given(regionService.getDistricts(unknownId))
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/regions/provinces/{provinceId}/districts", unknownId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ============================================================
    // GET /api/regions/hierarchy
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/hierarchy - 계층 구조 조회")
    class GetHierarchyTest {

        @Test
        @DisplayName("성공 - 전체 계층 구조 반환")
        void success() throws Exception {
            // given
            RegionResponse.Hierarchy seoulHierarchy = RegionResponse.Hierarchy.builder()
                    .id(1L)
                    .code("SEOUL")
                    .name("서울특별시")
                    .districts(Arrays.asList(gangnam, songpa))
                    .build();

            given(regionService.getHierarchy())
                    .willReturn(Collections.singletonList(seoulHierarchy));

            // when & then
            mockMvc.perform(get("/api/regions/hierarchy"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("서울특별시"))
                    .andExpect(jsonPath("$[0].districts", hasSize(2)))
                    .andExpect(jsonPath("$[0].districts[0].name").value("강남구"))
                    .andExpect(jsonPath("$[0].districts[1].name").value("송파구"));
        }
    }

    // ============================================================
    // GET /api/regions/{id}
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/{id} - 지역 단건 조회")
    class GetRegionByIdTest {

        @Test
        @DisplayName("성공 - 지역 조회")
        void success() throws Exception {
            // given
            given(regionService.getRegionById(18L))
                    .willReturn(gangnam);

            // when & then
            mockMvc.perform(get("/api/regions/{id}", 18L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(18))
                    .andExpect(jsonPath("$.name").value("강남구"))
                    .andExpect(jsonPath("$.fullName").value("서울특별시 강남구"))
                    .andExpect(jsonPath("$.code").value("SEOUL_GANGNAM"))
                    .andExpect(jsonPath("$.level").value(2))
                    .andExpect(jsonPath("$.parentId").value(1));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 지역")
        void fail_NotFound() throws Exception {
            // given
            given(regionService.getRegionById(999L))
                    .willThrow(new IllegalArgumentException("지역을 찾을 수 없습니다: 999"));

            // when & then
            mockMvc.perform(get("/api/regions/{id}", 999L))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // GET /api/regions/code/{code}
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/code/{code} - 코드로 조회")
    class GetRegionByCodeTest {

        @Test
        @DisplayName("성공 - 코드로 조회")
        void success() throws Exception {
            // given
            given(regionService.getRegionByCode("SEOUL"))
                    .willReturn(seoul);

            // when & then
            mockMvc.perform(get("/api/regions/code/{code}", "SEOUL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("서울특별시"))
                    .andExpect(jsonPath("$.code").value("SEOUL"));
        }
    }

    // ============================================================
    // GET /api/regions/search
    // ============================================================

    @Nested
    @DisplayName("GET /api/regions/search - 지역 검색")
    class SearchTest {

        @Test
        @DisplayName("성공 - 키워드 검색")
        void success() throws Exception {
            // given
            given(regionService.search("강남"))
                    .willReturn(Collections.singletonList(gangnam));

            // when & then
            mockMvc.perform(get("/api/regions/search")
                    .param("keyword", "강남"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("강남구"));
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void success_NoResult() throws Exception {
            // given
            given(regionService.search("없는지역"))
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/regions/search")
                    .param("keyword", "없는지역"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
