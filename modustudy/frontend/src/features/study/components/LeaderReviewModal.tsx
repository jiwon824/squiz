import React from 'react';
import { Star, User } from 'lucide-react';
import { Modal } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import { LeaderReview } from '../mockData';

interface LeaderReviewModalProps {
    isOpen: boolean;
    onClose: () => void;
    leaderNickname: string;
    reviews: LeaderReview[];
    averageRating: number;
}

/**
 * 스터디장 리뷰 모달 컴포넌트
 * 평점 클릭 시 해당 스터디장의 리뷰 목록과 평균 평점을 표시
 */
const LeaderReviewModal: React.FC<LeaderReviewModalProps> = ({
    isOpen,
    onClose,
    leaderNickname,
    reviews,
    averageRating
}) => {
    // 별점 렌더링 (소수점 포함)
    const renderStars = (rating: number) => {
        const stars = [];
        for (let i = 1; i <= 5; i++) {
            if (i <= rating) {
                // 꽉 찬 별
                stars.push(
                    <Star key={i} size={16} className="text-yellow-400 fill-current" />
                );
            } else if (i - 0.5 <= rating) {
                // 반 별 (간단히 꽉 찬 별로 표시)
                stars.push(
                    <Star key={i} size={16} className="text-yellow-400 fill-current opacity-50" />
                );
            } else {
                // 빈 별
                stars.push(
                    <Star key={i} size={16} className="text-gray-300" />
                );
            }
        }
        return stars;
    };

    // 날짜 포맷팅
    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    // 평점 분포 계산
    const getRatingDistribution = () => {
        const distribution = [0, 0, 0, 0, 0]; // 1~5점
        reviews.forEach(review => {
            distribution[review.rating - 1]++;
        });
        return distribution.reverse(); // 5점부터 표시
    };

    const ratingDistribution = getRatingDistribution();

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={`${leaderNickname}님의 리뷰`} maxWidth="lg">
            <div className="flex flex-col gap-8">
                {/* 평점 요약 섹션 */}
                <div className="flex flex-col md:flex-row gap-8 p-6 bg-background-secondary/30 rounded-3xl border border-border-light/50">
                    {/* 평균 평점 */}
                    <div className="flex flex-col items-center justify-center gap-2 md:pr-8 md:border-r border-border-light/50">
                        <span className="text-5xl font-black text-text-primary">
                            {averageRating.toFixed(1)}
                        </span>
                        <div className="flex gap-0.5">
                            {renderStars(averageRating)}
                        </div>
                        <span className="text-sm text-text-tertiary font-medium">
                            총 {reviews.length}개의 리뷰
                        </span>
                    </div>

                    {/* 평점 분포 */}
                    <div className="flex-grow flex flex-col gap-2">
                        {[5, 4, 3, 2, 1].map((rating, index) => {
                            const count = ratingDistribution[index];
                            const percentage = reviews.length > 0 ? (count / reviews.length) * 100 : 0;
                            return (
                                <div key={rating} className="flex items-center gap-3">
                                    <span className="text-sm font-bold text-text-secondary w-6">{rating}점</span>
                                    <div className="flex-grow h-2 bg-gray-200 rounded-full overflow-hidden">
                                        <div
                                            className={cn(
                                                "h-full rounded-full transition-all duration-500",
                                                rating >= 4 ? "bg-success" : rating === 3 ? "bg-warning" : "bg-error"
                                            )}
                                            style={{ width: `${percentage}%` }}
                                        />
                                    </div>
                                    <span className="text-xs font-medium text-text-tertiary w-8">{count}개</span>
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* 리뷰 목록 */}
                <div className="flex flex-col gap-4 max-h-[400px] overflow-y-auto pr-2">
                    {reviews.length === 0 ? (
                        <div className="py-12 text-center text-text-tertiary">
                            <p className="text-lg font-medium">아직 리뷰가 없습니다.</p>
                            <p className="text-sm mt-1">스터디 완료 후 리뷰를 남겨주세요!</p>
                        </div>
                    ) : (
                        reviews.map((review) => (
                            <div
                                key={review.id}
                                className="p-5 bg-white border border-border-light rounded-2xl hover:shadow-sm transition-shadow"
                            >
                                {/* 리뷰어 정보 */}
                                <div className="flex items-start justify-between mb-3">
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center overflow-hidden">
                                            {review.reviewerProfileImage ? (
                                                <img
                                                    src={review.reviewerProfileImage}
                                                    alt={review.reviewerNickname}
                                                    className="w-full h-full object-cover"
                                                />
                                            ) : (
                                                <User size={20} className="text-primary" />
                                            )}
                                        </div>
                                        <div>
                                            <p className="text-sm font-bold text-text-primary">
                                                {review.reviewerNickname}
                                            </p>
                                            <p className="text-[11px] text-text-tertiary">
                                                {review.studyName}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="flex flex-col items-end gap-1">
                                        <div className="flex gap-0.5">
                                            {renderStars(review.rating)}
                                        </div>
                                        <span className="text-[10px] text-text-tertiary">
                                            {formatDate(review.createdAt)}
                                        </span>
                                    </div>
                                </div>

                                {/* 리뷰 내용 */}
                                <p className="text-sm text-text-secondary leading-relaxed">
                                    {review.comment}
                                </p>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </Modal>
    );
};

export default LeaderReviewModal;
