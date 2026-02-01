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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 스터디장 정보 조회
     *
     * @param studyId 스터디 ID
     * @return 스터디장 정보
     */
    public LeaderInfoResponse getLeaderInfo(Long studyId) {
        log.info("스터디장 정보 조회 시작 - studyId: {}", studyId);

        // 1. 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 - studyId: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 스터디장 조회
        User leader = userRepository.findById(study.getLeaderId())
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자 - userId: {}", study.getLeaderId());
                    return new IllegalArgumentException("존재하지 않는 사용자입니다: " + study.getLeaderId());
                });

        log.info("스터디장 정보 조회 완료 - leaderId: {}, name: {}, rating: {}, reviewCount: {}",
                leader.getId(), leader.getName(), leader.getLeaderRating(), leader.getLeaderReviewCount());

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
        log.info("스터디장 리뷰 목록 조회 시작 - studyId: {}, page: {}, size: {}",
                studyId, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 - studyId: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 스터디장의 리뷰 조회
        Page<LeaderReview> reviews = leaderReviewRepository.findByLeaderId(study.getLeaderId(), pageable);

        log.info("스터디장 리뷰 조회 완료 - leaderId: {}, totalElements: {}, totalPages: {}",
                study.getLeaderId(), reviews.getTotalElements(), reviews.getTotalPages());

        // 3. DTO 변환 및 추가 정보 설정
        return reviews.map(review -> {
            LeaderReviewResponse response = LeaderReviewResponse.from(review);

            // 평가자 정보 설정
            userRepository.findById(review.getReviewerId()).ifPresent(reviewer -> {
                response.setReviewerInfo(reviewer.getName(), reviewer.getNickname());
                log.debug("평가자 정보 설정 - reviewerId: {}, name: {}", reviewer.getId(), reviewer.getName());
            });

            // 스터디 이름 설정
            studyRepository.findById(review.getStudyId()).ifPresent(reviewStudy -> {
                response.setStudyName(reviewStudy.getName());
                log.debug("스터디 이름 설정 - studyId: {}, name: {}", reviewStudy.getId(), reviewStudy.getName());
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
        log.info("스터디장 평균 평점 조회 - leaderId: {}", leaderId);

        Double avgRating = leaderReviewRepository.calculateAverageRating(leaderId);

        log.info("평균 평점 조회 완료 - leaderId: {}, avgRating: {}", leaderId, avgRating);

        return avgRating != null ? avgRating : 0.0;
    }

    /**
     * 특정 스터디장의 리뷰 개수 조회 (Optional)
     *
     * @param leaderId 스터디장 ID
     * @return 리뷰 개수
     */
    public Long getLeaderReviewCount(Long leaderId) {
        log.info("스터디장 리뷰 개수 조회 - leaderId: {}", leaderId);

        Long count = leaderReviewRepository.countByLeaderId(leaderId);

        log.info("리뷰 개수 조회 완료 - leaderId: {}, count: {}", leaderId, count);

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
        log.info("스터디장 리뷰 작성 시작 - studyId: {}, reviewerId: {}", studyId, reviewerId);

        // 1. 스터디 조회
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 스터디 - studyId: {}", studyId);
                    return new StudyException.StudyNotFoundException(studyId);
                });

        // 2. 스터디 완료 여부 확인
        if (study.getStatus() != Status.COMPLETED) {
            log.error("스터디가 완료되지 않음 - studyId: {}, status: {}", studyId, study.getStatus());
            throw new IllegalStateException("스터디가 완료된 후에만 평가할 수 있습니다.");
        }

        // 3. 스터디 멤버 여부 확인
        boolean isMember = studyMemberRepository.existsByStudyIdAndUserIdAndStatus(studyId, reviewerId, MemberStatus.APPROVED);
        if (!isMember) {
            log.error("스터디 멤버가 아님 - studyId: {}, userId: {}", studyId, reviewerId);
            throw new IllegalStateException("스터디 멤버만 평가할 수 있습니다.");
        }

        // 4. 스터디장 본인 평가 불가
        if (study.getLeaderId().equals(reviewerId)) {
            log.error("스터디장 본인 평가 시도 - studyId: {}, leaderId: {}", studyId, reviewerId);
            throw new IllegalStateException("스터디장은 본인을 평가할 수 없습니다.");
        }

        // 5. 중복 평가 확인
        if (leaderReviewRepository.existsByStudyIdAndReviewerId(studyId, reviewerId)) {
            log.error("이미 평가 완료 - studyId: {}, reviewerId: {}", studyId, reviewerId);
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
        log.info("리뷰 저장 완료 - reviewId: {}", savedReview.getId());

        // 7. 스터디장 평균 평점 및 리뷰 수 업데이트
        updateLeaderRatingStats(study.getLeaderId());

        // 8. 응답 생성
        LeaderReviewResponse response = LeaderReviewResponse.from(savedReview);
        userRepository.findById(reviewerId).ifPresent(reviewer ->
            response.setReviewerInfo(reviewer.getName(), reviewer.getNickname())
        );
        response.setStudyName(study.getName());

        log.info("스터디장 리뷰 작성 완료 - reviewId: {}", savedReview.getId());
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
        log.info("내 리뷰 조회 - studyId: {}, reviewerId: {}", studyId, reviewerId);

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
        log.info("스터디장 리뷰 수정 시작 - studyId: {}, reviewId: {}, reviewerId: {}", studyId, reviewId, reviewerId);

        // 1. 리뷰 조회
        LeaderReview review = leaderReviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 리뷰 - reviewId: {}", reviewId);
                    return new IllegalArgumentException("존재하지 않는 리뷰입니다.");
                });

        // 2. 스터디 일치 확인
        if (!review.getStudyId().equals(studyId)) {
            log.error("스터디 불일치 - reviewStudyId: {}, requestStudyId: {}", review.getStudyId(), studyId);
            throw new IllegalArgumentException("해당 스터디의 리뷰가 아닙니다.");
        }

        // 3. 작성자 확인
        if (!review.getReviewerId().equals(reviewerId)) {
            log.error("작성자 불일치 - reviewerId: {}, requesterId: {}", review.getReviewerId(), reviewerId);
            throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        // 4. 리뷰 수정
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        LeaderReview updatedReview = leaderReviewRepository.save(review);
        log.info("리뷰 수정 완료 - reviewId: {}", updatedReview.getId());

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

        log.info("스터디장 리뷰 수정 완료 - reviewId: {}", updatedReview.getId());
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
        log.info("스터디장 리뷰 삭제 시작 - studyId: {}, reviewId: {}, reviewerId: {}", studyId, reviewId, reviewerId);

        // 1. 리뷰 조회
        LeaderReview review = leaderReviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 리뷰 - reviewId: {}", reviewId);
                    return new IllegalArgumentException("존재하지 않는 리뷰입니다.");
                });

        // 2. 스터디 일치 확인
        if (!review.getStudyId().equals(studyId)) {
            log.error("스터디 불일치 - reviewStudyId: {}, requestStudyId: {}", review.getStudyId(), studyId);
            throw new IllegalArgumentException("해당 스터디의 리뷰가 아닙니다.");
        }

        // 3. 작성자 확인
        if (!review.getReviewerId().equals(reviewerId)) {
            log.error("작성자 불일치 - reviewerId: {}, requesterId: {}", review.getReviewerId(), reviewerId);
            throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        Long leaderId = review.getLeaderId();

        // 4. 리뷰 삭제
        leaderReviewRepository.delete(review);
        log.info("리뷰 삭제 완료 - reviewId: {}", reviewId);

        // 5. 스터디장 평균 평점 업데이트
        updateLeaderRatingStats(leaderId);

        log.info("스터디장 리뷰 삭제 완료 - reviewId: {}", reviewId);
    }

    /**
     * 스터디장 평균 평점 및 리뷰 수 업데이트
     *
     * @param leaderId 스터디장 ID
     */
    private void updateLeaderRatingStats(Long leaderId) {
        log.info("스터디장 평점 통계 업데이트 - leaderId: {}", leaderId);

        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + leaderId));

        Double avgRating = leaderReviewRepository.calculateAverageRating(leaderId);
        Long reviewCount = leaderReviewRepository.countByLeaderId(leaderId);

        leader.setLeaderRating(avgRating != null ? avgRating.floatValue() : 0.0f);
        leader.setLeaderReviewCount(reviewCount.intValue());

        userRepository.save(leader);

        log.info("스터디장 평점 통계 업데이트 완료 - leaderId: {}, avgRating: {}, reviewCount: {}",
                leaderId, avgRating, reviewCount);
    }
}