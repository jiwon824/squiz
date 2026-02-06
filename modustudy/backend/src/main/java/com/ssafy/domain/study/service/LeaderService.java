package com.ssafy.domain.study.service;

import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.LeaderReviewCreateRequest;
import com.ssafy.domain.study.dto.request.LeaderReviewUpdateRequest;
import com.ssafy.domain.study.dto.response.LeaderInfoResponse;
import com.ssafy.domain.study.dto.response.LeaderReviewResponse;
import com.ssafy.domain.study.entity.LeaderReview;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.LeaderReviewRepository;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.gamification.event.FirstLeaderReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 스터디장 정보/리뷰 Service
 */
 @Service
 @RequiredArgsConstructor
 @Slf4j
 @Transactional(readOnly = true)
 public class LeaderService {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final LeaderReviewRepository leaderReviewRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 스터디장 정보 조회
     *
     * @param studyId 스터디 ID
     * @return 스터디장 정보
     */
    public LeaderInfoResponse getLeaderInfo(Long studyId) {
// 1. 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 스터디장 조회
        User leader = userRepository.findById(study.getLeaderId())
                .orElseThrow(() -> {
                    return new IllegalArgumentException("존재하지 않는 사용자입니다: " + study.getLeaderId());
                });

// 3. DTO 변환 및 반환
        return LeaderInfoResponse.from(leader);
    }

    /**
     * 스터디장 리뷰 목록 조회
     *
     * @param studyId 스터디 ID
     * @param pageable 페이징 정보
     * @return 리뷰 목록 (페이징)
     */
    public Page<LeaderReviewResponse> getLeaderReviews(Long studyId, Pageable pageable) {
// 1. 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 스터디장의 리뷰 조회
        Page<LeaderReview> reviews = leaderReviewRepository.findByLeaderId(study.getLeaderId(), pageable);

// 3. DTO 변환 및 추가 정보 설정
        return reviews.map(review -> {
            LeaderReviewResponse response = LeaderReviewResponse.from(review);

            // 평가자 정보 설정
            userRepository.findById(review.getReviewerId()).ifPresent(reviewer -> {
                response.setReviewerInfo(reviewer.getName(), reviewer.getNickname());
});

            // 스터디 이름 설정
            studyRepository.findById(review.getStudyId()).ifPresent(reviewStudy -> {
                response.setStudyName(reviewStudy.getName());
});

            return response;
        });
    }

    /**
     * 특정 스터디장의 평균 평점 조회 (Optional)
     *
     * @param leaderId 스터디장 ID
     * @return 평균 평점
     */
    public Double getLeaderAverageRating(Long leaderId) {
        Double avgRating = leaderReviewRepository.calculateAverageRating(leaderId);

        return avgRating != null ? avgRating : 0.0;
    }

    /**
     * 특정 스터디장의 리뷰 개수 조회 (Optional)
     *
     * @param leaderId 스터디장 ID
     * @return 리뷰 개수
     */
    public Long getLeaderReviewCount(Long leaderId) {
        Long count = leaderReviewRepository.countByLeaderId(leaderId);

        return count;
    }

    /**
     * 스터디장 리뷰 작성
     *
     * @param studyId 스터디 ID
     * @param reviewerId 평가자 ID
     * @param request 리뷰 작성 요청
     * @return 작성된 리뷰 정보
     */
    @Transactional
    public LeaderReviewResponse createReview(Long studyId, Long reviewerId, LeaderReviewCreateRequest request) {
// 1. 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 스터디 완료 여부 확인
        if (study.getStatus() != Status.COMPLETED) {
            throw new IllegalStateException("스터디가 완료된 후에만 평가할 수 있습니다.");
        }

        // 3. 스터디 멤버 여부 확인
        boolean isMember = studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, reviewerId, MemberStatus.APPROVED);
        if (!isMember) {
            throw new IllegalStateException("스터디 멤버만 평가할 수 있습니다.");
        }

        // 4. 스터디장 본인 평가 불가
        if (study.getLeaderId().equals(reviewerId)) {
            throw new IllegalStateException("스터디장은 본인을 평가할 수 없습니다.");
        }

        // 5. 중복 평가 확인
        if (leaderReviewRepository.existsByStudyIdAndReviewerId(studyId, reviewerId)) {
            throw new IllegalStateException("이미 해당 스터디에서 평가를 완료했습니다.");
        }

        // 6. 리뷰 생성
        LeaderReview review = LeaderReview.builder()
                .studyId(studyId)
                .reviewerId(reviewerId)
                .leaderId(study.getLeaderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        LeaderReview savedReview = leaderReviewRepository.save(review);
// 7. 스터디장 평균 평점 및 리뷰 수 업데이트
        updateLeaderRatingStats(study.getLeaderId());

        // 8. 첫 리뷰인 경우 경험치 이벤트 발행
        Long reviewCount = leaderReviewRepository.countByReviewerId(reviewerId);
        if (reviewCount == 1) {  // 방금 저장한 것이 첫 번째 리뷰
        eventPublisher.publishEvent(new FirstLeaderReviewEvent(
                    reviewerId,
                    studyId,
                    study.getName(),
                    study.getLeaderId(),
                    LocalDate.now()
            ));
        }

        // 9. 응답 생성
        LeaderReviewResponse response = LeaderReviewResponse.from(savedReview);
        userRepository.findById(reviewerId).ifPresent(reviewer ->
            response.setReviewerInfo(reviewer.getName(), reviewer.getNickname())
        );
        response.setStudyName(study.getName());

        return response;
    }

    /**
     * 내 리뷰 조회 (특정 스터디에서 내가 작성한 리뷰)
     *
     * @param studyId 스터디 ID
     * @param reviewerId 평가자 ID
     * @return 내 리뷰 정보 (없으면 null)
     */
    public LeaderReviewResponse getMyReview(Long studyId, Long reviewerId) {
        return leaderReviewRepository.findByStudyIdAndReviewerId(studyId, reviewerId)
                .map(review -> {
                    LeaderReviewResponse response = LeaderReviewResponse.from(review);
                    userRepository.findById(reviewerId).ifPresent(reviewer ->
                        response.setReviewerInfo(reviewer.getName(), reviewer.getNickname())
                    );
                    studyRepository.findById(studyId).ifPresent(study ->
                        response.setStudyName(study.getName())
                    );
                    return response;
                })
                .orElse(null);
    }

    /**
     * 스터디장 리뷰 수정
     *
     * @param studyId 스터디 ID
     * @param reviewId 리뷰 ID
     * @param reviewerId 평가자 ID
     * @param request 리뷰 수정 요청
     * @return 수정된 리뷰 정보
     */
    @Transactional
    public LeaderReviewResponse updateReview(Long studyId, Long reviewId, Long reviewerId, LeaderReviewUpdateRequest request) {
// 1. 리뷰 조회
        LeaderReview review = leaderReviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("존재하지 않는 리뷰입니다.");
                });

        // 2. 스터디 일치 확인
        if (!review.getStudyId().equals(studyId)) {
            throw new IllegalArgumentException("해당 스터디의 리뷰가 아닙니다.");
        }

        // 3. 작성자 확인
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        // 4. 리뷰 수정
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        LeaderReview updatedReview = leaderReviewRepository.save(review);
// 5. 스터디장 평균 평점 업데이트
        updateLeaderRatingStats(review.getLeaderId());

        // 6. 응답 생성
        LeaderReviewResponse response = LeaderReviewResponse.from(updatedReview);
        userRepository.findById(reviewerId).ifPresent(reviewer ->
            response.setReviewerInfo(reviewer.getName(), reviewer.getNickname())
        );
        studyRepository.findById(studyId).ifPresent(study ->
            response.setStudyName(study.getName())
        );

        return response;
    }

    /**
     * 스터디장 리뷰 삭제
     *
     * @param studyId 스터디 ID
     * @param reviewId 리뷰 ID
     * @param reviewerId 평가자 ID
     */
    @Transactional
    public void deleteReview(Long studyId, Long reviewId, Long reviewerId) {
// 1. 리뷰 조회
        LeaderReview review = leaderReviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    return new IllegalArgumentException("존재하지 않는 리뷰입니다.");
                });

        // 2. 스터디 일치 확인
        if (!review.getStudyId().equals(studyId)) {
            throw new IllegalArgumentException("해당 스터디의 리뷰가 아닙니다.");
        }

        // 3. 작성자 확인
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        Long leaderId = review.getLeaderId();

        // 4. 리뷰 삭제
        leaderReviewRepository.delete(review);
// 5. 스터디장 평균 평점 업데이트
        updateLeaderRatingStats(leaderId);

}

    /**
     * 스터디장 평균 평점 및 리뷰 수 업데이트
     *
     * @param leaderId 스터디장 ID
     */
    private void updateLeaderRatingStats(Long leaderId) {
        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + leaderId));

        Double avgRating = leaderReviewRepository.calculateAverageRating(leaderId);
        Long reviewCount = leaderReviewRepository.countByLeaderId(leaderId);

        leader.setLeaderRating(avgRating != null ? avgRating.floatValue() : 0.0f);
        leader.setLeaderReviewCount(reviewCount.intValue());

        userRepository.save(leader);

}
}
