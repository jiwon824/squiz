package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.RegionResponse;
import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RegionService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    private Region seoul;
    private Region busan;
    private Region gangnam;
    private Region songpa;
    private Region haeundae;

    @BeforeEach
    void setUp() {
        // 시/도 (Level 1)
        seoul = Region.builder()
                .code("SEOUL")
                .name("서울특별시")
                .fullName("서울특별시")
                .level(1)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(seoul, "id", 1L);

        busan = Region.builder()
                .code("BUSAN")
                .name("부산광역시")
                .fullName("부산광역시")
                .level(1)
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(busan, "id", 2L);

        // 시/군/구 (Level 2)
        gangnam = Region.builder()
                .code("SEOUL_GANGNAM")
                .name("강남구")
                .fullName("서울특별시 강남구")
                .level(2)
                .parent(seoul)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(gangnam, "id", 18L);

        songpa = Region.builder()
                .code("SEOUL_SONGPA")
                .name("송파구")
                .fullName("서울특별시 송파구")
                .level(2)
                .parent(seoul)
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(songpa, "id", 19L);

        haeundae = Region.builder()
                .code("BUSAN_HAEUNDAE")
                .name("해운대구")
                .fullName("부산광역시 해운대구")
                .level(2)
                .parent(busan)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(haeundae, "id", 43L);

        // 시/도에 하위 지역 연결
        ReflectionTestUtils.setField(seoul, "children", Arrays.asList(gangnam, songpa));
        ReflectionTestUtils.setField(busan, "children", Collections.singletonList(haeundae));
    }

    // ============================================================
    // 시/도 목록 조회
    // ============================================================

    @Nested
    @DisplayName("getProvinces - 시/도 목록 조회")
    class GetProvincesTest {

        @Test
        @DisplayName("성공 - 시/도 목록 반환")
        void success() {
            // given
            given(regionRepository.findByLevelOrderBySortOrderAsc(1))
                    .willReturn(Arrays.asList(seoul, busan));

            // when
            List<RegionResponse> result = regionService.getProvinces();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("서울특별시");
            assertThat(result.get(0).getLevel()).isEqualTo(1);
            assertThat(result.get(0).getParentId()).isNull();
            assertThat(result.get(1).getName()).isEqualTo("부산광역시");
            verify(regionRepository).findByLevelOrderBySortOrderAsc(1);
        }

        @Test
        @DisplayName("성공 - 빈 목록")
        void success_EmptyList() {
            // given
            given(regionRepository.findByLevelOrderBySortOrderAsc(1))
                    .willReturn(Collections.emptyList());

            // when
            List<RegionResponse> result = regionService.getProvinces();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // 시/도 드롭다운 옵션 조회
    // ============================================================

    @Nested
    @DisplayName("getProvinceOptions - 시/도 드롭다운 옵션 조회")
    class GetProvinceOptionsTest {

        @Test
        @DisplayName("성공 - 드롭다운 옵션 반환")
        void success() {
            // given
            given(regionRepository.findByLevelOrderBySortOrderAsc(1))
                    .willReturn(Arrays.asList(seoul, busan));

            // when
            List<RegionResponse.Option> result = regionService.getProvinceOptions();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLabel()).isEqualTo("서울특별시");
            assertThat(result.get(0).getCode()).isEqualTo("SEOUL");
            assertThat(result.get(1).getLabel()).isEqualTo("부산광역시");
        }
    }

    // ============================================================
    // 시/군/구 목록 조회
    // ============================================================

    @Nested
    @DisplayName("getDistricts - 시/군/구 목록 조회")
    class GetDistrictsTest {

        @Test
        @DisplayName("성공 - 서울의 시/군/구 반환")
        void success() {
            // given
            Long seoulId = 1L;
            given(regionRepository.findByParentIdOrderBySortOrderAsc(seoulId))
                    .willReturn(Arrays.asList(gangnam, songpa));

            // when
            List<RegionResponse> result = regionService.getDistricts(seoulId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("강남구");
            assertThat(result.get(0).getFullName()).isEqualTo("서울특별시 강남구");
            assertThat(result.get(0).getLevel()).isEqualTo(2);
            assertThat(result.get(0).getParentId()).isEqualTo(1L);
            assertThat(result.get(1).getName()).isEqualTo("송파구");
            verify(regionRepository).findByParentIdOrderBySortOrderAsc(seoulId);
        }

        @Test
        @DisplayName("성공 - 하위 지역 없음")
        void success_NoDistricts() {
            // given
            Long unknownId = 999L;
            given(regionRepository.findByParentIdOrderBySortOrderAsc(unknownId))
                    .willReturn(Collections.emptyList());

            // when
            List<RegionResponse> result = regionService.getDistricts(unknownId);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ============================================================
    // 시/군/구 드롭다운 옵션 조회
    // ============================================================

    @Nested
    @DisplayName("getDistrictOptions - 시/군/구 드롭다운 옵션 조회")
    class GetDistrictOptionsTest {

        @Test
        @DisplayName("성공 - 드롭다운 옵션 반환")
        void success() {
            // given
            Long seoulId = 1L;
            given(regionRepository.findByParentIdOrderBySortOrderAsc(seoulId))
                    .willReturn(Arrays.asList(gangnam, songpa));

            // when
            List<RegionResponse.Option> result = regionService.getDistrictOptions(seoulId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLabel()).isEqualTo("강남구");
            assertThat(result.get(0).getCode()).isEqualTo("SEOUL_GANGNAM");
        }
    }

    // ============================================================
    // 계층 구조 조회
    // ============================================================

    @Nested
    @DisplayName("getHierarchy - 계층 구조 조회")
    class GetHierarchyTest {

        @Test
        @DisplayName("성공 - 시/도 + 시/군/구 계층 구조 반환")
        void success() {
            // given
            given(regionRepository.findAllProvincesWithDistricts())
                    .willReturn(Arrays.asList(seoul, busan));

            // when
            List<RegionResponse.Hierarchy> result = regionService.getHierarchy();

            // then
            assertThat(result).hasSize(2);

            // 서울
            RegionResponse.Hierarchy seoulHierarchy = result.get(0);
            assertThat(seoulHierarchy.getName()).isEqualTo("서울특별시");
            assertThat(seoulHierarchy.getCode()).isEqualTo("SEOUL");
            assertThat(seoulHierarchy.getDistricts()).hasSize(2);
            assertThat(seoulHierarchy.getDistricts().get(0).getName()).isEqualTo("강남구");
            assertThat(seoulHierarchy.getDistricts().get(1).getName()).isEqualTo("송파구");

            // 부산
            RegionResponse.Hierarchy busanHierarchy = result.get(1);
            assertThat(busanHierarchy.getName()).isEqualTo("부산광역시");
            assertThat(busanHierarchy.getDistricts()).hasSize(1);
            assertThat(busanHierarchy.getDistricts().get(0).getName()).isEqualTo("해운대구");
        }
    }

    // ============================================================
    // ID로 조회
    // ============================================================

    @Nested
    @DisplayName("getRegionById - ID로 조회")
    class GetRegionByIdTest {

        @Test
        @DisplayName("성공 - 시/도 조회")
        void success_Province() {
            // given
            given(regionRepository.findById(1L))
                    .willReturn(Optional.of(seoul));

            // when
            RegionResponse result = regionService.getRegionById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("서울특별시");
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getParentId()).isNull();
        }

        @Test
        @DisplayName("성공 - 시/군/구 조회")
        void success_District() {
            // given
            given(regionRepository.findById(18L))
                    .willReturn(Optional.of(gangnam));

            // when
            RegionResponse result = regionService.getRegionById(18L);

            // then
            assertThat(result.getId()).isEqualTo(18L);
            assertThat(result.getName()).isEqualTo("강남구");
            assertThat(result.getFullName()).isEqualTo("서울특별시 강남구");
            assertThat(result.getLevel()).isEqualTo(2);
            assertThat(result.getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        void fail_NotFound() {
            // given
            given(regionRepository.findById(999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.getRegionById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지역을 찾을 수 없습니다: 999");
        }
    }

    // ============================================================
    // 코드로 조회
    // ============================================================

    @Nested
    @DisplayName("getRegionByCode - 코드로 조회")
    class GetRegionByCodeTest {

        @Test
        @DisplayName("성공 - 코드로 조회")
        void success() {
            // given
            given(regionRepository.findByCode("SEOUL_GANGNAM"))
                    .willReturn(Optional.of(gangnam));

            // when
            RegionResponse result = regionService.getRegionByCode("SEOUL_GANGNAM");

            // then
            assertThat(result.getCode()).isEqualTo("SEOUL_GANGNAM");
            assertThat(result.getName()).isEqualTo("강남구");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 코드")
        void fail_NotFound() {
            // given
            given(regionRepository.findByCode("INVALID_CODE"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionService.getRegionByCode("INVALID_CODE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지역을 찾을 수 없습니다: INVALID_CODE");
        }
    }

    // ============================================================
    // 검색
    // ============================================================

    @Nested
    @DisplayName("search - 지역 검색")
    class SearchTest {

        @Test
        @DisplayName("성공 - 키워드로 검색")
        void success() {
            // given
            given(regionRepository.searchByKeyword("강남"))
                    .willReturn(Collections.singletonList(gangnam));

            // when
            List<RegionResponse> result = regionService.search("강남");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("강남구");
            assertThat(result.get(0).getFullName()).isEqualTo("서울특별시 강남구");
        }

        @Test
        @DisplayName("성공 - 시/도명으로 검색 시 시/도 + 하위 지역 모두 반환")
        void success_SearchProvince() {
            // given
            given(regionRepository.searchByKeyword("서울"))
                    .willReturn(Arrays.asList(seoul, gangnam, songpa));

            // when
            List<RegionResponse> result = regionService.search("서울");

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getLevel()).isEqualTo(1);
            assertThat(result.get(1).getLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void success_NoResult() {
            // given
            given(regionRepository.searchByKeyword("없는지역"))
                    .willReturn(Collections.emptyList());

            // when
            List<RegionResponse> result = regionService.search("없는지역");

            // then
            assertThat(result).isEmpty();
        }
    }
}
