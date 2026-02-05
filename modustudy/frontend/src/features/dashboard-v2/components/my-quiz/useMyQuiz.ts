import { useState, useEffect, useCallback, useMemo } from 'react';
import {
  getTodayReviews,
  getWrongAnswers,
  submitReview,
  ReviewItemDto,
  WrongAnswerSortType,
  getCourseWeaknessStats,
  ReviewCourseWeaknessResponse,
  getReviewStats,
  ReviewStatsResponse,
} from '../../api/reviewApi';
import { useTimer } from '@/features/quiz/hooks/useTimer';
import { indexToOptionId } from '@/shared/utils/quizUtils';
import { getCourseQuizStats, CourseQuizStat } from '@/api/endpoints/continuousQuizApi';
import { TabType, QuizRetryState, UseMyQuizReturn } from './types';
import { calculateWeakConcepts, calculateTotalWrongCount, calculateAvgWrongCount } from './utils';

// 퀴즈 재도전 상태 초기값
const INITIAL_RETRY_STATE: QuizRetryState = {
  selectedReviewItem: null,
  isRetrying: false,
  selectedAnswer: null,
  selectedAnswers: [],
  shortAnswer: '',
  showResult: false,
  isCorrectAnswer: null,
};

/**
 * MyQuizPage의 상태 관리와 비즈니스 로직을 담당하는 커스텀 훅
 *
 * 주요 책임:
 * - 오늘의 복습/틀린 문제 데이터 패칭
 * - 탭 상태 관리
 * - 정렬 옵션 관리
 * - 퀴즈 재도전 상태 및 제출 로직
 * - 통계 데이터 계산 (useMemo 활용)
 */
