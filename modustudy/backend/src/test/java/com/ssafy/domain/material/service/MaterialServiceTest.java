package com.ssafy.domain.material.service;

import com.ssafy.common.exception.MaterialException;
import com.ssafy.domain.material.dto.request.MaterialCreateRequest;
import com.ssafy.domain.material.dto.request.MaterialSearchCondition;
import com.ssafy.domain.material.dto.request.MaterialUpdateRequest;
import com.ssafy.domain.material.dto.response.MaterialCreateResponse;
import com.ssafy.domain.material.dto.response.MaterialDetailResponse;
import com.ssafy.domain.material.dto.response.MaterialListResponse;
import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import com.ssafy.domain.material.repository.MaterialCommentRepository;
import com.ssafy.domain.material.repository.MaterialRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MaterialService 통합 테스트
 *
 * 테스트 규칙:
 * - 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
 * - 부모 엔티티 저장 후 flush() 호출
 * - 벌크 삭제 후 entityManager.flush() + clear() 호출
 */
 @SpringBootTest
 @Transactional
 class MaterialServiceTest {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialCommentRepository commentRepository;

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
    private Study study;
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
                .userId("materialServiceUser1")
                .email("service1@test.com")
                .nickname("서비스테스트1")
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
                .userId("materialServiceUser2")
                .email("service2@test.com")
                .nickname("서비스테스트2")
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
        study = studyRepository.save(Study.builder()
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
    }

    @Nested
    @DisplayName("자료 목록 조회 테스트")
    class GetMaterialsTest {

        @Test
        @DisplayName("전체 자료 목록 조회 성공")
        void getMaterials_All_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료 1", "설명 1", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료 2", "설명 2", "https://test.com/2", 2));
            materialRepository.flush();

            MaterialSearchCondition condition = MaterialSearchCondition.builder().build();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MaterialListResponse> result = materialService.getMaterials(study.getId(), condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("주차 필터링 조회 성공")
        void getMaterials_FilterByWeekNumber_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "1주차 자료", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "2주차 자료", "설명", "https://test.com/2", 2));
            materialRepository.flush();

            MaterialSearchCondition condition = MaterialSearchCondition.builder()
                    .weekNumber(1)
                    .build();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MaterialListResponse> result = materialService.getMaterials(study.getId(), condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getWeekNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("타입 필터링 조회 성공")
        void getMaterials_FilterByType_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "링크 자료", "설명", "https://test.com", 1));
            materialRepository.save(Material.createFileMaterial(
                    study.getId(), user1.getId(), "파일 자료", "설명",
                    MaterialType.FILE, "/path/file.pdf", "file.pdf", 1024L, 1));
            materialRepository.flush();

            MaterialSearchCondition condition = MaterialSearchCondition.builder()
                    .type(MaterialType.LINK)
                    .build();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MaterialListResponse> result = materialService.getMaterials(study.getId(), condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getMaterialType()).isEqualTo(MaterialType.LINK);
        }

        @Test
        @DisplayName("키워드 검색 조회 성공")
        void getMaterials_SearchByKeyword_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "DP 개념 정리", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "그래프 탐색", "설명", "https://test.com/2", 1));
            materialRepository.flush();

            MaterialSearchCondition condition = MaterialSearchCondition.builder()
                    .keyword("DP")
                    .build();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MaterialListResponse> result = materialService.getMaterials(study.getId(), condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("DP");
        }

        @Test
        @DisplayName("업로더 정보 포함 확인")
        void getMaterials_ContainsUploaderInfo_Success() {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            MaterialSearchCondition condition = MaterialSearchCondition.builder().build();
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MaterialListResponse> result = materialService.getMaterials(study.getId(), condition, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUploader()).isNotNull();
            assertThat(result.getContent().get(0).getUploader().getNickname()).isEqualTo("서비스테스트1");
        }
    }

    @Nested
    @DisplayName("자료 상세 조회 테스트")
    class GetMaterialDetailTest {

        @Test
        @DisplayName("자료 상세 조회 성공")
        void getMaterialDetail_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "테스트 자료", "상세 설명", "https://test.com", 1));
            materialRepository.flush();

            // when
            MaterialDetailResponse result = materialService.getMaterialDetail(study.getId(), material.getId());

            // then
            assertThat(result.getId()).isEqualTo(material.getId());
            assertThat(result.getTitle()).isEqualTo("테스트 자료");
            assertThat(result.getDescription()).isEqualTo("상세 설명");
            assertThat(result.getUploader().getNickname()).isEqualTo("서비스테스트1");
        }

        @Test
        @DisplayName("자료 상세 조회 시 조회수 증가")
        void getMaterialDetail_IncrementsViewCount() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "테스트 자료", "설명", "https://test.com", 1));
            materialRepository.flush();
            assertThat(material.getViewCount()).isEqualTo(0);

            // when
            MaterialDetailResponse result = materialService.getMaterialDetail(study.getId(), material.getId());

            // then
            assertThat(result.getViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 자료 조회 시 예외 발생")
        void getMaterialDetail_NotFound_ThrowsException() {
            // given
            Long nonExistentId = 99999L;

            // when & then
            assertThatThrownBy(() -> materialService.getMaterialDetail(study.getId(), nonExistentId))
                    .isInstanceOf(MaterialException.MaterialNotFoundException.class);
        }

        @Test
        @DisplayName("다른 스터디 자료 조회 시 예외 발생")
        void getMaterialDetail_WrongStudy_ThrowsException() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "테스트 자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            Long wrongStudyId = study.getId() + 1000L;

            // when & then
            assertThatThrownBy(() -> materialService.getMaterialDetail(wrongStudyId, material.getId()))
                    .isInstanceOf(MaterialException.MaterialNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("링크 자료 생성 테스트")
    class CreateLinkMaterialTest {

        @Test
        @DisplayName("링크 자료 생성 성공")
        void createLinkMaterial_Success() {
            // given
            MaterialCreateRequest request = MaterialCreateRequest.builder()
                    .title("백준 문제 링크")
                    .description("이번 주 풀어야 할 문제")
                    .materialType(MaterialType.LINK)
                    .url("https://www.acmicpc.net/problem/1000")
                    .weekNumber(1)
                    .build();

            // when
            MaterialCreateResponse result = materialService.createLinkMaterial(study.getId(), user1.getId(), request);

            // then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo("백준 문제 링크");
            assertThat(result.getMaterialType()).isEqualTo(MaterialType.LINK);
        }

        @Test
        @DisplayName("링크 타입이 아닌 요청 시 예외 발생")
        void createLinkMaterial_WrongType_ThrowsException() {
            // given
            MaterialCreateRequest request = MaterialCreateRequest.builder()
                    .title("파일 자료")
                    .materialType(MaterialType.FILE)
                    .url("https://test.com")
                    .build();

            // when & then
            assertThatThrownBy(() -> materialService.createLinkMaterial(study.getId(), user1.getId(), request))
                    .isInstanceOf(MaterialException.InvalidFileTypeException.class);
        }
    }

    @Nested
    @DisplayName("파일 자료 생성 테스트")
    class CreateFileMaterialTest {

        @Test
        @DisplayName("파일 자료 생성 성공")
        void createFileMaterial_Success() {
            // when
            MaterialCreateResponse result = materialService.createFileMaterial(
                    study.getId(),
                    user1.getId(),
                    "DP 개념 정리",
                    "다이나믹 프로그래밍 기초",
                    MaterialType.FILE,
                    "/uploads/materials/dp_concept.pdf",
                    "dp_concept.pdf",
                    1024000L,
                    1
            );

            // then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo("DP 개념 정리");
            assertThat(result.getMaterialType()).isEqualTo(MaterialType.FILE);
        }

        @Test
        @DisplayName("이미지 자료 생성 성공")
        void createImageMaterial_Success() {
            // when
            MaterialCreateResponse result = materialService.createFileMaterial(
                    study.getId(),
                    user1.getId(),
                    "알고리즘 그림",
                    "트리 구조 설명",
                    MaterialType.IMAGE,
                    "/uploads/materials/tree.png",
                    "tree.png",
                    512000L,
                    2
            );

            // then
            assertThat(result.getMaterialType()).isEqualTo(MaterialType.IMAGE);
        }
    }

    @Nested
    @DisplayName("자료 수정 테스트")
    class UpdateMaterialTest {

        @Test
        @DisplayName("자료 수정 성공")
        void updateMaterial_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "원래 제목", "원래 설명", "https://test.com", 1));
            materialRepository.flush();

            MaterialUpdateRequest request = MaterialUpdateRequest.builder()
                    .title("수정된 제목")
                    .description("수정된 설명")
                    .weekNumber(2)
                    .build();

            // when
            materialService.updateMaterial(study.getId(), material.getId(), user1.getId(), request);
            entityManager.flush();
            entityManager.clear();

            // then
            Material updated = materialRepository.findById(material.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("수정된 제목");
            assertThat(updated.getDescription()).isEqualTo("수정된 설명");
            assertThat(updated.getWeekNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 수정 시 예외 발생")
        void updateMaterial_NotOwner_ThrowsException() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            MaterialUpdateRequest request = MaterialUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() ->
                    materialService.updateMaterial(study.getId(), material.getId(), user2.getId(), request))
                    .isInstanceOf(MaterialException.NotMaterialOwnerException.class);
        }

        @Test
        @DisplayName("존재하지 않는 자료 수정 시 예외 발생")
        void updateMaterial_NotFound_ThrowsException() {
            // given
            Long nonExistentId = 99999L;
            MaterialUpdateRequest request = MaterialUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() ->
                    materialService.updateMaterial(study.getId(), nonExistentId, user1.getId(), request))
                    .isInstanceOf(MaterialException.MaterialNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("자료 삭제 테스트")
    class DeleteMaterialTest {

        @Test
        @DisplayName("본인 자료 삭제 성공")
        void deleteMaterial_ByOwner_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "삭제할 자료", "설명", "https://test.com", 1));
            materialRepository.flush();
            Long materialId = material.getId();

            // when
            materialService.deleteMaterial(study.getId(), materialId, user1.getId(), false);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(materialRepository.findById(materialId)).isEmpty();
        }

        @Test
        @DisplayName("스터디장이 다른 사람 자료 삭제 성공")
        void deleteMaterial_ByLeader_Success() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user2.getId(), "user2 자료", "설명", "https://test.com", 1));
            materialRepository.flush();
            Long materialId = material.getId();

            // when - user1이 스터디장
            materialService.deleteMaterial(study.getId(), materialId, user1.getId(), true);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(materialRepository.findById(materialId)).isEmpty();
        }

        @Test
        @DisplayName("본인도 스터디장도 아닌 사용자가 삭제 시 예외 발생")
        void deleteMaterial_NotOwnerNotLeader_ThrowsException() {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            // when & then
            assertThatThrownBy(() ->
                    materialService.deleteMaterial(study.getId(), material.getId(), user2.getId(), false))
                    .isInstanceOf(MaterialException.NotMaterialOwnerException.class);
        }

        @Test
        @DisplayName("존재하지 않는 자료 삭제 시 예외 발생")
        void deleteMaterial_NotFound_ThrowsException() {
            // given
            Long nonExistentId = 99999L;

            // when & then
            assertThatThrownBy(() ->
                    materialService.deleteMaterial(study.getId(), nonExistentId, user1.getId(), false))
                    .isInstanceOf(MaterialException.MaterialNotFoundException.class);
        }
    }
}
