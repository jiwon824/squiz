package com.ssafy.domain.study.controller;

import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.repository.RegionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("RegionController 테스트")
class RegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private EntityManager entityManager;

    private Region seoul;
    private Region busan;
    private Region gangnam;
    private Region songpa;
    private Region haeundae;

    @BeforeEach
    void setUp() {
        // 1. 시/도 (Level 1) 생성
        seoul = regionRepository.save(Region.builder()
                .code("SEOUL")
                .name("서울특별시")
                .level(1)
                .fullName("서울특별시")
                .sortOrder(1)
                .isActive(true)
                .build());
        regionRepository.flush();

        busan = regionRepository.save(Region.builder()
                .code("BUSAN")
                .name("부산광역시")
                .level(1)
                .fullName("부산광역시")
                .sortOrder(2)
                .isActive(true)
                .build());
        regionRepository.flush();

        // 2. 시/군/구 (Level 2) 생성 - 서울 하위
        gangnam = regionRepository.save(Region.builder()
                .parent(seoul)
                .code("SEOUL_GANGNAM")
                .name("강남구")
                .level(2)
                .fullName("서울특별시 강남구")
                .sortOrder(1)
                .isActive(true)
                .build());
        regionRepository.flush();

        songpa = regionRepository.save(Region.builder()
                .parent(seoul)
                .code("SEOUL_SONGPA")
                .name("송파구")
                .level(2)
                .fullName("서울특별시 송파구")
                .sortOrder(2)
                .isActive(true)
                .build());
        regionRepository.flush();

        // 3. 시/군/구 (Level 2) 생성 - 부산 하위
        haeundae = regionRepository.save(Region.builder()
                .parent(busan)
                .code("BUSAN_HAEUNDAE")
                .name("해운대구")
                .level(2)
                .fullName("부산광역시 해운대구")
                .sortOrder(1)
                .isActive(true)
                .build());
        regionRepository.flush();

        entityManager.clear();
    }

    @Nested
    @DisplayName("GET /api/regions/provinces")
    class GetAllProvinces {

        @Test
        @WithMockUser
        @DisplayName("모든 시/도 목록 조회 성공")
        void getAllProvinces_Success() throws Exception {
            mockMvc.perform(get("/api/regions/provinces")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].code").value("SEOUL"))
                    .andExpect(jsonPath("$.data[1].code").value("BUSAN"));
        }
    }

    @Nested
    @DisplayName("GET /api/regions/provinces/options")
    class GetProvinceOptions {

        @Test
        @WithMockUser
        @DisplayName("시/도 드롭다운 옵션 조회 성공")
        void getProvinceOptions_Success() throws Exception {
            mockMvc.perform(get("/api/regions/provinces/options")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].label").value("서울특별시"));
        }
    }

    @Nested
    @DisplayName("GET /api/regions/hierarchy")
    class GetAllProvincesWithDistricts {

        @Test
        @WithMockUser
        @DisplayName("시/도 + 하위 지역 계층 조회 성공")
        void getAllProvincesWithDistricts_Success() throws Exception {
            mockMvc.perform(get("/api/regions/hierarchy")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].code").value("SEOUL"))
                    .andExpect(jsonPath("$.data[0].districts").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/regions/provinces/{provinceId}/districts")
    class GetDistrictsByProvinceId {

        @Test
        @WithMockUser
        @DisplayName("특정 시/도의 하위 지역 조회 성공")
        void getDistrictsByProvinceId_Success() throws Exception {
            mockMvc.perform(get("/api/regions/provinces/{provinceId}/districts", seoul.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("강남구"));
        }
    }

    @Nested
    @DisplayName("GET /api/regions/provinces/{provinceId}/districts/options")
    class GetDistrictOptions {

        @Test
        @WithMockUser
        @DisplayName("하위 지역 드롭다운 옵션 조회 성공")
        void getDistrictOptions_Success() throws Exception {
            mockMvc.perform(get("/api/regions/provinces/{provinceId}/districts/options", seoul.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].label").value("강남구"));
        }
    }

    @Nested
    @DisplayName("GET /api/regions/{id}")
    class GetRegionById {

        @Test
        @WithMockUser
        @DisplayName("ID로 지역 조회 성공")
        void getRegionById_Success() throws Exception {
            mockMvc.perform(get("/api/regions/{id}", seoul.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.code").value("SEOUL"))
                    .andExpect(jsonPath("$.data.name").value("서울특별시"));
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 ID로 조회 시 404")
        void getRegionById_NotFound() throws Exception {
            mockMvc.perform(get("/api/regions/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/regions/code/{code}")
    class GetRegionByCode {

        @Test
        @WithMockUser
        @DisplayName("코드로 지역 조회 성공")
        void getRegionByCode_Success() throws Exception {
            mockMvc.perform(get("/api/regions/code/{code}", "SEOUL_GANGNAM")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.code").value("SEOUL_GANGNAM"))
                    .andExpect(jsonPath("$.data.fullName").value("서울특별시 강남구"));
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 코드로 조회 시 404")
        void getRegionByCode_NotFound() throws Exception {
            mockMvc.perform(get("/api/regions/code/{code}", "INVALID")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/regions/search")
    class SearchRegions {

        @Test
        @WithMockUser
        @DisplayName("지역 검색 성공")
        void searchRegions_Success() throws Exception {
            mockMvc.perform(get("/api/regions/search")
                            .param("keyword", "강남")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].name").value("강남구"));
        }

        @Test
        @WithMockUser
        @DisplayName("검색 결과 없음")
        void searchRegions_NoResults() throws Exception {
            mockMvc.perform(get("/api/regions/search")
                            .param("keyword", "제주")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }
}