export const useMyQuiz = (): UseMyQuizReturn => {
  // === 데이터 상태 ===
  const [todayReviews, setTodayReviews] = useState<ReviewItemDto[]>([]);
  const [wrongReviews, setWrongReviews] = useState<ReviewItemDto[]>([]);
  const [courseStats, setCourseStats] = useState<ReviewCourseWeaknessResponse | null>(null);
  const [courseQuizStats, setCourseQuizStats] = useState<CourseQuizStat[]>([]);
  const [reviewStats, setReviewStats] = useState<ReviewStatsResponse | null>(null);
  const [loading, setLoading] = useState(false);

  // === UI 상태 ===
  const [activeTab, setActiveTab] = useState<TabType>('review');
  const [wrongSortType, setWrongSortType] = useState<WrongAnswerSortType>('LATEST');

  // === 퀴즈 재도전 상태 ===
  const [retryState, setRetryState] = useState<QuizRetryState>(INITIAL_RETRY_STATE);

  // === 타이머 훅 ===
  const timer = useTimer();

  // === 데이터 패칭 ===
  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [todayData, wrongData, statsData, quizStatsData, reviewStatsData] = await Promise.all([
        getTodayReviews(),
        getWrongAnswers(wrongSortType),
        getCourseWeaknessStats(),
        getCourseQuizStats(),
        getReviewStats(),
      ]);
      setTodayReviews(todayData?.items || []);
      setWrongReviews(wrongData?.items || []);
      setCourseStats(statsData || null);
      setCourseQuizStats(quizStatsData || []);
      setReviewStats(reviewStatsData || null);
    } catch (error) {
      console.error('Failed to fetch reviews', error);
      setTodayReviews([]);
      setWrongReviews([]);
    } finally {
      setLoading(false);
    }
  }, [wrongSortType]);

  // 정렬 타입 변경 시 데이터 재패칭
  useEffect(() => {
    fetchData();
  }, [wrongSortType]);

  // === Memoized 계산 ===
  const weakConcepts = useMemo(
    () => calculateWeakConcepts(wrongReviews, courseStats),
    [wrongReviews, courseStats]
  );

  const totalWrongCount = useMemo(
    () => calculateTotalWrongCount(wrongReviews),
    [wrongReviews]
  );

  const avgWrongCount = useMemo(
    () => calculateAvgWrongCount(wrongReviews),
    [wrongReviews]
  );

  // === 퀴즈 재도전 액션 ===
  const resetRetryState = useCallback(() => {
    setRetryState(INITIAL_RETRY_STATE);
  }, []);

  const handleRetry = useCallback(
    (item: ReviewItemDto) => {
      setRetryState({
        selectedReviewItem: item,
        isRetrying: true,
        selectedAnswer: null,
        selectedAnswers: [],
        shortAnswer: '',
        showResult: false,
        isCorrectAnswer: null,
      });
      timer.start();
    },
    [timer]
  );

  const handleToggleAnswer = useCallback((index: number) => {
    setRetryState((prev) => ({
      ...prev,
      selectedAnswers: prev.selectedAnswers.includes(index)
        ? prev.selectedAnswers.filter((i) => i !== index)
        : [...prev.selectedAnswers, index].sort((a, b) => a - b),
    }));
  }, []);

  const setSelectedAnswer = useCallback((index: number | null) => {
    setRetryState((prev) => ({ ...prev, selectedAnswer: index }));
  }, []);

  const setShortAnswer = useCallback((answer: string | null) => {
    setRetryState((prev) => ({ ...prev, shortAnswer: answer }));
  }, []);

  const handleSubmitMultiple = useCallback(async () => {
    const { selectedReviewItem, selectedAnswer, selectedAnswers } = retryState;
    if (!selectedReviewItem) return;

    const isSingleType = selectedReviewItem.question.questionType === 'MULTIPLE_CHOICE';
    const hasAnswer = isSingleType ? selectedAnswer !== null : selectedAnswers.length > 0;

    if (!hasAnswer) return;

    const responseTimeMs = timer.stop();

    // 옵션 ID 변환 헬퍼
    const getOptionId = (idx: number) => {
      const option = selectedReviewItem.question.options[idx];
      return option?.id || indexToOptionId(idx);
    };

    const answerString = isSingleType
      ? getOptionId(selectedAnswer as number)
      : selectedAnswers.sort((a, b) => a - b).map(getOptionId).join(',');

    try {
      const result = await submitReview({
        contentType: selectedReviewItem.contentType,
        contentId: selectedReviewItem.contentId,
        userAnswer: answerString,
        responseTimeMs,
      });
      setRetryState((prev) => ({
        ...prev,
        isCorrectAnswer: result.isCorrect,
        showResult: true,
      }));
    } catch (e) {
      console.error('Review submission failed', e);
      setRetryState((prev) => ({
        ...prev,
        isCorrectAnswer: false,
        showResult: true,
      }));
    }
  }, [retryState, timer]);

  const handleSubmitShort = useCallback(async () => {
    const { selectedReviewItem, shortAnswer } = retryState;
    if ((shortAnswer !== null && !shortAnswer.trim()) || !selectedReviewItem) return;

    const responseTimeMs = timer.stop();

    try {
      const result = await submitReview({
        contentType: selectedReviewItem.contentType,
        contentId: selectedReviewItem.contentId,
        userAnswer: shortAnswer,
        responseTimeMs,
      });
      setRetryState((prev) => ({
        ...prev,
        isCorrectAnswer: result.isCorrect,
        showResult: true,
      }));
    } catch (e) {
      console.error('Review submission failed', e);
      setRetryState((prev) => ({
        ...prev,
        isCorrectAnswer: false,
        showResult: true,
      }));
    }
  }, [retryState, timer]);

  const handleFinishRetry = useCallback(() => {
    resetRetryState();
    fetchData();
  }, [resetRetryState, fetchData]);

  // === 반환 객체 (useCallback으로 래핑된 setters 포함) ===
  return {
    // 데이터 상태
    todayReviews,
    wrongReviews,
    weakConcepts,
    courseQuizStats,
    reviewStats,
    loading,

    // 탭 관리
    activeTab,
    setActiveTab,

    // 정렬 관리
    wrongSortType,
    setWrongSortType,

    // 퀴즈 재도전 상태 (전체 객체 + 개별 setters)
    retryState: {
      ...retryState,
      // 개별 필드 업데이트를 위한 함수들을 상태와 함께 제공
    },

    // 퀴즈 재도전 액션
    handleRetry,
    handleToggleAnswer,
    handleSubmitMultiple,
    handleSubmitShort,
    handleFinishRetry,
    resetRetryState,

    // 통계
    totalWrongCount,
    avgWrongCount,

    // 데이터 갱신
    fetchData,
  };
};

// 개별 상태 업데이트를 위한 추가 훅 (필요 시 사용)
export const useQuizRetryActions = (
  setRetryState: React.Dispatch<React.SetStateAction<QuizRetryState>>
) => {
  const setSelectedAnswer = useCallback(
    (index: number | null) => {
      setRetryState((prev) => ({ ...prev, selectedAnswer: index }));
    },
    [setRetryState]
  );

  const setShortAnswer = useCallback(
    (answer: string | null) => {
      setRetryState((prev) => ({ ...prev, shortAnswer: answer }));
    },
    [setRetryState]
  );

  return { setSelectedAnswer, setShortAnswer };
};

export default useMyQuiz;
