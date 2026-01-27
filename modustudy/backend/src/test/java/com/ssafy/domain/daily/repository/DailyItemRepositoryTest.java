package com.ssafy.domain.daily.repository;

import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.entity.DailyItem;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DailyItemRepositoryTest {

    @Autowired
    private DailyItemRepository dailyItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Long dailyReportId;
    private DailyItem yesterdayItem1;
    private DailyItem todayItem1;
    private DailyItem blockerItem1;
    private DailyItem yesterdayItem2;

    @BeforeEach
    void setUp() {
        // User 생성
        user1 = userRepository.save(User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .nickname("테스트유저1")
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
                .userId("testuser2")
                .email("test2@test.com")
                .nickname("테스트유저2")
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

        // DailyReport ID (실제 테이블이 있다면 생성 후 사용, 여기선 임의 ID 사용)
        dailyReportId = 1L;

        // User1의 데일리 항목들
        yesterdayItem1 = dailyItemRepository.save(DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(user1.getId())
                .category(DailyCategory.YESTERDAY)
                .content("어제 API 설계 완료했습니다")
                .createdAt(LocalDateTime.now())
                .build());

        todayItem1 = dailyItemRepository.save(DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(user1.getId())
                .category(DailyCategory.TODAY)
                .content("오늘 구현 시작할 예정입니다")
                .createdAt(LocalDateTime.now())
                .build());

        blockerItem1 = dailyItemRepository.save(DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(user1.getId())
                .category(DailyCategory.BLOCKER)
                .content("DB 연결 이슈가 있습니다")
                .createdAt(LocalDateTime.now())
                .build());

        // User2의 데일리 항목
        yesterdayItem2 = dailyItemRepository.save(DailyItem.builder()
                .dailyReportId(dailyReportId)
                .userId(user2.getId())
                .category(DailyCategory.YESTERDAY)
                .content("어제 테스트 코드 작성했습니다")
                .createdAt(LocalDateTime.now())
                .build());

        dailyItemRepository.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByDailyReportIdOrderByCreatedAtAsc - 데일리 리포트의 전체 항목 조회")
    class FindByDailyReportIdTest {

        @Test
        @DisplayName("성공 - 모든 항목 조회")
        void success() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdOrderByCreatedAtAsc(dailyReportId);

            // then
            assertThat(items).hasSize(4);
        }

        @Test
        @DisplayName("성공 - 존재하지 않는 리포트 ID")
        void success_NotFound() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdOrderByCreatedAtAsc(999L);

            // then
            assertThat(items).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDailyReportIdAndUserIdOrderByCreatedAtAsc - 특정 사용자의 항목 조회")
    class FindByDailyReportIdAndUserIdTest {

        @Test
        @DisplayName("성공 - User1의 항목 3개 조회")
        void success_User1() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdAndUserIdOrderByCreatedAtAsc(dailyReportId, user1.getId());

            // then
            assertThat(items).hasSize(3);
            assertThat(items).allMatch(item -> item.getUserId().equals(user1.getId()));
        }

        @Test
        @DisplayName("성공 - User2의 항목 1개 조회")
        void success_User2() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdAndUserIdOrderByCreatedAtAsc(dailyReportId, user2.getId());

            // then
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getContent()).isEqualTo("어제 테스트 코드 작성했습니다");
        }
    }

    @Nested
    @DisplayName("findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc - 특정 사용자의 특정 카테고리 항목 조회")
    class FindByDailyReportIdAndUserIdAndCategoryTest {

        @Test
        @DisplayName("성공 - User1의 YESTERDAY 항목 조회")
        void success_Yesterday() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc(
                            dailyReportId, user1.getId(), DailyCategory.YESTERDAY);

            // then
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getCategory()).isEqualTo(DailyCategory.YESTERDAY);
            assertThat(items.get(0).getContent()).contains("API 설계");
        }

        @Test
        @DisplayName("성공 - User1의 BLOCKER 항목 조회")
        void success_Blocker() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdAndUserIdAndCategoryOrderByCreatedAtAsc(
                            dailyReportId, user1.getId(), DailyCategory.BLOCKER);

            // then
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getCategory()).isEqualTo(DailyCategory.BLOCKER);
            assertThat(items.get(0).getContent()).contains("DB 연결");
        }
    }

    @Nested
    @DisplayName("findByDailyReportIdAndCategoryOrderByCreatedAtAsc - 특정 카테고리의 모든 항목 조회")
    class FindByDailyReportIdAndCategoryTest {

        @Test
        @DisplayName("성공 - 전체 YESTERDAY 항목 조회")
        void success_AllYesterday() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdAndCategoryOrderByCreatedAtAsc(
                            dailyReportId, DailyCategory.YESTERDAY);

            // then
            assertThat(items).hasSize(2);  // user1 + user2
            assertThat(items).allMatch(item -> item.getCategory() == DailyCategory.YESTERDAY);
        }

        @Test
        @DisplayName("성공 - 전체 BLOCKER 항목 조회")
        void success_AllBlockers() {
            // when
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdAndCategoryOrderByCreatedAtAsc(
                            dailyReportId, DailyCategory.BLOCKER);

            // then
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getContent()).contains("DB 연결");
        }
    }

    @Nested
    @DisplayName("countByDailyReportId - 데일리 리포트 항목 개수 조회")
    class CountByDailyReportIdTest {

        @Test
        @DisplayName("성공 - 전체 항목 개수")
        void success() {
            // when
            long count = dailyItemRepository.countByDailyReportId(dailyReportId);

            // then
            assertThat(count).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("countByDailyReportIdAndUserId - 특정 사용자의 항목 개수 조회")
    class CountByDailyReportIdAndUserIdTest {

        @Test
        @DisplayName("성공 - User1의 항목 개수")
        void success_User1() {
            // when
            long count = dailyItemRepository.countByDailyReportIdAndUserId(
                    dailyReportId, user1.getId());

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - User2의 항목 개수")
        void success_User2() {
            // when
            long count = dailyItemRepository.countByDailyReportIdAndUserId(
                    dailyReportId, user2.getId());

            // then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteByDailyReportId - 데일리 리포트의 모든 항목 삭제")
    class DeleteByDailyReportIdTest {

        @Test
        @DisplayName("성공 - 전체 항목 삭제")
        void success() {
            // when
            dailyItemRepository.deleteByDailyReportId(dailyReportId);
            entityManager.flush();
            entityManager.clear();

            // then
            List<DailyItem> items = dailyItemRepository
                    .findByDailyReportIdOrderByCreatedAtAsc(dailyReportId);
            assertThat(items).isEmpty();
        }
    }

    @Nested
    @DisplayName("기본 CRUD")
    class BasicCrudTest {

        @Test
        @DisplayName("성공 - 항목 생성")
        void create_Success() {
            // given
            DailyItem newItem = DailyItem.builder()
                    .dailyReportId(dailyReportId)
                    .userId(user1.getId())
                    .category(DailyCategory.TODAY)
                    .content("새로운 할 일입니다")
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            DailyItem saved = dailyItemRepository.save(newItem);
            dailyItemRepository.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getContent()).isEqualTo("새로운 할 일입니다");
        }

        @Test
        @DisplayName("성공 - 항목 조회")
        void findById_Success() {
            // when
            DailyItem found = dailyItemRepository.findById(yesterdayItem1.getId())
                    .orElseThrow();

            // then
            assertThat(found.getContent()).contains("API 설계");
            assertThat(found.getCategory()).isEqualTo(DailyCategory.YESTERDAY);
        }

        @Test
        @DisplayName("성공 - 항목 삭제")
        void delete_Success() {
            // given
            Long itemId = blockerItem1.getId();

            // when
            dailyItemRepository.deleteById(itemId);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(dailyItemRepository.findById(itemId)).isEmpty();
        }
    }
}