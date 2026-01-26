package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.repository.RegionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("RegionService 테스트")
class RegionServiceTest {

    @Autowired
    private RegionService regionService;

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
    @DisplayName("getAllProvinces")
    class GetAllProvinces {

        @Test
        @DisplayName("모든 시/도 조회 성공")
        void getAllProvinces_Success() {
            // when
            List<RegionResponse.Detail> result = regionService.getAllProvinces();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("name")
                    .containsExactly("서울특별시", "부산광역시");
            assertThat(result).allMatch(r -> r.getLevel() == 1);
        }
    }

    @Nested
    @DisplayName("getProvinceOptions")
    class GetProvinceOptions {

        @Test
        @DisplayName("시/도 드롭다운 옵션 조회 성공")
        void getProvinceOptions_Success() {
            // when
            List<RegionResponse.SelectOption> result = regionService.getProvinceOptions();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("label")
                    .containsExactly("서울특별시", "부산광역시");
            assertThat(result.get(0).getCode()).isEqualTo("SEOUL");
        }
    }

    @Nested
    @DisplayName("getDistrictsByProvinceId")
    class GetDistrictsByProvinceId {

        @Test
        @DisplayName("서울의 하위 지역 조회 성공")
        void getDistrictsByProvinceId_Success() {
            // when
            List<RegionResponse.Detail> result = regionService.getDistrictsByProvinceId(seoul.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("name")
                    .containsExactly("강남구", "송파구");
            assertThat(result).allMatch(r -> r.getLevel() == 2);
        }

        @Test
        @DisplayName("하위 지역이 없는 경우 빈 리스트")
        void getDistrictsByProvinceId_NoChildren() {
            // when
            List<RegionResponse.Detail> result = regionService.getDistrictsByProvinceId(gangnam.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDistrictsByProvinceCode")
    class GetDistrictsByProvinceCode {

        @Test
        @DisplayName("시/도 코드로 하위 지역 조회 성공")
        void getDistrictsByProvinceCode_Success() {
            // when
            List<RegionResponse.Detail> result = regionService.getDistrictsByProvinceCode("BUSAN");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("해운대구");
        }
    }

    @Nested
    @DisplayName("getDistrictOptions")
    class GetDistrictOptions {

        @Test
        @DisplayName("하위 지역 드롭다운 옵션 조회 성공")
        void getDistrictOptions_Success() {
            // when
            List<RegionResponse.SelectOption> result = regionService.getDistrictOptions(seoul.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("label")
                    .containsExactly("강남구", "송파구");
        }
    }

    @Nested
    @DisplayName("getAllProvincesWithDistricts")
    class GetAllProvincesWithDistricts {

        @Test
        @DisplayName("시/도 + 하위 지역 계층 조회 성공")
        void getAllProvincesWithDistricts_Success() {
            // when
            List<RegionResponse.ProvinceWithDistricts> result = regionService.getAllProvincesWithDistricts();

            // then
            assertThat(result).hasSize(2);

            RegionResponse.ProvinceWithDistricts seoulResult = result.stream()
                    .filter(r -> r.getCode().equals("SEOUL"))
                    .findFirst()
                    .orElseThrow();
            assertThat(seoulResult.getDistricts()).hasSize(2);
            assertThat(seoulResult.getDistricts()).extracting("name")
                    .containsExactlyInAnyOrder("강남구", "송파구");

            RegionResponse.ProvinceWithDistricts busanResult = result.stream()
                    .filter(r -> r.getCode().equals("BUSAN"))
                    .findFirst()
                    .orElseThrow();
            assertThat(busanResult.getDistricts()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getRegionById")
    class GetRegionById {

        @Test
        @DisplayName("ID로 지역 조회 성공")
        void getRegionById_Success() {
            // when
            RegionResponse.Detail result = regionService.getRegionById(seoul.getId());

            // then
            assertThat(result.getId()).isEqualTo(seoul.getId());
            assertThat(result.getCode()).isEqualTo("SEOUL");
            assertThat(result.getName()).isEqualTo("서울특별시");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외")
        void getRegionById_NotFound() {
            // when & then
            assertThatThrownBy(() -> regionService.getRegionById(999L))
                    .isInstanceOf(RegionService.RegionNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getRegionByCode")
    class GetRegionByCode {

        @Test
        @DisplayName("코드로 지역 조회 성공")
        void getRegionByCode_Success() {
            // when
            RegionResponse.Detail result = regionService.getRegionByCode("SEOUL_GANGNAM");

            // then
            assertThat(result.getCode()).isEqualTo("SEOUL_GANGNAM");
            assertThat(result.getName()).isEqualTo("강남구");
            assertThat(result.getFullName()).isEqualTo("서울특별시 강남구");
            assertThat(result.getParentId()).isEqualTo(seoul.getId());
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회 시 예외")
        void getRegionByCode_NotFound() {
            // when & then
            assertThatThrownBy(() -> regionService.getRegionByCode("INVALID"))
                    .isInstanceOf(RegionService.RegionNotFoundException.class)
                    .hasMessageContaining("INVALID");
        }
    }

    @Nested
    @DisplayName("searchRegions")
    class SearchRegions {

        @Test
        @DisplayName("이름으로 검색 성공")
        void searchRegions_ByName() {
            // when
            List<RegionResponse.Detail> result = regionService.searchRegions("강남");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("강남구");
        }

        @Test
        @DisplayName("전체 지역명으로 검색 성공")
        void searchRegions_ByFullName() {
            // when
            List<RegionResponse.Detail> result = regionService.searchRegions("부산광역시");

            // then
            assertThat(result).hasSize(2); // 부산 + 해운대
            assertThat(result).extracting("code")
                    .containsExactlyInAnyOrder("BUSAN", "BUSAN_HAEUNDAE");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchRegions_NoResults() {
            // when
            List<RegionResponse.Detail> result = regionService.searchRegions("제주");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRegionEntityById")
    class GetRegionEntityById {

        @Test
        @DisplayName("엔티티 조회 성공")
        void getRegionEntityById_Success() {
            // when
            Region result = regionService.getRegionEntityById(gangnam.getId());

            // then
            assertThat(result.getId()).isEqualTo(gangnam.getId());
            assertThat(result.getCode()).isEqualTo("SEOUL_GANGNAM");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외")
        void getRegionEntityById_NotFound() {
            // when & then
            assertThatThrownBy(() -> regionService.getRegionEntityById(999L))
                    .isInstanceOf(RegionService.RegionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRegionEntityByCode")
    class GetRegionEntityByCode {

        @Test
        @DisplayName("코드로 엔티티 조회 성공")
        void getRegionEntityByCode_Success() {
            // when
            Region result = regionService.getRegionEntityByCode("BUSAN_HAEUNDAE");

            // then
            assertThat(result.getCode()).isEqualTo("BUSAN_HAEUNDAE");
            assertThat(result.getName()).isEqualTo("해운대구");
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회 시 예외")
        void getRegionEntityByCode_NotFound() {
            // when & then
            assertThatThrownBy(() -> regionService.getRegionEntityByCode("INVALID"))
                    .isInstanceOf(RegionService.RegionNotFoundException.class);
        }
    }
}