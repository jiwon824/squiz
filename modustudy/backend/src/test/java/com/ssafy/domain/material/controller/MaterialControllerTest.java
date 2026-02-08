package com.ssafy.domain.material.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.material.dto.request.MaterialCreateRequest;
import com.ssafy.domain.material.dto.request.MaterialUpdateRequest;
import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MaterialController 통합 테스트
 *
 * 테스트 규칙:
 * - 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
 * - 부모 엔티티 저장 후 flush() 호출
 * - 벌크 삭제 후 entityManager.flush() + clear() 호출
 */
 @SpringBootTest
 @AutoConfigureMockMvc
 @Transactional
 class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private Study study;
    private Topic topic;
    private Format format;

    private static final String BASE_URL = "/api/v1/studies/{studyId}/materials";

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
                .userId("materialCtrlUser1")
                .email("materialCtrl1@test.com")
                .nickname("자료컨트롤러1")
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
                .userId("materialCtrlUser2")
                .email("materialCtrl2@test.com")
                .nickname("자료컨트롤러2")
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
    @DisplayName("자료 목록 조회 API")
    class GetMaterialsTest {

        @Test
        @DisplayName("전체 자료 목록 조회 성공")
        void getMaterials_Success() throws Exception {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료 1", "설명 1", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료 2", "설명 2", "https://test.com/2", 2));
            materialRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("주차 필터링 조회 성공")
        void getMaterials_FilterByWeekNumber_Success() throws Exception {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "1주차 자료", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "2주차 자료", "설명", "https://test.com/2", 2));
            materialRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId())
                            .param("weekNumber", "1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].weekNumber").value(1));
        }

        @Test
        @DisplayName("타입 필터링 조회 성공")
        void getMaterials_FilterByType_Success() throws Exception {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "링크 자료", "설명", "https://test.com", 1));
            materialRepository.save(Material.createFileMaterial(
                    study.getId(), user1.getId(), "파일 자료", "설명",
                    MaterialType.FILE, "/path/file.pdf", "file.pdf", 1024L, 1));
            materialRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId())
                            .param("type", "LINK")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].materialType").value("LINK"));
        }

        @Test
        @DisplayName("키워드 검색 조회 성공")
        void getMaterials_SearchByKeyword_Success() throws Exception {
            // given
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "DP 개념 정리", "설명", "https://test.com/1", 1));
            materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "그래프 탐색", "설명", "https://test.com/2", 1));
            materialRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId())
                            .param("keyword", "DP")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("DP 개념 정리"));
        }

        @Test
        @DisplayName("페이징 조회 성공")
        void getMaterials_WithPaging_Success() throws Exception {
            // given
            for (int i = 1; i <= 25; i++) {
                materialRepository.save(Material.createLinkMaterial(
                        study.getId(), user1.getId(), "자료 " + i, "설명", "https://test.com/" + i, 1));
            }
            materialRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL, study.getId())
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(10))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }
    }

    @Nested
    @DisplayName("자료 상세 조회 API")
    class GetMaterialDetailTest {

        @Test
        @DisplayName("자료 상세 조회 성공")
        void getMaterialDetail_Success() throws Exception {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "테스트 자료", "상세 설명", "https://test.com", 1));
            materialRepository.flush();

            // when & then
            mockMvc.perform(get(BASE_URL + "/{materialId}", study.getId(), material.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(material.getId()))
                    .andExpect(jsonPath("$.title").value("테스트 자료"))
                    .andExpect(jsonPath("$.description").value("상세 설명"))
                    .andExpect(jsonPath("$.materialType").value("LINK"))
                    .andExpect(jsonPath("$.uploader.nickname").value("자료컨트롤러1"));
        }

        @Test
        @DisplayName("존재하지 않는 자료 조회 시 404")
        void getMaterialDetail_NotFound() throws Exception {
            // given
            Long nonExistentId = 99999L;

            // when & then
            mockMvc.perform(get(BASE_URL + "/{materialId}", study.getId(), nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("링크 자료 생성 API")
    class CreateLinkMaterialTest {

        @Test
        @DisplayName("링크 자료 생성 성공")
        void createLinkMaterial_Success() throws Exception {
            // given
            MaterialCreateRequest request = MaterialCreateRequest.builder()
                    .title("백준 문제 링크")
                    .description("이번 주 풀어야 할 문제")
                    .materialType(MaterialType.LINK)
                    .url("https://www.acmicpc.net/problem/1000")
                    .weekNumber(1)
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value("백준 문제 링크"))
                    .andExpect(jsonPath("$.materialType").value("LINK"));
        }

        @Test
        @DisplayName("제목 없이 생성 시 400")
        void createLinkMaterial_NoTitle_BadRequest() throws Exception {
            // given
            MaterialCreateRequest request = MaterialCreateRequest.builder()
                    .description("설명")
                    .materialType(MaterialType.LINK)
                    .url("https://test.com")
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("URL 없이 생성 시 400")
        void createLinkMaterial_NoUrl_BadRequest() throws Exception {
            // given
            MaterialCreateRequest request = MaterialCreateRequest.builder()
                    .title("자료 제목")
                    .materialType(MaterialType.LINK)
                    .build();

            // when & then
            mockMvc.perform(post(BASE_URL, study.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("자료 수정 API")
    class UpdateMaterialTest {

        @Test
        @DisplayName("자료 수정 성공")
        void updateMaterial_Success() throws Exception {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "원래 제목", "원래 설명", "https://test.com", 1));
            materialRepository.flush();

            MaterialUpdateRequest request = MaterialUpdateRequest.builder()
                    .title("수정된 제목")
                    .description("수정된 설명")
                    .weekNumber(2)
                    .build();

            // when & then
            mockMvc.perform(put(BASE_URL + "/{materialId}", study.getId(), material.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());

            // 수정 확인
            entityManager.flush();
            entityManager.clear();
            Material updated = materialRepository.findById(material.getId()).orElseThrow();
            assert updated.getTitle().equals("수정된 제목");
        }

        @Test
        @DisplayName("본인이 아닌 사용자가 수정 시 403")
        void updateMaterial_NotOwner_Forbidden() throws Exception {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            MaterialUpdateRequest request = MaterialUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when & then
            mockMvc.perform(put(BASE_URL + "/{materialId}", study.getId(), material.getId())
                            .header("User-Id", user2.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 자료 수정 시 404")
        void updateMaterial_NotFound() throws Exception {
            // given
            Long nonExistentId = 99999L;
            MaterialUpdateRequest request = MaterialUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when & then
            mockMvc.perform(put(BASE_URL + "/{materialId}", study.getId(), nonExistentId)
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("자료 삭제 API")
    class DeleteMaterialTest {

        @Test
        @DisplayName("본인 자료 삭제 성공")
        void deleteMaterial_ByOwner_Success() throws Exception {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "삭제할 자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{materialId}", study.getId(), material.getId())
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // 삭제 확인
            entityManager.flush();
            entityManager.clear();
            assert materialRepository.findById(material.getId()).isEmpty();
        }

        @Test
        @DisplayName("스터디장이 다른 사람 자료 삭제 성공")
        void deleteMaterial_ByLeader_Success() throws Exception {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user2.getId(), "user2 자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            // when & then - user1이 스터디장 (Is-Leader: true)
            mockMvc.perform(delete(BASE_URL + "/{materialId}", study.getId(), material.getId())
                            .header("User-Id", user1.getId())
                            .header("Is-Leader", "true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("본인도 스터디장도 아닌 사용자가 삭제 시 403")
        void deleteMaterial_NotOwnerNotLeader_Forbidden() throws Exception {
            // given
            Material material = materialRepository.save(Material.createLinkMaterial(
                    study.getId(), user1.getId(), "자료", "설명", "https://test.com", 1));
            materialRepository.flush();

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{materialId}", study.getId(), material.getId())
                            .header("User-Id", user2.getId())
                            .header("Is-Leader", "false")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 자료 삭제 시 404")
        void deleteMaterial_NotFound() throws Exception {
            // given
            Long nonExistentId = 99999L;

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{materialId}", study.getId(), nonExistentId)
                            .header("User-Id", user1.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}
