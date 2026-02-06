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
  const [wrongTotalCount, setWrongTotalCount] = useState(0);
  const [courseStats, setCourseStats] = useState<ReviewCourseWeaknessResponse | null>(null);
  const [courseQuizStats, setCourseQuizStats] = useState<CourseQuizStat[]>([]);
  const [reviewStats, setReviewStats] = useState<ReviewStatsResponse | null>(null);
  const [loading, setLoading] = useState(false);

  // === UI 상태 ===
  const [activeTab, setActiveTab] = useState<TabType>('review');
  const [wrongSortType, setWrongSortType] = useState<WrongAnswerSortType>('LATEST');
  const [wrongPage, setWrongPage] = useState(1);

  // === 퀴즈 재도전 상태 ===
  const [retryState, setRetryState] = useState<QuizRetryState>(INITIAL_RETRY_STATE);

  // === 타이머 훅 ===
  const timer = useTimer();

  // === 데이터 패칭 (개별 API 실패가 다른 데이터에 영향을 주지 않도록 분리) ===
  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      // 클라이언트에서 SHORT_ANSWER를 필터링하므로, 틀린 문제는 전체를 가져와서 클라이언트 페이지네이션 사용
      const results = await Promise.allSettled([
        getTodayReviews(),
        getWrongAnswers(wrongSortType, 0, 10000),
        getCourseWeaknessStats(),
        getCourseQuizStats(),
        getReviewStats(),
      ]);

      const todayData = results[0].status === 'fulfilled' ? results[0].value : null;
      const wrongData = results[1].status === 'fulfilled' ? results[1].value : null;
      const statsData = results[2].status === 'fulfilled' ? results[2].value : null;
      const quizStatsData = results[3].status === 'fulfilled' ? results[3].value : null;
      const reviewStatsData = results[4].status === 'fulfilled' ? results[4].value : null;

      // question이 null인 항목 필터링 + 주관식(SHORT_ANSWER) 임시 제외
      const filteredToday = (todayData?.items || []).filter(
        item => item.question != null && item.question.questionType !== 'SHORT_ANSWER'
      ) as ReviewItemDto[];
      const filteredWrong = (wrongData?.items || []).filter(
        item => item.question != null && item.question.questionType !== 'SHORT_ANSWER'
      ) as ReviewItemDto[];
      setTodayReviews(filteredToday);
      setWrongReviews(filteredWrong);
      setWrongTotalCount(filteredWrong.length);
      setCourseStats(statsData || null);
      setCourseQuizStats(quizStatsData || []);
      setReviewStats(reviewStatsData || null);
    } catch (error) {
      console.error('Failed to fetch reviews', error);
      setTodayReviews([]);
      setWrongReviews([]);
      setWrongTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [wrongSortType]);

  // 정렬 타입 변경 시 데이터 재패칭 및 페이지 초기화
  useEffect(() => {
    setWrongPage(1);
  }, [wrongSortType]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

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

  // retryState 내부 필드 개별 setter (MyQuizPage에서 직접 사용)
  const setSelectedAnswer = useCallback((index: number | null) => {
    setRetryState((prev) => ({ ...prev, selectedAnswer: index }));
  }, []);

  const setShortAnswer = useCallback((answer: string | null) => {
    setRetryState((prev) => ({ ...prev, shortAnswer: answer }));
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

    // 정렬 및 페이지 관리
    wrongSortType,
    setWrongSortType,
    wrongPage,
    setWrongPage,
    wrongTotalCount,

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
    setSelectedAnswer,
    setShortAnswer,

    // 통계
    totalWrongCount,
    avgWrongCount,

    // 데이터 갱신
    fetchData,
  };
};

export default useMyQuiz;
