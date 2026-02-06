package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.response.LeaderInfoResponse;
import com.ssafy.domain.study.dto.response.LeaderReviewResponse;
import com.ssafy.domain.study.entity.LeaderReview;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.LeaderReviewRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * LeaderService 단위 테스트
 */
 @ExtendWith(MockitoExtension.class)
 class LeaderServiceTest {

    @Mock
    private StudyRepository studyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaderReviewRepository leaderReviewRepository;

    @InjectMocks
    private LeaderService leaderService;

    // ============================================================
    // 스터디장 정보 조회
    // ============================================================

    @Test
    @DisplayName("스터디장 정보 조회 성공")
    void getLeaderInfo_Success() {
        // given
        Long studyId = 1L;
        Long leaderId = 100L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .name("알고리즘 스터디")
                .build();

        User leader = User.builder()
                .name("김싸피")
                .nickname("ssafy_kim")
                .email("kim@ssafy.com")
                .leaderRating(4.5f)
                .leaderReviewCount(10)
                .currentLevel(5)
                .levelName("Gold")
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(leaderId)).willReturn(Optional.of(leader));

        // when
        LeaderInfoResponse response = leaderService.getLeaderInfo(studyId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("김싸피");
        assertThat(response.getNickname()).isEqualTo("ssafy_kim");
        assertThat(response.getEmail()).isEqualTo("kim@ssafy.com");
        assertThat(response.getLeaderRating()).isEqualTo(4.5f);
        assertThat(response.getLeaderReviewCount()).isEqualTo(10);
        assertThat(response.getCurrentLevel()).isEqualTo(5);
        assertThat(response.getLevelName()).isEqualTo("Gold");

        verify(studyRepository).findById(studyId);
        verify(userRepository).findById(leaderId);
    }

    @Test
    @DisplayName("존재하지 않는 스터디")
    void getLeaderInfo_StudyNotFound() {
        // given
        Long studyId = 999L;
        given(studyRepository.findById(studyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> leaderService.getLeaderInfo(studyId))
                .isInstanceOf(StudyException.StudyNotFoundException.class);

        verify(studyRepository).findById(studyId);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 스터디장")
    void getLeaderInfo_LeaderNotFound() {
        // given
        Long studyId = 1L;
        Long leaderId = 999L;

        Study study = Study.builder()
                .leaderId(leaderId)
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(userRepository.findById(leaderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> leaderService.getLeaderInfo(studyId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(studyRepository).findById(studyId);
        verify(userRepository).findById(leaderId);
    }

    // ============================================================
    // 리뷰 목록 조회
    // ============================================================

    @Test
    @DisplayName("리뷰 목록 조회 성공")
    void getLeaderReviews_Success() {
        // given
        Long studyId = 1L;
        Long leaderId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        Study study = Study.builder()
                .leaderId(leaderId)
                .name("알고리즘 스터디")
                .build();

        LeaderReview review1 = LeaderReview.builder()
                .studyId(studyId)
                .reviewerId(2L)
                .leaderId(leaderId)
                .rating(new BigDecimal("5.0"))
                .comment("좋은 스터디였습니다!")
                .build();

        LeaderReview review2 = LeaderReview.builder()
                .studyId(studyId)
                .reviewerId(3L)
                .leaderId(leaderId)
                .rating(new BigDecimal("4.5"))
                .comment("체계적이었습니다.")
                .build();

        Page<LeaderReview> reviewPage =
                new PageImpl<>(List.of(review1, review2));

        User reviewer1 = User.builder()
                .name("이싸피")
                .nickname("ssafy_lee")
                .build();

        User reviewer2 = User.builder()
                .name("박싸피")
                .nickname("ssafy_park")
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(leaderReviewRepository.findByLeaderId(leaderId, pageable))
                .willReturn(reviewPage);
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewer1));
        given(userRepository.findById(3L)).willReturn(Optional.of(reviewer2));

        // when
        Page<LeaderReviewResponse> response =
                leaderService.getLeaderReviews(studyId, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);

        LeaderReviewResponse first = response.getContent().get(0);
        assertThat(first.getReviewerId()).isEqualTo(2L);
        assertThat(first.getReviewerName()).isEqualTo("이싸피");
        assertThat(first.getReviewerNickname()).isEqualTo("ssafy_lee");
        assertThat(first.getStudyName()).isEqualTo("알고리즘 스터디");

        verify(leaderReviewRepository)
                .findByLeaderId(leaderId, pageable);
        verify(studyRepository, atLeastOnce())
                .findById(studyId);
    }

    @Test
    @DisplayName("리뷰 없음")
    void getLeaderReviews_Empty() {
        // given
        Long studyId = 1L;
        Long leaderId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        Study study = Study.builder()
                .leaderId(leaderId)
                .build();

        given(studyRepository.findById(studyId)).willReturn(Optional.of(study));
        given(leaderReviewRepository.findByLeaderId(leaderId, pageable))
                .willReturn(Page.empty());

        // when
        Page<LeaderReviewResponse> response =
                leaderService.getLeaderReviews(studyId, pageable);

        // then
        assertThat(response.getContent()).isEmpty();
        verify(leaderReviewRepository)
                .findByLeaderId(leaderId, pageable);
    }

    // ============================================================
    // 평균 평점
    // ============================================================

    @Test
    @DisplayName("평균 평점 조회 성공")
    void getLeaderAverageRating() {
        // given
        Long leaderId = 100L;
        given(leaderReviewRepository.calculateAverageRating(leaderId))
                .willReturn(4.5);

        // when
        Double rating = leaderService.getLeaderAverageRating(leaderId);

        // then
        assertThat(rating).isEqualTo(4.5);
        verify(leaderReviewRepository)
                .calculateAverageRating(leaderId);
    }

    @Test
    @DisplayName("평균 평점 - 리뷰 없음")
    void getLeaderAverageRating_Empty() {
        // given
        Long leaderId = 100L;
        given(leaderReviewRepository.calculateAverageRating(leaderId))
                .willReturn(null);

        // when
        Double rating = leaderService.getLeaderAverageRating(leaderId);

        // then
        assertThat(rating).isEqualTo(0.0);
    }

    // ============================================================
    // 리뷰 개수
    // ============================================================

    @Test
    @DisplayName("리뷰 개수 조회")
    void getLeaderReviewCount() {
        // given
        Long leaderId = 100L;
        given(leaderReviewRepository.countByLeaderId(leaderId))
                .willReturn(10L);

        // when
        Long count = leaderService.getLeaderReviewCount(leaderId);

        // then
        assertThat(count).isEqualTo(10L);
        verify(leaderReviewRepository)
                .countByLeaderId(leaderId);
    }
}
