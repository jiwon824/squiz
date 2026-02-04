import React, { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import '../styles/DashboardV2.css';

// 분리된 컴포넌트 및 훅 import
import {
  useMyQuiz,
  TabType,
  ReviewItemList,
  WeakConceptList,
  StatsView,
  QuizRetryView,
  TabNavigation,
  SortToggle,
} from '../components/my-quiz';

/**
 * 퀴즈 관리 페이지
 *
 * 구조:
 * - 브레드크럼 네비게이션
 * - 좌측 탭 네비게이션 (오늘의 복습, 틀린 문제, 취약 개념, 통계)
 * - 우측 콘텐츠 영역 (각 탭별 내용)
 * - 퀴즈 재도전 모드
 */
export const MyQuizPage: React.FC = () => {
  const navigate = useNavigate();

  // useMyQuiz 훅에서 모든 상태와 액션 가져오기
  const {
    todayReviews,
    wrongReviews,
    weakConcepts,
    loading,
    activeTab,
    setActiveTab,
    wrongSortType,
    setWrongSortType,
    retryState,
    handleRetry,
    handleToggleAnswer,
    handleSubmitMultiple,
    handleSubmitShort,
    handleFinishRetry,
    resetRetryState,
    totalWrongCount,
    avgWrongCount,
    courseQuizStats,
  } = useMyQuiz();

  // 퀴즈 재도전 상태에서 개별 값 사용을 위한 로컬 상태
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
  const [shortAnswer, setShortAnswer] = useState<string | null>('');

  // 뒤로가기 핸들러
  const handleBack = useCallback(() => {
    if (retryState.isRetrying) {
      resetRetryState();
      setSelectedAnswer(null);
      setShortAnswer('');
    } else {
      navigate(-1);
    }
  }, [retryState.isRetrying, resetRetryState, navigate]);

  // 재도전 시작 핸들러 (로컬 상태도 리셋)
  const handleStartRetry = useCallback(
    (item: typeof todayReviews[0]) => {
      setSelectedAnswer(null);
      setShortAnswer('');
      handleRetry(item);
    },
    [handleRetry]
  );

  // 재도전 종료 핸들러 (로컬 상태도 리셋)
  const handleEndRetry = useCallback(() => {
    setSelectedAnswer(null);
    setShortAnswer('');
    handleFinishRetry();
  }, [handleFinishRetry]);

  const { selectedReviewItem, isRetrying, showResult, isCorrectAnswer, selectedAnswers } =
    retryState;

  return (
    <div className="py-8">
      <div className="max-w-[1400px] mx-auto px-8">
        {/* 브레드크럼 + 미니멀 헤더 */}
        <PageHeader
          isRetrying={isRetrying}
          selectedReviewItem={selectedReviewItem}
          onBack={handleBack}
          onNavigateDashboard={() => navigate('/dashboard')}
        />

        {isRetrying && selectedReviewItem ? (
          // 재도전 화면
          <QuizRetryView
            selectedReviewItem={selectedReviewItem}
            selectedAnswer={selectedAnswer}
            selectedAnswers={selectedAnswers}
            shortAnswer={shortAnswer}
            showResult={showResult}
            isCorrectAnswer={isCorrectAnswer}
            onSelectAnswer={setSelectedAnswer}
            onToggleAnswer={handleToggleAnswer}
            onChangeShortAnswer={setShortAnswer}
            onSubmitMultiple={handleSubmitMultiple}
            onSubmitShort={handleSubmitShort}
            onFinishRetry={handleEndRetry}
          />
        ) : (
          // 메인 콘텐츠 (탭 + 리스트)
          <MainContent
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            todayReviews={todayReviews}
            wrongReviews={wrongReviews}
            weakConcepts={weakConcepts}
            loading={loading}
            wrongSortType={wrongSortType}
            setWrongSortType={setWrongSortType}
            totalWrongCount={totalWrongCount}
            avgWrongCount={avgWrongCount}
            courseQuizStats={courseQuizStats}
            onRetry={handleStartRetry}
          />
        )}
      </div>
    </div>
  );
};

// === 페이지 헤더 컴포넌트 ===
interface PageHeaderProps {
  isRetrying: boolean;
  selectedReviewItem: ReturnType<typeof useMyQuiz>['retryState']['selectedReviewItem'];
  onBack: () => void;
  onNavigateDashboard: () => void;
}

const PageHeader: React.FC<PageHeaderProps> = React.memo(
  ({ isRetrying, selectedReviewItem, onBack, onNavigateDashboard }) => (
    <div className="mb-6">
      {/* 브레드크럼 */}
      <nav className="flex items-center gap-1.5 text-sm mb-2">
        <button
          onClick={onNavigateDashboard}
          className="text-text-tertiary hover:text-primary transition-colors"
        >
          대시보드
        </button>
        <ChevronRight size={14} className="text-text-tertiary" />
        <button
          onClick={isRetrying ? onBack : undefined}
          className={cn(
            isRetrying
              ? 'text-text-tertiary hover:text-primary transition-colors'
              : 'text-text-primary font-medium'
          )}
        >
          퀴즈 관리
        </button>
        {isRetrying && selectedReviewItem && (
          <>
            <ChevronRight size={14} className="text-text-tertiary" />
            <span className="text-text-primary font-medium">다시 풀기</span>
          </>
        )}
      </nav>

      {/* 페이지 타이틀 + 뒤로가기 */}
      <div className="flex items-center gap-3">
        <button
          onClick={onBack}
          className="text-text-tertiary hover:text-text-primary transition-colors"
        >
          <ChevronLeft size={24} strokeWidth={1.5} />
        </button>
        <h1 className="text-2xl font-bold text-text-primary mb-0">
          {isRetrying ? '문제 다시 풀기' : '퀴즈 관리'}
        </h1>
        {isRetrying && selectedReviewItem && (
          <span className="px-3 py-1 bg-primary/10 text-primary rounded-full text-sm font-medium">
            {selectedReviewItem.question.category}
          </span>
        )}
      </div>
    </div>
  )
);

PageHeader.displayName = 'PageHeader';

// === 메인 콘텐츠 컴포넌트 ===
interface MainContentProps {
  activeTab: TabType;
  setActiveTab: (tab: TabType) => void;
  todayReviews: ReturnType<typeof useMyQuiz>['todayReviews'];
  wrongReviews: ReturnType<typeof useMyQuiz>['wrongReviews'];
  weakConcepts: ReturnType<typeof useMyQuiz>['weakConcepts'];
  loading: boolean;
  wrongSortType: ReturnType<typeof useMyQuiz>['wrongSortType'];
  setWrongSortType: ReturnType<typeof useMyQuiz>['setWrongSortType'];
  totalWrongCount: number;
  avgWrongCount: string;
  courseQuizStats: ReturnType<typeof useMyQuiz>['courseQuizStats'];
  onRetry: (item: ReturnType<typeof useMyQuiz>['todayReviews'][0]) => void;
}

const MainContent: React.FC<MainContentProps> = React.memo(
  ({
    activeTab,
    setActiveTab,
    todayReviews,
    wrongReviews,
    weakConcepts,
    loading,
    wrongSortType,
    setWrongSortType,
    totalWrongCount,
    avgWrongCount,
    courseQuizStats,
    onRetry,
  }) => (
    <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
      <div className="flex">
        {/* 좌측 탭 네비게이션 */}
        <TabNavigation
          activeTab={activeTab}
          onTabChange={setActiveTab}
          todayReviewCount={todayReviews.length}
          wrongReviewCount={wrongReviews.length}
          weakConceptCount={weakConcepts.length}
        />

        {/* 우측 콘텐츠 영역 */}
        <div className="flex-1 p-8">
          {loading ? (
            <LoadingSpinner />
          ) : (
            <AnimatePresence mode="wait">
              {activeTab === 'review' && (
                <TabContent key="review">
                  <ReviewItemList
                    items={todayReviews}
                    onRetry={onRetry}
                    type="review"
                  />
                </TabContent>
              )}

              {activeTab === 'wrong' && (
                <TabContent key="wrong">
                  <SortToggle
                    currentSort={wrongSortType}
                    onSortChange={setWrongSortType}
                  />
                  <ReviewItemList
                    items={wrongReviews}
                    onRetry={onRetry}
                    type="wrong"
                  />
                </TabContent>
              )}

              {activeTab === 'weak' && (
                <TabContent key="weak">
                  <WeakConceptList concepts={weakConcepts} />
                </TabContent>
              )}

              {activeTab === 'stats' && (
                <TabContent key="stats">
                  <StatsView
                    wrongReviews={wrongReviews}
                    totalWrong={wrongReviews.length}
                    totalWrongCount={totalWrongCount}
                    avgWrongCount={avgWrongCount}
                    weakConcepts={weakConcepts}
                    courseQuizStats={courseQuizStats}
                  />
                </TabContent>
              )}
            </AnimatePresence>
          )}
        </div>
      </div>
    </div>
  )
);

MainContent.displayName = 'MainContent';

// === 로딩 스피너 ===
const LoadingSpinner: React.FC = () => (
  <div className="flex flex-col items-center justify-center py-20">
    <div className="w-12 h-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin mb-4" />
    <p className="text-text-secondary font-medium">데이터를 불러오는 중...</p>
  </div>
);

// === 탭 콘텐츠 래퍼 (애니메이션) ===
interface TabContentProps {
  children: React.ReactNode;
}

const TabContent: React.FC<TabContentProps> = ({ children }) => (
  <motion.div
    initial={{ opacity: 0, x: 10 }}
    animate={{ opacity: 1, x: 0 }}
    exit={{ opacity: 0, x: -10 }}
  >
    {children}
  </motion.div>
);

export default MyQuizPage;
