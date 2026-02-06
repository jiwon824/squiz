package com.ssafy.domain.retrospect.repository;

import com.ssafy.domain.retrospect.entity.Category;
import com.ssafy.domain.retrospect.entity.Retrospective;
import com.ssafy.domain.retrospect.entity.RetrospectiveItem;
import com.ssafy.domain.retrospect.entity.RetrospectiveType;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RetrospectiveItemRepositoryTest {

    @Autowired
    private RetrospectiveItemRepository retrospectiveItemRepository;

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Study study;
    private Retrospective retrospective;
    private Long retrospectiveId;
    private User user1;
    private User user2;
    private RetrospectiveItem item1;
    private RetrospectiveItem item2;
    private RetrospectiveItem item3;
    private RetrospectiveItem item4;

    @BeforeEach
    void setUp() {
        // 1. User 생성
        user1 = userRepository.save(User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .nickname("테스트유저1")
                .name("홍길동")
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
                .userId("testuser2")
                .email("test2@test.com")
                .nickname("테스트유저2")
                .name("김싸피")
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

        // 2. Topic 생성
        Topic topic = topicRepository.save(Topic.builder()
                .name("알고리즘")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 3. Format 생성
        Format format = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 4. Study 생성
        study = studyRepository.save(Study.builder()
                .leaderId(user1.getId())
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .meetingType(MeetingType.ONLINE)
                .status(Status.DRAFT)
                .maxMembers(10)
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 5, 1))
                .extensionCount(0)
                .build());
        studyRepository.flush();

        // 5. Retrospective 생성
        retrospective = retrospectiveRepository.save(Retrospective.builder()
                .studyId(study.getId())
                .createdBy(user1.getId())
                .title("1회차 회고")
                .retrospectiveType(RetrospectiveType.KPT)
                .build());
        retrospectiveRepository.flush();

        retrospectiveId = retrospective.getId();

        // 6. RetrospectiveItem 생성
        item1 = retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retrospectiveId)
                .userId(user1.getId())
                .category(Category.KEEP)
                .content("시간 약속을 잘 지켰다")
                .build());

        item2 = retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retrospectiveId)
                .userId(user1.getId())
                .category(Category.PROBLEM)
                .content("회의 시간이 너무 길었다")
                .build());

        item3 = retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retrospectiveId)
                .userId(user2.getId())
                .category(Category.KEEP)
                .content("자료 공유가 활발했다")
                .build());

        item4 = retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(retrospectiveId)
                .userId(user2.getId())
                .category(Category.TRY)
                .content("다음에는 타임키퍼를 정하자")
                .build());

        retrospectiveItemRepository.flush();
    }

    // ============================================================
    // 조회 테스트
    // ============================================================

    @Test
    @DisplayName("회고별 항목 목록 조회")
    void findByRetrospectiveId_Success() {
        // when
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveId(retrospectiveId);

        // then
        assertThat(items).hasSize(4);
        assertThat(items).extracting(RetrospectiveItem::getRetrospectiveId)
                .containsOnly(retrospectiveId);
    }

    @Test
    @DisplayName("존재하지 않는 회고 ID로 조회 시 빈 목록 반환")
    void findByRetrospectiveId_NotFound() {
        // when
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveId(999L);

        // then
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("회고별 카테고리별 항목 조회 - KEEP")
    void findByRetrospectiveIdAndCategory_Keep() {
        // when
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveIdAndCategory(retrospectiveId, Category.KEEP);

        // then
        assertThat(items).hasSize(2);
        assertThat(items).extracting(RetrospectiveItem::getCategory)
                .containsOnly(Category.KEEP);
    }

    @Test
    @DisplayName("회고별 카테고리별 항목 조회 - PROBLEM")
    void findByRetrospectiveIdAndCategory_Problem() {
        // when
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveIdAndCategory(retrospectiveId, Category.PROBLEM);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getContent()).isEqualTo("회의 시간이 너무 길었다");
    }

    @Test
    @DisplayName("회고별 카테고리별 항목 조회 - TRY")
    void findByRetrospectiveIdAndCategory_Try() {
        // when
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveIdAndCategory(retrospectiveId, Category.TRY);

        // then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getContent()).isEqualTo("다음에는 타임키퍼를 정하자");
    }

    @Test
    @DisplayName("회고 + 항목 ID로 조회")
    void findByIdAndRetrospectiveId_Success() {
        // when
        Optional<RetrospectiveItem> result = retrospectiveItemRepository
                .findByIdAndRetrospectiveId(item1.getId(), retrospectiveId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("시간 약속을 잘 지켰다");
    }

    @Test
    @DisplayName("다른 회고의 항목 ID로 조회 시 빈 Optional 반환")
    void findByIdAndRetrospectiveId_DifferentRetrospective() {
        // when
        Optional<RetrospectiveItem> result = retrospectiveItemRepository
                .findByIdAndRetrospectiveId(item1.getId(), 999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("회고별 특정 사용자의 항목 조회")
    void findByRetrospectiveIdAndUserId_Success() {
        // when
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveIdAndUserId(retrospectiveId, user1.getId());

        // then
        assertThat(items).hasSize(2);
        assertThat(items).extracting(RetrospectiveItem::getUserId)
                .containsOnly(user1.getId());
    }

    // ============================================================
    // 개수 및 존재 여부 테스트
    // ============================================================

    @Test
    @DisplayName("회고별 항목 개수 조회")
    void countByRetrospectiveId_Success() {
        // when
        Long count = retrospectiveItemRepository.countByRetrospectiveId(retrospectiveId);

        // then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("회고별 참여자 수 조회")
    void countDistinctUserByRetrospectiveId_Success() {
        // when
        Long count = retrospectiveItemRepository.countDistinctUserByRetrospectiveId(retrospectiveId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("회고에 특정 사용자 항목 존재 여부 - 존재함")
    void existsByRetrospectiveIdAndUserId_Exists() {
        // when
        boolean exists = retrospectiveItemRepository
                .existsByRetrospectiveIdAndUserId(retrospectiveId, user1.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("회고에 특정 사용자 항목 존재 여부 - 존재하지 않음")
    void existsByRetrospectiveIdAndUserId_NotExists() {
        // when
        boolean exists = retrospectiveItemRepository
                .existsByRetrospectiveIdAndUserId(retrospectiveId, 999L);

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // 삭제 테스트
    // ============================================================

    @Test
    @DisplayName("항목 단건 삭제")
    void deleteById_Success() {
        // given
        Long itemId = item1.getId();

        // when
        retrospectiveItemRepository.deleteById(itemId);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<RetrospectiveItem> result = retrospectiveItemRepository.findById(itemId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("회고별 항목 전체 삭제")
    void deleteByRetrospectiveId_Success() {
        // when
        retrospectiveItemRepository.deleteByRetrospectiveId(retrospectiveId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<RetrospectiveItem> items = retrospectiveItemRepository
                .findByRetrospectiveId(retrospectiveId);
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("다른 회고 항목은 삭제되지 않음")
    void deleteByRetrospectiveId_OnlyTargetRetrospective() {
        // given - 다른 회고 생성
        Retrospective otherRetro = retrospectiveRepository.save(Retrospective.builder()
                .studyId(study.getId())
                .createdBy(user1.getId())
                .title("다른 회고")
                .retrospectiveType(RetrospectiveType.FREE)
                .build());
        retrospectiveRepository.flush();

        retrospectiveItemRepository.save(RetrospectiveItem.builder()
                .retrospectiveId(otherRetro.getId())
                .userId(user1.getId())
                .category(Category.KEEP)
                .content("다른 회고 항목")
                .build());
        retrospectiveItemRepository.flush();

        // when
        retrospectiveItemRepository.deleteByRetrospectiveId(retrospectiveId);
        entityManager.flush();
        entityManager.clear();

        // then
        List<RetrospectiveItem> remainingItems = retrospectiveItemRepository
                .findByRetrospectiveId(otherRetro.getId());
        assertThat(remainingItems).hasSize(1);
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("항목 생성")
    void save_Success() {
        // given
        RetrospectiveItem item = RetrospectiveItem.builder()
                .retrospectiveId(retrospectiveId)
                .userId(user1.getId())
                .category(Category.TRY)
                .content("새로운 시도")
                .build();

        // when
        RetrospectiveItem saved = retrospectiveItemRepository.save(item);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo("새로운 시도");
        assertThat(saved.getCategory()).isEqualTo(Category.TRY);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("항목 내용 수정")
    void updateContent_Success() {
        // given
        RetrospectiveItem item = retrospectiveItemRepository.findById(item1.getId()).orElseThrow();

        // when
        item.updateContent("수정된 내용");
        retrospectiveItemRepository.flush();
        entityManager.clear();

        // then
        RetrospectiveItem updated = retrospectiveItemRepository.findById(item1.getId()).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("항목 단건 조회")
    void findById_Success() {
        // when
        Optional<RetrospectiveItem> result = retrospectiveItemRepository.findById(item1.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("시간 약속을 잘 지켰다");
        assertThat(result.get().getCategory()).isEqualTo(Category.KEEP);
    }
}
