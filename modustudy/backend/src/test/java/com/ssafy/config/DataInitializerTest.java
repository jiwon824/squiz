package com.ssafy.config;

import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.RegionRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataInitializer 통합 테스트
 * - test 프로필에서는 DataInitializer가 @Profile("!test")로 비활성화되므로
 *   직접 인스턴스를 생성하여 초기 데이터 삽입 로직을 검증합니다.
 */
 @SpringBootTest
 @Transactional
 class DataInitializerTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private EntityManager entityManager;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        // 테스트 격리를 위해 기존 데이터 정리
        topicRepository.deleteAll();
        formatRepository.deleteAll();
        regionRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        dataInitializer = new DataInitializer(topicRepository, formatRepository, regionRepository);
    }

    // ============================================================
    // Topic 초기 데이터 테스트
    // ============================================================

    @Nested
    @DisplayName("Topic 초기 데이터")
    class TopicInitTest {

        @Test
        @DisplayName("대분류 10개 생성 확인")
        void initTopics_CreatesParentTopics() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            List<Topic> parents = topicRepository.findByParentIsNullOrderBySortOrderAsc();
            assertThat(parents).hasSize(10);
            assertThat(parents.get(0).getName()).isEqualTo("알고리즘/코딩테스트");
            assertThat(parents.get(1).getName()).isEqualTo("CS 기초");
            assertThat(parents.get(2).getName()).isEqualTo("프론트엔드");
            assertThat(parents.get(3).getName()).isEqualTo("백엔드");
            assertThat(parents.get(9).getName()).isEqualTo("프로젝트");
        }

        @Test
        @DisplayName("알고리즘 대분류의 세부주제 5개 생성 확인")
        void initTopics_CreatesAlgorithmChildren() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            List<Topic> parents = topicRepository.findByParentIsNullOrderBySortOrderAsc();
            Topic algorithm = parents.get(0);
            List<Topic> children = topicRepository.findByParentIdOrderBySortOrderAsc(algorithm.getId());

            assertThat(children).hasSize(5);
            assertThat(children.get(0).getName()).isEqualTo("백준");
            assertThat(children.get(1).getName()).isEqualTo("프로그래머스");
            assertThat(children.get(4).getName()).isEqualTo("코딩테스트 대비");
        }

        @Test
        @DisplayName("이미 데이터 존재 시 중복 삽입하지 않음")
        void initTopics_SkipsIfDataExists() throws Exception {
            // given
            dataInitializer.run(null);
            long countAfterFirst = topicRepository.count();

            // when
            dataInitializer.run(null);
            long countAfterSecond = topicRepository.count();

            // then
            assertThat(countAfterFirst).isEqualTo(countAfterSecond);
        }
    }

    // ============================================================
    // Format 초기 데이터 테스트
    // ============================================================

    @Nested
    @DisplayName("Format 초기 데이터")
    class FormatInitTest {

        @Test
        @DisplayName("형식 8개 생성 확인")
        void initFormats_CreatesAllFormats() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            List<Format> formats = formatRepository.findAllByOrderBySortOrderAsc();
            assertThat(formats).hasSize(8);
            assertThat(formats.get(0).getName()).isEqualTo("문제 풀이");
            assertThat(formats.get(1).getName()).isEqualTo("독서/책 스터디");
            assertThat(formats.get(7).getName()).isEqualTo("토론");
        }

        @Test
        @DisplayName("이미 데이터 존재 시 중복 삽입하지 않음")
        void initFormats_SkipsIfDataExists() throws Exception {
            // given
            dataInitializer.run(null);
            long countAfterFirst = formatRepository.count();

            // when
            dataInitializer.run(null);
            long countAfterSecond = formatRepository.count();

            // then
            assertThat(countAfterFirst).isEqualTo(countAfterSecond);
        }
    }

    // ============================================================
    // Region 초기 데이터 테스트
    // ============================================================

    @Nested
    @DisplayName("Region 초기 데이터")
    class RegionInitTest {

        @Test
        @DisplayName("시/도 17개 생성 확인")
        void initRegions_CreatesProvinces() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            List<Region> provinces = regionRepository.findByLevelOrderBySortOrderAsc(1);
            assertThat(provinces).hasSize(17);
            assertThat(provinces.get(0).getName()).isEqualTo("서울특별시");
            assertThat(provinces.get(0).getCode()).isEqualTo("SEOUL");
            assertThat(provinces.get(0).getLevel()).isEqualTo(1);
            assertThat(provinces.get(16).getName()).isEqualTo("제주특별자치도");
        }

        @Test
        @DisplayName("서울 시/군/구 25개 생성 확인")
        void initRegions_CreatesSeoulDistricts() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            List<Region> provinces = regionRepository.findByLevelOrderBySortOrderAsc(1);
            Region seoul = provinces.get(0);

            List<Region> districts = regionRepository.findByParentIdOrderBySortOrderAsc(seoul.getId());
            assertThat(districts).hasSize(25);
            assertThat(districts.get(0).getName()).isEqualTo("강남구");
            assertThat(districts.get(0).getFullName()).isEqualTo("서울특별시 강남구");
            assertThat(districts.get(0).getLevel()).isEqualTo(2);
            assertThat(districts.get(0).getParent().getId()).isEqualTo(seoul.getId());
        }

        @Test
        @DisplayName("제주 시/군/구 2개 생성 확인")
        void initRegions_CreatesJejuDistricts() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            List<Region> provinces = regionRepository.findByLevelOrderBySortOrderAsc(1);
            Region jeju = provinces.get(16);

            List<Region> districts = regionRepository.findByParentIdOrderBySortOrderAsc(jeju.getId());
            assertThat(districts).hasSize(2);
            assertThat(districts.get(0).getName()).isEqualTo("제주시");
            assertThat(districts.get(1).getName()).isEqualTo("서귀포시");
        }

        @Test
        @DisplayName("시/군/구 코드 형식 확인 (시/도코드_구코드)")
        void initRegions_DistrictCodeFormat() throws Exception {
            // when
            dataInitializer.run(null);

            // then
            Region gangnam = regionRepository.findByCode("SEOUL_GANGNAM")
                    .orElse(null);
            assertThat(gangnam).isNotNull();
            assertThat(gangnam.getName()).isEqualTo("강남구");
            assertThat(gangnam.getFullName()).isEqualTo("서울특별시 강남구");
        }

        @Test
        @DisplayName("이미 데이터 존재 시 중복 삽입하지 않음")
        void initRegions_SkipsIfDataExists() throws Exception {
            // given
            dataInitializer.run(null);
            long countAfterFirst = regionRepository.count();

            // when
            dataInitializer.run(null);
            long countAfterSecond = regionRepository.count();

            // then
            assertThat(countAfterFirst).isEqualTo(countAfterSecond);
        }
    }

    // ============================================================
    // 전체 초기화 테스트
    // ============================================================

    @Test
    @DisplayName("전체 초기 데이터 생성 - Topic, Format, Region 모두 생성")
    void run_InitializesAllData() throws Exception {
        // when
        dataInitializer.run(null);

        // then
        assertThat(topicRepository.count()).isGreaterThan(0);
        assertThat(formatRepository.count()).isEqualTo(8);
        assertThat(regionRepository.findByLevelOrderBySortOrderAsc(1)).hasSize(17);
    }
}
