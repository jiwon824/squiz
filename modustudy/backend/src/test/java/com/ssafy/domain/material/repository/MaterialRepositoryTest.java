package com.ssafy.domain.material.repository;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MaterialRepository 통합 테스트
 *
 * 테스트 규칙:
 * - 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
 * - 부모 엔티티 저장 후 flush() 호출
 * - 벌크 삭제 후 entityManager.flush() + clear() 호출
 */
 @SpringBootTest
 @Transactional
 class MaterialRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Study study1;
    private Study study2;
    private Topic topic;
    private Format format;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. User 생성
        user1 = userRepository.save(User.builder()
                .userId("materialTestUser1")
                .email("material1@test.com")
                .nickname("자료테스트1")
                .name("테스트1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        user2 = userRepository.save(User.builder()
                .userId("materialTestUser2")
                .email("material2@test.com")
                .nickname("자료테스트2")
                .name("테스트2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        // 4. Study 생성
        study1 = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .topic(topic)
                .format(format)
                .name("알고리즘 스터디")
                .description("알고리즘 문제 풀이 스터디")
                .maxMembers(6)
                .studyType(StudyType.PLANNED)
                .status(Status.IN_PROGRESS)
                .recruitStartDate(LocalDate.now().minusDays(7))
                .recruitEndDate(LocalDate.now().minusDays(1))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
        studyRepository.flush();

        study2 = studyRepository.save(Study.builder()
                .leaderId(user2.getId())
                .topic(topic)
                .format(format)
                .name("CS 스터디")
                .description("CS 기초 스터디")
                .maxMembers(4)
                .studyType(StudyType.PLANNED)
                .status(Status.RECRUITING)
                .recruitStartDate(LocalDate.now())
                .recruitEndDate(LocalDate.now().plusDays(7))
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusMonths(2))
                .build());
        studyRepository.flush();
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTest {

        @Test
        @DisplayName("링크 자료 저장 성공")
        void save_LinkMaterial_Success() {
            // given
            Material material = Material.createLinkMaterial(
                    study1.getId(),
                    user1.getId(),
                    "백준 문제 링크",
                    "이번 주 풀어야 할 문제",
                    "https://www.acmicpc.net/problem/1000",
                    1
            );

            // when
            Material saved = materialRepository.save(material);
            materialRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStudyId()).isEqualTo(study1.getId());
            assertThat(saved.getUploaderId()).isEqualTo(user1.getId());
            assertThat(saved.getTitle()).isEqualTo("백준 문제 링크");
            assertThat(saved.getMaterialType()).isEqualTo(MaterialType.LINK);
            assertThat(saved.getUrl()).isEqualTo("https://www.acmicpc.net/problem/1000");
            assertThat(saved.getWeekNumber()).isEqualTo(1);
            assertThat(saved.getViewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("파일 자료 저장 성공")
        void save_FileMaterial_Success() {
            // given
            Material material = Material.createFileMaterial(
                    study1.getId(),
                    user1.getId(),
                    "DP 개념 정리",
                    "다이나믹 프로그래밍 기초",
                    MaterialType.FILE,
                    "/uploads/materials/dp_concept.pdf",
                    "dp_concept.pdf",
                    1024000L,
                    1
            );

            // when
            Material saved = materialRepository.save(material);
            materialRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getMaterialType()).isEqualTo(MaterialType.FILE);
            assertThat(saved.getFilePath()).isEqualTo("/uploads/materials/dp_concept.pdf");
            assertThat(saved.getFileName()).isEqualTo("dp_concept.pdf");
            assertThat(saved.getFileSize()).isEqualTo(1024000L);
        }

        @Test
        @DisplayName("자료 ID로 조회 성공")
        void findById_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study1.getId(),
                    user1.getId(),
                    "테스트 자료",
                    "설명",
                    "https://test.com",
                    1
            ));
            materialRepository.flush();

            // when
            Optional<Material> found = materialRepository.findById(material.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("테스트 자료");
        }

        @Test
        @DisplayName("자료 수정 성공")
        void update_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study1.getId(),
                    user1.getId(),
                    "원래 제목",
                    "원래 설명",
                    "https://test.com",
                    1
            ));
            materialRepository.flush();

            // when
            material.update("수정된 제목", "수정된 설명", 2);
            entityManager.flush();
            entityManager.clear();

            // then
            Material found = materialRepository.findById(material.getId()).orElseThrow();
            assertThat(found.getTitle()).isEqualTo("수정된 제목");
            assertThat(found.getDescription()).isEqualTo("수정된 설명");
            assertThat(found.getWeekNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("자료 삭제 성공")
        void delete_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study1.getId(),
                    user1.getId(),
                    "삭제할 자료",
                    "설명",
                    "https://test.com",
                    1
            ));
            materialRepository.flush();
            Long materialId = material.getId();

            // when
            materialRepository.delete(material);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<Material> found = materialRepository.findById(materialId);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("스터디 ID 기반 조회 테스트")
    class FindByStudyIdTest {

        @Test
        @DisplayName("스터디 ID로 자료 목록 조회 - 페이징")
        void findByStudyId_WithPaging_Success() {
            // given
            for (int i = 1; i <= 15; i++) {
                materialRepository.save(Material.createLinkMaterial(
                        study1.getId(),
                        user1.getId(),
                        "자료 " + i,
                        "설명 " + i,
                        "https://test.com/" + i,
                        i % 3 + 1
                ));
            }
            materialRepository.flush();

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

            // when
            Page<Material> result = materialRepository.findByStudyId(study1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(10);
            assertThat(result.getTotalElements()).isEqualTo(15);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("스터디 ID로 자료 목록 조회 - 전체")
        void findByStudyId_All_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "자료 1", "설명 1", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "자료 2", "설명 2", "https://test.com/2", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study2.getId(), user2.getId(), "다른 스터디 자료", "설명", "https://test.com/3", 1));
            materialRepository.flush();

            // when
            List<Material> result = materialRepository.findByStudyId(study1.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(m -> m.getStudyId().equals(study1.getId()));
        }

        @Test
        @DisplayName("스터디 ID + 자료 ID로 조회")
        void findByIdAndStudyId_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "테스트 자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            // when
            Optional<Material> found = materialRepository.findByIdAndStudyId(material.getId(), study1.getId());
            Optional<Material> notFound = materialRepository.findByIdAndStudyId(material.getId(), study2.getId());

            // then
            assertThat(found).isPresent();
            assertThat(notFound).isEmpty();
        }

        @Test
        @DisplayName("스터디 ID로 자료 개수 조회")
        void countByStudyId_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "자료 1", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "자료 2", "설명", "https://test.com/2", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study2.getId(), user2.getId(), "다른 스터디", "설명", "https://test.com/3", 1));
            materialRepository.flush();

            // when
            long count = materialRepository.countByStudyId(study1.getId());

            // then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("필터링 조회 테스트")
    class FilteringTest {

        @Test
        @DisplayName("주차별 자료 조회")
        void findByStudyIdAndWeekNumber_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "1주차 자료", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "2주차 자료", "설명", "https://test.com/2", 2));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "1주차 자료2", "설명", "https://test.com/3", 1));
            materialRepository.flush();

            // when
            List<Material> week1Materials = materialRepository
                    .findByStudyIdAndWeekNumber(study1.getId(), 1);

            // then
            assertThat(week1Materials).hasSize(2);
            assertThat(week1Materials).allMatch(m -> m.getWeekNumber().equals(1));
        }

        @Test
        @DisplayName("자료 타입별 조회")
        void findByStudyIdAndMaterialType_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "링크 자료", "설명", "https://test.com", 1));
            materialRepository.save(Material.createFileMaterial(
                    study1.getId(), user1.getId(), "파일 자료", "설명",
                    MaterialType.FILE, "/path/file.pdf", "file.pdf", 1024L, 1));
            materialRepository.save(Material.createFileMaterial(
                    study1.getId(), user1.getId(), "이미지 자료", "설명",
                    MaterialType.IMAGE, "/path/image.png", "image.png", 2048L, 1));
            materialRepository.flush();

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> linkMaterials = materialRepository
                    .findByStudyIdAndMaterialType(study1.getId(), MaterialType.LINK, pageable);
            Page<Material> fileMaterials = materialRepository
                    .findByStudyIdAndMaterialType(study1.getId(), MaterialType.FILE, pageable);

            // then
            assertThat(linkMaterials.getContent()).hasSize(1);
            assertThat(fileMaterials.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("QueryDSL 동적 검색 테스트")
    class DynamicSearchTest {

        @BeforeEach
        void setUpMaterials() {
            // 다양한 조건의 자료 생성
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "1주차 DP 링크", "DP 설명", "https://test.com/1", 1));
            materialRepository.save(Material.createFileMaterial(
                    study1.getId(), user1.getId(), "1주차 그래프 파일", "그래프 설명",
                    MaterialType.FILE, "/path/graph.pdf", "graph.pdf", 1024L, 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "2주차 DP 링크", "DP 설명", "https://test.com/2", 2));
            materialRepository.save(Material.createFileMaterial(
                    study1.getId(), user1.getId(), "2주차 이미지", "이미지 설명",
                    MaterialType.IMAGE, "/path/image.png", "image.png", 2048L, 2));
            materialRepository.flush();
        }

        @Test
        @DisplayName("스터디 ID만으로 전체 조회")
        void searchMaterials_OnlyStudyId_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), null, null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(4);
        }

        @Test
        @DisplayName("스터디 ID + 주차로 조회")
        void searchMaterials_WithWeekNumber_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), 1, null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(m -> m.getWeekNumber().equals(1));
        }

        @Test
        @DisplayName("스터디 ID + 자료 타입으로 조회")
        void searchMaterials_WithMaterialType_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), null, MaterialType.LINK, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(m -> m.getMaterialType() == MaterialType.LINK);
        }

        @Test
        @DisplayName("스터디 ID + 키워드로 조회 - 제목 검색")
        void searchMaterials_WithKeyword_Title_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), null, null, "DP", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("스터디 ID + 키워드로 조회 - 설명 검색")
        void searchMaterials_WithKeyword_Description_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), null, null, "그래프", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("복합 조건 검색 - 주차 + 타입")
        void searchMaterials_WithMultipleConditions_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), 1, MaterialType.LINK, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getWeekNumber()).isEqualTo(1);
            assertThat(result.getContent().get(0).getMaterialType()).isEqualTo(MaterialType.LINK);
        }

        @Test
        @DisplayName("복합 조건 검색 - 주차 + 키워드")
        void searchMaterials_WithWeekAndKeyword_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), 1, null, "DP", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("1주차");
            assertThat(result.getContent().get(0).getTitle()).contains("DP");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchMaterials_NoResult_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Material> result = materialRepository.searchMaterials(
                    study1.getId(), 99, null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("벌크 삭제 테스트")
    class BulkDeleteTest {

        @Test
        @DisplayName("스터디 ID로 자료 전체 삭제")
        void deleteByStudyId_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "자료 1", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "자료 2", "설명", "https://test.com/2", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study2.getId(), user2.getId(), "다른 스터디 자료", "설명", "https://test.com/3", 1));
            materialRepository.flush();

            // when
            materialRepository.deleteByStudyId(study1.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            List<Material> study1Materials = materialRepository.findByStudyId(study1.getId());
            List<Material> study2Materials = materialRepository.findByStudyId(study2.getId());

            assertThat(study1Materials).isEmpty();
            assertThat(study2Materials).hasSize(1);
        }
    }

    @Nested
    @DisplayName("조회수 테스트")
    class ViewCountTest {

        @Test
        @DisplayName("조회수 증가")
        void incrementViewCount_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "테스트 자료", "설명", "https://test.com", 1));
            materialRepository.flush();
            assertThat(material.getViewCount()).isEqualTo(0);

            // when
            material.incrementViewCount();
            material.incrementViewCount();
            material.incrementViewCount();
            entityManager.flush();
            entityManager.clear();

            // then
            Material found = materialRepository.findById(material.getId()).orElseThrow();
            assertThat(found.getViewCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("업로더 관련 테스트")
    class UploaderTest {

        @Test
        @DisplayName("업로더 ID로 자료 목록 조회")
        void findByUploaderId_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "user1 자료 1", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "user1 자료 2", "설명", "https://test.com/2", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user2.getId(), "user2 자료", "설명", "https://test.com/3", 1));
            materialRepository.flush();

            // when
            List<Material> user1Materials = materialRepository.findByUploaderId(user1.getId());

            // then
            assertThat(user1Materials).hasSize(2);
            assertThat(user1Materials).allMatch(m -> m.getUploaderId().equals(user1.getId()));
        }

        @Test
        @DisplayName("업로더 확인 - 본인 여부")
        void isUploader_Test() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study1.getId(), user1.getId(), "테스트", "설명", "https://test.com", 1));
            materialRepository.flush();

            // when & then
            assertThat(material.isUploader(user1.getId())).isTrue();
            assertThat(material.isUploader(user2.getId())).isFalse();
        }
    }
}
