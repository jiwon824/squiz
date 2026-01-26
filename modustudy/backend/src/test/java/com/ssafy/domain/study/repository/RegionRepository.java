package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Region;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("RegionRepository 테스트")
class RegionRepositoryTest {

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
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("존재하는 코드로 조회 성공")
        void findByCode_ExistingCode_Success() {
            // when
            Optional<Region> result = regionRepository.findByCode("SEOUL");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("서울특별시");
            assertThat(result.get().getLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회 시 빈 결과")
        void findByCode_NonExistingCode_Empty() {
            // when
            Optional<Region> result = regionRepository.findByCode("INVALID");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllProvinces")
    class FindAllProvinces {

        @Test
        @DisplayName("모든 시/도 조회 성공")
        void findAllProvinces_Success() {
            // when
            List<Region> provinces = regionRepository.findAllProvinces();

            // then
            assertThat(provinces).hasSize(2);
            assertThat(provinces).extracting("name")
                    .containsExactly("서울특별시", "부산광역시");
        }

        @Test
        @DisplayName("비활성화된 시/도는 제외")
        void findAllProvinces_ExcludesInactive() {
            // given
            Region inactiveProvince = regionRepository.save(Region.builder()
                    .code("DAEGU")
                    .name("대구광역시")
                    .level(1)
                    .fullName("대구광역시")
                    .sortOrder(3)
                    .isActive(false)
                    .build());
            regionRepository.flush();
            entityManager.clear();

            // when
            List<Region> provinces = regionRepository.findAllProvinces();

            // then
            assertThat(provinces).hasSize(2);
            assertThat(provinces).extracting("code")
                    .doesNotContain("DAEGU");
        }
    }

    @Nested
    @DisplayName("findByParentId")
    class FindByParentId {

        @Test
        @DisplayName("서울의 하위 지역 조회 성공")
        void findByParentId_Seoul_Success() {
            // when
            List<Region> districts = regionRepository.findByParentId(seoul.getId());

            // then
            assertThat(districts).hasSize(2);
            assertThat(districts).extracting("name")
                    .containsExactly("강남구", "송파구");
        }

        @Test
        @DisplayName("부산의 하위 지역 조회 성공")
        void findByParentId_Busan_Success() {
            // when
            List<Region> districts = regionRepository.findByParentId(busan.getId());

            // then
            assertThat(districts).hasSize(1);
            assertThat(districts.get(0).getName()).isEqualTo("해운대구");
        }

        @Test
        @DisplayName("하위 지역이 없는 경우 빈 리스트")
        void findByParentId_NoChildren_EmptyList() {
            // when
            List<Region> districts = regionRepository.findByParentId(gangnam.getId());

            // then
            assertThat(districts).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByParentCode")
    class FindByParentCode {

        @Test
        @DisplayName("시/도 코드로 하위 지역 조회 성공")
        void findByParentCode_Success() {
            // when
            List<Region> districts = regionRepository.findByParentCode("SEOUL");

            // then
            assertThat(districts).hasSize(2);
            assertThat(districts).extracting("code")
                    .containsExactly("SEOUL_GANGNAM", "SEOUL_SONGPA");
        }
    }

    @Nested
    @DisplayName("findByLevelAndIsActiveTrueOrderBySortOrder")
    class FindByLevel {

        @Test
        @DisplayName("Level 1(시/도) 조회 성공")
        void findByLevel_Level1_Success() {
            // when
            List<Region> provinces = regionRepository.findByLevelAndIsActiveTrueOrderBySortOrder(1);

            // then
            assertThat(provinces).hasSize(2);
            assertThat(provinces).allMatch(r -> r.getLevel() == 1);
        }

        @Test
        @DisplayName("Level 2(시/군/구) 조회 성공")
        void findByLevel_Level2_Success() {
            // when
            List<Region> districts = regionRepository.findByLevelAndIsActiveTrueOrderBySortOrder(2);

            // then
            assertThat(districts).hasSize(3);
            assertThat(districts).allMatch(r -> r.getLevel() == 2);
        }
    }

    @Nested
    @DisplayName("searchByKeyword")
    class SearchByKeyword {

        @Test
        @DisplayName("이름으로 검색 성공")
        void searchByKeyword_ByName_Success() {
            // when
            List<Region> results = regionRepository.searchByKeyword("강남");

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("강남구");
        }

        @Test
        @DisplayName("전체 지역명으로 검색 성공")
        void searchByKeyword_ByFullName_Success() {
            // when
            List<Region> results = regionRepository.searchByKeyword("서울특별시");

            // then
            assertThat(results).hasSize(3); // 서울 + 강남 + 송파
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchByKeyword_NoResults() {
            // when
            List<Region> results = regionRepository.searchByKeyword("제주");

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllProvincesWithChildren")
    class FindAllProvincesWithChildren {

        @Test
        @DisplayName("시/도와 하위 지역 함께 조회 성공")
        void findAllProvincesWithChildren_Success() {
            // when
            List<Region> provinces = regionRepository.findAllProvincesWithChildren();

            // then
            assertThat(provinces).hasSize(2);

            Region seoulResult = provinces.stream()
                    .filter(r -> r.getCode().equals("SEOUL"))
                    .findFirst()
                    .orElseThrow();
            assertThat(seoulResult.getChildren()).hasSize(2);

            Region busanResult = provinces.stream()
                    .filter(r -> r.getCode().equals("BUSAN"))
                    .findFirst()
                    .orElseThrow();
            assertThat(busanResult.getChildren()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByIdIn")
    class FindByIdIn {

        @Test
        @DisplayName("여러 ID로 조회 성공")
        void findByIdIn_Success() {
            // when
            List<Region> regions = regionRepository.findByIdIn(
                    List.of(seoul.getId(), gangnam.getId(), haeundae.getId())
            );

            // then
            assertThat(regions).hasSize(3);
            assertThat(regions).extracting("code")
                    .containsExactlyInAnyOrder("SEOUL", "SEOUL_GANGNAM", "BUSAN_HAEUNDAE");
        }

        @Test
        @DisplayName("빈 리스트로 조회 시 빈 결과")
        void findByIdIn_EmptyList() {
            // when
            List<Region> regions = regionRepository.findByIdIn(List.of());

            // then
            assertThat(regions).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllActiveOrderByLevelAndSortOrder")
    class FindAllActive {

        @Test
        @DisplayName("활성화된 모든 지역 계층 순서대로 조회")
        void findAllActive_Success() {
            // when
            List<Region> regions = regionRepository.findAllActiveOrderByLevelAndSortOrder();

            // then
            assertThat(regions).hasSize(5);
            // Level 1이 먼저, 그 다음 Level 2
            assertThat(regions.get(0).getLevel()).isEqualTo(1);
            assertThat(regions.get(1).getLevel()).isEqualTo(1);
            assertThat(regions.get(2).getLevel()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("existsByCode")
    class ExistsByCode {

        @Test
        @DisplayName("존재하는 코드 확인")
        void existsByCode_Exists() {
            // when & then
            assertThat(regionRepository.existsByCode("SEOUL")).isTrue();
            assertThat(regionRepository.existsByCode("SEOUL_GANGNAM")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 코드 확인")
        void existsByCode_NotExists() {
            // when & then
            assertThat(regionRepository.existsByCode("INVALID")).isFalse();
        }
    }

    @Nested
    @DisplayName("Region 엔티티 메서드")
    class EntityMethods {

        @Test
        @DisplayName("isProvince - 시/도 확인")
        void isProvince_Success() {
            // given
            Region province = regionRepository.findByCode("SEOUL").orElseThrow();
            Region district = regionRepository.findByCode("SEOUL_GANGNAM").orElseThrow();

            // then
            assertThat(province.isProvince()).isTrue();
            assertThat(district.isProvince()).isFalse();
        }

        @Test
        @DisplayName("isDistrict - 시/군/구 확인")
        void isDistrict_Success() {
            // given
            Region province = regionRepository.findByCode("SEOUL").orElseThrow();
            Region district = regionRepository.findByCode("SEOUL_GANGNAM").orElseThrow();

            // then
            assertThat(province.isDistrict()).isFalse();
            assertThat(district.isDistrict()).isTrue();
        }

        @Test
        @DisplayName("getDisplayName - 표시명 반환")
        void getDisplayName_Success() {
            // given
            Region district = regionRepository.findByCode("SEOUL_GANGNAM").orElseThrow();

            // then
            assertThat(district.getDisplayName()).isEqualTo("서울특별시 강남구");
        }
    }
}