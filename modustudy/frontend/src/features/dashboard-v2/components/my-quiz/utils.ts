import { ReviewItemDto } from '../../api/reviewApi';
import { WeakConcept, CategoryStats } from './types';

/**
 * 오답 리뷰 데이터에서 취약 개념 목록을 계산합니다.
 * 카테고리별로 그룹화하고 오답률을 계산합니다.
 */
export const calculateWeakConcepts = (wrongReviews: ReviewItemDto[]): WeakConcept[] => {
  if (!wrongReviews || wrongReviews.length === 0) return [];

  // 카테고리별로 그룹화
  const grouped = wrongReviews.reduce((acc, item) => {
    const key = item.question.category || 'General';

    if (!acc[key]) {
      acc[key] = {
        id: key,
        concept: key,
        category: key,
        wrongCount: 0,
        totalReps: 0,
        wrongRate: 0,
        relatedQuestions: [],
        lastReviewDate: item.question.lastReviewAt || '',
      };
    }

    acc[key].wrongCount += item.lapses || 0;
    // reps가 lapses보다 작은 경우를 방지
    const safeReps = Math.max(item.reps || 0, item.lapses || 0);
    acc[key].totalReps += safeReps;

    acc[key].relatedQuestions.push(item.question.questionNumber);

    // 가장 최근 리뷰 날짜 업데이트
    if (
      item.question.lastReviewAt &&
      (!acc[key].lastReviewDate ||
        new Date(item.question.lastReviewAt) > new Date(acc[key].lastReviewDate))
    ) {
      acc[key].lastReviewDate = item.question.lastReviewAt;
    }

    return acc;
  }, {} as Record<string, WeakConcept>);

  // 오답률 계산 및 정렬
  return Object.values(grouped)
    .map((concept) => ({
      ...concept,
      wrongRate:
        concept.totalReps > 0
          ? Math.min((concept.wrongCount / concept.totalReps) * 100, 100)
          : 0,
    }))
    .sort((a, b) => b.wrongRate - a.wrongRate);
};

/**
 * 카테고리별 오답 통계를 계산합니다.
 */
export const calculateCategoryStats = (
  wrongReviews: ReviewItemDto[]
): Record<string, CategoryStats> => {
  return wrongReviews.reduce((acc, wrong) => {
    const category = wrong.question.category || '기타';
    if (!acc[category]) {
      acc[category] = { questionCount: 0, totalWrongCount: 0 };
    }
    acc[category].questionCount += 1;
    acc[category].totalWrongCount += wrong.lapses;
    return acc;
  }, {} as Record<string, CategoryStats>);
};

/**
 * 총 오답 횟수를 계산합니다.
 */
export const calculateTotalWrongCount = (wrongReviews: ReviewItemDto[]): number => {
  return wrongReviews.reduce((sum, w) => sum + w.lapses, 0);
};

/**
 * 평균 오답 횟수를 계산합니다.
 */
export const calculateAvgWrongCount = (wrongReviews: ReviewItemDto[]): string => {
  const totalWrongCount = calculateTotalWrongCount(wrongReviews);
  return wrongReviews.length ? (totalWrongCount / wrongReviews.length).toFixed(1) : '0';
};

/**
 * 난이도에 따른 색상 클래스를 반환합니다.
 */
export const getDifficultyColor = (difficulty: number): string => {
  if (difficulty < 3) return 'bg-accent/20 text-accent-dark';
  if (difficulty < 7) return 'bg-secondary/20 text-secondary-dark';
  return 'bg-error/20 text-error';
};

/**
 * 난이도에 따른 라벨을 반환합니다.
 */
export const getDifficultyLabel = (difficulty: number): string => {
  if (difficulty < 3) return '쉬움';
  if (difficulty < 7) return '보통';
  return '어려움';
};

/**
 * 오답률에 따른 취약도 색상 클래스를 반환합니다.
 */
export const getWeaknessBarColor = (wrongRate: number): string => {
  if (wrongRate >= 50) return 'bg-red-500';
  if (wrongRate >= 30) return 'bg-amber-500';
  return 'bg-blue-500';
};

/**
 * 순위에 따른 뱃지 색상 클래스를 반환합니다.
 */
export const getRankBadgeColor = (index: number): string => {
  if (index === 0) return 'bg-red-100 text-red-600';
  if (index === 1) return 'bg-amber-100 text-amber-600';
  if (index === 2) return 'bg-blue-100 text-blue-600';
  return 'bg-gray-100 text-gray-500';
};

/**
 * 백분율에 따른 진행 바 색상 클래스를 반환합니다.
 */
export const getProgressBarColor = (percentage: number): string => {
  if (percentage >= 70) return 'bg-error';
  if (percentage >= 40) return 'bg-warning';
  return 'bg-primary';
};
