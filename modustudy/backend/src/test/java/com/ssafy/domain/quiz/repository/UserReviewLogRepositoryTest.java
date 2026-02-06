package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.UserReviewLog;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserReviewLogRepositoryTest {

    @Autowired
    private UserReviewLogRepository userReviewLogRepository;

    @Autowired
    private UserReviewItemRepository userReviewItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("특정 기간 동안의 사용자 학습 이력을 오름차순으로 조회한다")
    void findAllByReviewItemUserIdAndReviewedAtBetweenOrderByReviewedAtAsc_ShouldReturnLogsInOrder() {
        // given
        Long userId = 12345L;
        Long otherUserId = 67890L;

        // 1. Create Review Items
        UserReviewItem item1 = userReviewItemRepository.save(UserReviewItem.builder()
                .userId(userId)
                .contentType(ReviewContentType.COURSE_QUESTION)
                .contentId(1L)
                .build());

        UserReviewItem item2 = userReviewItemRepository.save(UserReviewItem.builder()
                .userId(userId)
                .contentType(ReviewContentType.COURSE_QUESTION)
                .contentId(2L)
                .build());

        UserReviewItem otherUserItem = userReviewItemRepository.save(UserReviewItem.builder()
                .userId(otherUserId)
                .contentType(ReviewContentType.COURSE_QUESTION)
                .contentId(3L)
                .build());

        // 2. Create Logs (Mixed dates and users)
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 12, 0);

        // Target User Logs within range
        UserReviewLog log1 = createLog(item1, baseTime.plusHours(1), true);
        UserReviewLog log2 = createLog(item2, baseTime.plusHours(2), false);
        UserReviewLog log3 = createLog(item1, baseTime.plusHours(3), true);

        // Target User Log OUT of range (Before)
        createLog(item1, baseTime.minusHours(1), true);

        // Target User Log OUT of range (After)
        createLog(item1, baseTime.plusDays(2), true);

        // Other User Log within range
        createLog(otherUserItem, baseTime.plusHours(2), true);

        entityManager.flush();
        entityManager.clear();

        // when
        LocalDateTime start = baseTime;
        LocalDateTime end = baseTime.plusDays(1);
        List<UserReviewLog> results = userReviewLogRepository
                .findAllByReviewItemUserIdAndReviewedAtBetweenOrderByReviewedAtAsc(
                        userId, start, end);

        // then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(log1.getId());
        assertThat(results.get(1).getId()).isEqualTo(log2.getId());
        assertThat(results.get(2).getId()).isEqualTo(log3.getId());

        // Verify sorting (Ascending)
        assertThat(results).isSortedAccordingTo((a, b) -> a.getReviewedAt().compareTo(b.getReviewedAt()));
    }

    private UserReviewLog createLog(UserReviewItem item, LocalDateTime reviewedAt, boolean isCorrect) {
        return userReviewLogRepository.save(UserReviewLog.builder()
                .reviewItem(item)
                .reviewedAt(reviewedAt)
                .isCorrect(isCorrect)
                .responseTimeMs(1000L)
                .stability(5.0)
                .difficulty(5.0)
                .build());
    }
}
