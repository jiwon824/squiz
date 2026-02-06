package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.LeaderReview;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * LeaderReviewRepository 테스트
 */
 @SpringBootTest
 @Transactional
 class LeaderReviewRepositoryTest {

    @Autowired
    private LeaderReviewRepository leaderReviewRepository;

    @Autowired
    private UserRepository userRepository;

    private Long leaderId;
    private Long reviewerId2;

    @BeforeEach
    void setUp() {
        // 스터디장 생성
        User leader = User.builder()
                .userId("leader123")
                .email("leader@test.com")
                .nickname("리더")
                .name("김리더")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(5)
                .levelName("Gold")
                .leaderRating(0.0f)
                .leaderReviewCount(0)
                .build();
        leader = userRepository.save(leader);
        leaderId = leader.getId();

        // 평가자 1
        User reviewer1 = User.builder()
                .userId("reviewer1")
                .email("reviewer1@test.com")
                .nickname("평가자1")
                .name("이평가")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        reviewer1 = userRepository.save(reviewer1);

        // 평가자 2
        User reviewer2 = User.builder()
                .userId("reviewer2")
                .email("reviewer2@test.com")
                .nickname("평가자2")
                .name("박평가")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
        reviewer2 = userRepository.save(reviewer2);
        reviewerId2 = reviewer2.getId();

        // 리뷰 데이터 생성 (studyId=1L)
        LeaderReview review1 = LeaderReview.builder()
                .studyId(1L)
                .reviewerId(reviewer1.getId())
                .leaderId(leaderId)
                .rating(new BigDecimal("5.0"))
                .comment("정말 좋은 스터디장이었습니다!")
                .build();
        leaderReviewRepository.save(review1);

        LeaderReview review2 = LeaderReview.builder()
                .studyId(1L)
                .reviewerId(reviewerId2)
                .leaderId(leaderId)
                .rating(new BigDecimal("4.0"))
                .comment("체계적인 진행이 좋았습니다.")
                .build();
        leaderReviewRepository.save(review2);
    }

    // ============================================================
    // 리뷰 조회 테스트
    // ============================================================

    @Test
    @DisplayName("특정 스터디장의 리뷰 목록 조회 - 페이징")
    void findByLeaderId_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<LeaderReview> reviews = leaderReviewRepository.findByLeaderId(leaderId, pageable);

        // then
        assertThat(reviews).isNotNull();
        assertThat(reviews.getContent()).hasSize(2);
        assertThat(reviews.getContent()).allMatch(review -> review.getLeaderId().equals(leaderId));
    }

    @Test
    @DisplayName("특정 스터디장의 리뷰 개수 조회")
    void countByLeaderId_Success() {
        // when
        Long count = leaderReviewRepository.countByLeaderId(leaderId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 스터디장의 평균 평점 계산")
    void calculateAverageRating_Success() {
        // when
        Double avgRating = leaderReviewRepository.calculateAverageRating(leaderId);

        // then
        assertThat(avgRating).isNotNull();
        // (5.0 + 4.0) / 2 = 4.5
        assertThat(avgRating).isEqualTo(4.5);
    }

    @Test
    @DisplayName("리뷰가 없는 스터디장의 평균 평점 - null 반환")
    void calculateAverageRating_NoReviews_ReturnsNull() {
        // given
        Long leaderIdWithNoReviews = 999L;

        // when
        Double avgRating = leaderReviewRepository.calculateAverageRating(leaderIdWithNoReviews);

        // then
        assertThat(avgRating).isNull();
    }

    @Test
    @DisplayName("특정 스터디의 리뷰 조회")
    void findByStudyId_Success() {
        // given
        Long studyId = 1L;

        // when
        List<LeaderReview> reviews = leaderReviewRepository.findByStudyId(studyId);

        // then
        assertThat(reviews).isNotNull();
        assertThat(reviews).hasSize(2);
        assertThat(reviews).allMatch(review -> review.getStudyId().equals(studyId));
    }

    // ============================================================
    // 중복 체크 테스트
    // ============================================================

    @Test
    @DisplayName("이미 리뷰를 작성한 사용자 - 존재함")
    void existsByStudyIdAndReviewerId_Exists() {
        // given
        Long studyId = 1L;

        // when
        boolean exists = leaderReviewRepository.existsByStudyIdAndReviewerId(studyId, reviewerId2);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("리뷰를 작성하지 않은 사용자 - 존재하지 않음")
    void existsByStudyIdAndReviewerId_NotExists() {
        // given
        Long studyId = 1L;
        Long reviewerId = 999L;

        // when
        boolean exists = leaderReviewRepository.existsByStudyIdAndReviewerId(studyId, reviewerId);

        // then
        assertThat(exists).isFalse();
    }

    // ============================================================
    // CRUD 기본 테스트
    // ============================================================

    @Test
    @DisplayName("리뷰 생성 테스트")
    void save_Success() {
        // given
        LeaderReview review = LeaderReview.builder()
                .studyId(2L)
                .reviewerId(5L)
                .leaderId(leaderId)
                .rating(new BigDecimal("4.5"))
                .comment("새로운 리뷰입니다.")
                .build();

        // when
        LeaderReview saved = leaderReviewRepository.save(review);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyId()).isEqualTo(2L);
        assertThat(saved.getReviewerId()).isEqualTo(5L);
        assertThat(saved.getLeaderId()).isEqualTo(leaderId);
        assertThat(saved.getRating()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(saved.getComment()).isEqualTo("새로운 리뷰입니다.");
    }

    @Test
    @DisplayName("리뷰 수정 테스트")
    void update_Success() {
        // given
        LeaderReview review = LeaderReview.builder()
                .studyId(3L)
                .reviewerId(6L)
                .leaderId(leaderId)
                .rating(new BigDecimal("3.0"))
                .comment("초기 리뷰")
                .build();
        LeaderReview saved = leaderReviewRepository.save(review);

        // when
        saved.setRating(new BigDecimal("5.0"));
        saved.setComment("수정된 리뷰");
        LeaderReview updated = leaderReviewRepository.save(saved);

        // then
        assertThat(updated.getRating()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(updated.getComment()).isEqualTo("수정된 리뷰");
    }

    @Test
    @DisplayName("리뷰 삭제 테스트")
    void delete_Success() {
        // given
        LeaderReview review = LeaderReview.builder()
                .studyId(4L)
                .reviewerId(7L)
                .leaderId(leaderId)
                .rating(new BigDecimal("4.0"))
                .comment("삭제될 리뷰")
                .build();
        LeaderReview saved = leaderReviewRepository.save(review);
        Long reviewId = saved.getId();

        // when
        leaderReviewRepository.delete(saved);

        // then
        assertThat(leaderReviewRepository.findById(reviewId)).isEmpty();
    }

    @Test
    @DisplayName("리뷰 ID로 조회")
    void findById_Success() {
        // given
        LeaderReview review = LeaderReview.builder()
                .studyId(5L)
                .reviewerId(8L)
                .leaderId(leaderId)
                .rating(new BigDecimal("4.7"))
                .comment("조회 테스트")
                .build();
        LeaderReview saved = leaderReviewRepository.save(review);

        // when
        LeaderReview found = leaderReviewRepository.findById(saved.getId()).orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getComment()).isEqualTo("조회 테스트");
    }
}
