import React, { useState, useEffect } from 'react';
import { Star } from 'lucide-react';
import { Modal, Button } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import {
  LeaderReviewResponse,
  LeaderReviewCreateRequest,
  createLeaderReview,
  updateLeaderReview,
  deleteLeaderReview,
} from '@/api/endpoints/studyApi';
import { useUIStore } from '@/store/uiStore';

interface LeaderReviewWriteModalProps {
  isOpen: boolean;
  onClose: () => void;
  studyId: number;
  leaderNickname: string;
  existingReview?: LeaderReviewResponse | null;
  onSuccess: () => void;
}

/**
 * 스터디장 리뷰 작성/수정 모달 컴포넌트
 * 별점 선택 + 코멘트 입력
 */
const LeaderReviewWriteModal: React.FC<LeaderReviewWriteModalProps> = ({
  isOpen,
  onClose,
  studyId,
  leaderNickname,
  existingReview,
  onSuccess,
}) => {
  const { showToast } = useUIStore();
  const [rating, setRating] = useState<number>(0);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [comment, setComment] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const isEditMode = !!existingReview;

  // 기존 리뷰가 있으면 값 설정
  useEffect(() => {
    if (existingReview) {
      setRating(existingReview.rating);
      setComment(existingReview.comment || '');
    } else {
      setRating(0);
      setComment('');
    }
  }, [existingReview, isOpen]);

  // 별점 라벨
  const getRatingLabel = (value: number) => {
    switch (value) {
      case 1:
        return '별로예요';
      case 2:
        return '그저 그래요';
      case 3:
        return '괜찮아요';
      case 4:
        return '좋아요';
      case 5:
        return '최고예요!';
      default:
        return '별점을 선택해주세요';
    }
  };

  // 제출 핸들러
  const handleSubmit = async () => {
    if (rating === 0) {
      showToast('별점을 선택해주세요.', 'error');
      return;
    }

    setIsSubmitting(true);

    try {
      const request: LeaderReviewCreateRequest = {
        rating,
        comment: comment.trim() || undefined,
      };

      if (isEditMode && existingReview) {
        await updateLeaderReview(studyId, existingReview.reviewId, request);
        showToast('리뷰가 수정되었습니다.', 'success');
      } else {
        await createLeaderReview(studyId, request);
        showToast('리뷰가 등록되었습니다.', 'success');
      }

      onSuccess();
      onClose();
    } catch (error: any) {
      const message = error.response?.data?.message || '리뷰 등록에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 삭제 핸들러
  const handleDelete = async () => {
    if (!existingReview) return;

    setIsSubmitting(true);

    try {
      await deleteLeaderReview(studyId, existingReview.reviewId);
      showToast('리뷰가 삭제되었습니다.', 'success');
      onSuccess();
      onClose();
    } catch (error: any) {
      const message = error.response?.data?.message || '리뷰 삭제에 실패했습니다.';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
      setShowDeleteConfirm(false);
    }
  };

  // 별점 렌더링 (인터랙티브)
  const renderInteractiveStars = () => {
    const displayRating = hoverRating || rating;

    return (
      <div className="flex gap-2 justify-center">
        {[1, 2, 3, 4, 5].map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setRating(value)}
            onMouseEnter={() => setHoverRating(value)}
            onMouseLeave={() => setHoverRating(0)}
            className={cn(
              'p-1 rounded-lg transition-all duration-200',
              'hover:scale-110 focus:outline-none focus:ring-2 focus:ring-primary/30'
            )}
          >
            <Star
              size={36}
              className={cn(
                'transition-colors duration-200',
                value <= displayRating
                  ? 'text-yellow-400 fill-current'
                  : 'text-gray-300 hover:text-yellow-200'
              )}
            />
          </button>
        ))}
      </div>
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditMode ? '리뷰 수정' : `${leaderNickname}님 평가하기`}
      maxWidth="sm"
    >
      <div className="flex flex-col gap-6">
        {/* 별점 선택 */}
        <div className="flex flex-col items-center gap-3 py-4">
          {renderInteractiveStars()}
          <p
            className={cn(
              'text-base font-medium transition-colors',
              rating > 0 ? 'text-text-primary' : 'text-text-tertiary'
            )}
          >
            {getRatingLabel(hoverRating || rating)}
          </p>
        </div>

        {/* 코멘트 입력 */}
        <div className="flex flex-col gap-2">
          <label className="text-sm font-medium text-text-secondary">
            리뷰 작성 (선택)
          </label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="스터디장에 대한 솔직한 리뷰를 남겨주세요."
            maxLength={1000}
            rows={4}
            className={cn(
              'w-full px-4 py-3 rounded-xl border border-border-light',
              'bg-background-secondary/30 text-text-primary',
              'placeholder:text-text-tertiary text-sm leading-relaxed',
              'focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary',
              'resize-none transition-all'
            )}
          />
          <p className="text-xs text-text-tertiary text-right">
            {comment.length}/1000
          </p>
        </div>

        {/* 버튼 영역 */}
        <div className="flex gap-3">
          {isEditMode && (
            <Button
              variant="danger"
              onClick={() => setShowDeleteConfirm(true)}
              disabled={isSubmitting}
              className="flex-1"
            >
              삭제
            </Button>
          )}
          <Button
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
            className="flex-1"
          >
            취소
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            disabled={isSubmitting || rating === 0}
            className="flex-1"
          >
            {isSubmitting ? '처리중...' : isEditMode ? '수정하기' : '등록하기'}
          </Button>
        </div>

        {/* 삭제 확인 다이얼로그 */}
        {showDeleteConfirm && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="bg-white rounded-2xl p-6 max-w-sm mx-4 shadow-xl">
              <h3 className="text-lg font-bold text-text-primary mb-2">
                리뷰를 삭제하시겠습니까?
              </h3>
              <p className="text-sm text-text-secondary mb-6">
                삭제된 리뷰는 복구할 수 없습니다.
              </p>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={isSubmitting}
                  className="flex-1"
                >
                  취소
                </Button>
                <Button
                  variant="danger"
                  onClick={handleDelete}
                  disabled={isSubmitting}
                  className="flex-1"
                >
                  {isSubmitting ? '삭제중...' : '삭제'}
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default LeaderReviewWriteModal;
