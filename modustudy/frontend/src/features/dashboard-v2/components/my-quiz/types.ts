import { ReviewItemDto, WrongAnswerSortType } from '../../api/reviewApi';
import { CourseQuizStat } from '@/api/endpoints/continuousQuizApi';

// 탭 타입 정의
export type TabType = 'review' | 'wrong' | 'weak' | 'stats';

// 취약 개념 인터페이스
export interface WeakConcept {
  id: string;
  concept: string;
  category: string;
  wrongCount: number;
  totalReps: number;
  wrongRate: number; // (wrongCount / totalReps) * 100
  relatedQuestions: number[];
  lastReviewDate: string;
}

// 탭 구성 정보 인터페이스
export interface TabConfig {
  id: TabType;
  label: string;
  icon: React.ComponentType<{ size?: number }>;
  count?: number;
}

// 퀴즈 재도전 상태 인터페이스
export interface QuizRetryState {
  selectedReviewItem: ReviewItemDto | null;
  isRetrying: boolean;
  selectedAnswer: number | null;
  selectedAnswers: number[];
  shortAnswer: string | null;
  showResult: boolean;
  isCorrectAnswer: boolean | null;
}

// 카테고리 통계 인터페이스
export interface CategoryStats {
  questionCount: number;
  totalWrongCount: number;
}

// 기술 스택 숙련도 인터페이스
export interface TechStackProficiency {
  name: string;
  level: number;
  brandColor: string;
  quizCount: number;
  correctRate: number;
  logo: React.ReactNode;
}

// ReviewItemList 컴포넌트 Props
export interface ReviewItemListProps {
  items: ReviewItemDto[];
  onRetry: (item: ReviewItemDto) => void;
  type: 'review' | 'wrong';
}

// WeakConceptList 컴포넌트 Props
export interface WeakConceptListProps {
  concepts: WeakConcept[];
}

// StatsView 컴포넌트 Props
export interface StatsViewProps {
  wrongReviews: ReviewItemDto[];
  totalWrong: number;
  totalWrongCount: number;
  avgWrongCount: string;
  weakConcepts: WeakConcept[];
  courseQuizStats: CourseQuizStat[];
}

// QuizRetryView 컴포넌트 Props
export interface QuizRetryViewProps {
  selectedReviewItem: ReviewItemDto;
  selectedAnswer: number | null;
  selectedAnswers: number[];
  shortAnswer: string | null;
  showResult: boolean;
  isCorrectAnswer: boolean | null;
  onSelectAnswer: (index: number) => void;
  onToggleAnswer: (index: number) => void;
  onChangeShortAnswer: (answer: string | null) => void;
  onSubmitMultiple: () => void;
  onSubmitShort: () => void;
  onFinishRetry: () => void;
}

// useMyQuiz 훅 반환 타입
export interface UseMyQuizReturn {
  // 데이터 상태
  todayReviews: ReviewItemDto[];
  wrongReviews: ReviewItemDto[];
  weakConcepts: WeakConcept[];
  courseQuizStats: CourseQuizStat[];
  loading: boolean;

  // 탭 관리
  activeTab: TabType;
  setActiveTab: (tab: TabType) => void;

  // 정렬 관리
  wrongSortType: WrongAnswerSortType;
  setWrongSortType: (type: WrongAnswerSortType) => void;

  // 퀴즈 재도전 상태
  retryState: QuizRetryState;

  // 퀴즈 재도전 액션
  handleRetry: (item: ReviewItemDto) => void;
  handleToggleAnswer: (index: number) => void;
  handleSubmitMultiple: () => Promise<void>;
  handleSubmitShort: () => Promise<void>;
  handleFinishRetry: () => void;
  resetRetryState: () => void;

  // 통계
  totalWrongCount: number;
  avgWrongCount: string;

  // 데이터 갱신
  fetchData: () => Promise<void>;
}
