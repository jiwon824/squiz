import { ReviewItemDto, ReviewCourseWeaknessResponse } from '../../api/reviewApi';
import { WeakConcept, CategoryStats } from './types';

/**
 * 오답 목록을 분석하여 취약 개념(코스/카테고리)을 추출하고 통계를 계산합니다.
 * 
 * 변경 전: 오답 목록에 있는 문제들의 reps/lapses 만으로 계산하여 오답률이 부정확함.
 * 변경 후: 백엔드에서 집계된 '전체 코스 통계(courseWeaknessStats)'를 사용하여 
 *         (전체 오답 횟수 / 전체 복습 횟수)로 정확한 오답률을 계산합니다.
 */
export const calculateWeakConcepts = (
  wrongReviews: ReviewItemDto[],
  courseStats: ReviewCourseWeaknessResponse | null
): WeakConcept[] => {
  if (!wrongReviews || wrongReviews.length === 0) return [];

  // 1. 오답 목록을 카테고리별로 그룹화 (관련 문제 번호, 최근 복습일 수집용)
  const categoryMap = new Map<string, ReviewItemDto[]>();

  wrongReviews.forEach((item) => {
    // 코스 이름(category)을 키로 사용
    const category = item.question?.category || '기타';
    if (!categoryMap.has(category)) {
      categoryMap.set(category, []);
    }
    categoryMap.get(category)?.push(item);
  });

  // 2. 전체 코스 통계를 맵으로 변환 (빠른 조회를 위해)
  const statsMap = new Map<string, { totalReps: number; totalLapses: number; courseId: number }>();
  if (courseStats?.courseWeaknessStats) {
    courseStats.courseWeaknessStats.forEach(stat => {
      statsMap.set(stat.courseName, {
        totalReps: stat.totalReps,
        totalLapses: stat.totalLapses,
        courseId: stat.courseId
      });
    });
  }

  // 3. 취약 개념 데이터 생성
  const weakConcepts: WeakConcept[] = Array.from(categoryMap.entries()).map(([category, items]) => {
    // 3-1. 해당 카테고리의 전체 통계 가져오기
    const stats = statsMap.get(category);

    // 3-2. 오답률 계산
    let totalReps = 0;
    let wrongRate = 0;
    let courseId: number | undefined;

    if (stats && stats.totalReps > 0) {
      // 통계 데이터가 있으면 사용 (정확한 계산)
      totalReps = stats.totalReps;
      // totalLapses는 사용하지 않지만 stats에 있음. wrongRate는 lapses/reps
      wrongRate = (stats.totalLapses / stats.totalReps) * 100;
      courseId = stats.courseId;
    } else {
      // 통계 데이터가 없으면 기존 방식대로 오답 목록 내에서 추산 (fallback)
      const localReps = items.reduce((sum, item) => sum + item.reps, 0);
      const localLapses = items.reduce((sum, item) => sum + item.lapses, 0);
      totalReps = localReps;
      wrongRate = localReps > 0 ? (localLapses / localReps) * 100 : 0;
    }

    // 3-3. 기타 메타데이터 수집
    const wrongCount = items.length;

    // 관련 문제 번호 추출 (중복 제거 및 정렬)
    const relatedQuestions = Array.from(
      new Set(items.map((item) => item.question?.questionNumber || 0))
    )
      .filter((num) => num > 0)
      .sort((a, b) => a - b);

    // 가장 최근 복습 날짜 찾기
    const lastReviewDate = items.reduce((latest, item) => {
      const current = item.question?.lastReviewAt || '';
      return current > latest ? current : latest;
    }, '');

    return {
      id: category,
      concept: category,
      category,
      wrongCount,  // 현재 오답 목록에 있는 문제 개수
      totalReps,   // 전체 복습 횟수 (통계 기준)
      wrongRate,   // 전체 오답률 (통계 기준)
      relatedQuestions,
      lastReviewDate,
      courseId,
    };
  });

  // 4. 오답률 높은 순으로 정렬
  return weakConcepts.sort((a, b) => b.wrongRate - a.wrongRate);
};

/**
 * 카테고리별 오답 통계를 계산합니다.
 */
export const calculateCategoryStats = (
  wrongReviews: ReviewItemDto[]
): Record<string, CategoryStats> => {
  return wrongReviews.reduce((acc, wrong) => {
    const category = wrong.question?.category || '기타';
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
