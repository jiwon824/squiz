import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { AlertTriangle, CheckCircle2, XCircle } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import {
  QuizSingleChoice,
  QuizMultipleChoice,
  QuizShortAnswer,
} from '@/shared/components';
import { transformToQuizQuestion } from '@/shared/utils/quizUtils';
import { QuizRetryViewProps } from './types';

/**
 * 퀴즈 재도전 화면 컴포넌트
 *
 * 특징:
 * - 문제 유형별 다른 UI (단일선택, 다중선택)
 * - 오답 횟수 표시
 * - 채점 결과 애니메이션
 */
export const QuizRetryView: React.FC<QuizRetryViewProps> = React.memo(
  ({
    selectedReviewItem,
    selectedAnswer,
    selectedAnswers,
    shortAnswer,
    showResult,
    isCorrectAnswer,
    onSelectAnswer,
    onToggleAnswer,
    onChangeShortAnswer,
    onSubmitMultiple,
    onSubmitShort,
    onFinishRetry,
  }) => {
    const questionType = selectedReviewItem.question.questionType;
    const quizData = transformToQuizQuestion(selectedReviewItem);

    return (
      <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
        <div className="p-8">
          <AnimatePresence mode="wait">
            <motion.div
              key="retry"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
            >
              {/* 틀린 횟수 표시 */}
              <WrongCountBanner lapses={selectedReviewItem.lapses} />

              {/* 문제 유형별 렌더링 */}
              {questionType === 'MULTIPLE_CHOICE' && (
                <QuizSingleChoice
                  quiz={quizData}
                  selectedAnswer={selectedAnswer}
                  showResult={showResult}
                  onSelectAnswer={onSelectAnswer}
                  onSubmit={onSubmitMultiple}
                />
              )}

              {questionType === 'MULTIPLE_CHOICE_MULTIPLE' && (
                <QuizMultipleChoice
                  quiz={quizData}
                  selectedAnswers={selectedAnswers}
                  showResult={showResult}
                  onToggleAnswer={onToggleAnswer}
                  onSubmit={onSubmitMultiple}
                />
              )}

              {questionType === 'SHORT_ANSWER' && (
                <QuizShortAnswer
                  quiz={quizData}
                  userAnswer={shortAnswer}
                  showResult={showResult}
                  onChangeAnswer={onChangeShortAnswer}
                  onSubmit={onSubmitShort}
                />
              )}

              {/* 결과 표시 */}
              {showResult && (
                <ResultSection
                  isCorrect={isCorrectAnswer}
                  onFinish={onFinishRetry}
                />
              )}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    );
  }
);

QuizRetryView.displayName = 'QuizRetryView';

// === 오답 횟수 배너 ===
interface WrongCountBannerProps {
  lapses: number;
}

const WrongCountBanner: React.FC<WrongCountBannerProps> = React.memo(({ lapses }) => (
  <div className="flex items-center gap-2 mb-4 p-3 bg-error/5 rounded-xl border border-error/20">
    <AlertTriangle className="text-error" size={18} />
    <span className="text-sm text-error font-medium">
      이 문제를 {lapses}번 틀렸습니다
    </span>
  </div>
));

WrongCountBanner.displayName = 'WrongCountBanner';

// === 결과 섹션 ===
interface ResultSectionProps {
  isCorrect: boolean | null;
  onFinish: () => void;
}

const ResultSection: React.FC<ResultSectionProps> = React.memo(
  ({ isCorrect, onFinish }) => (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="mt-4 space-y-4"
    >
      {/* 정답/오답 결과 표시 */}
      <div
        className={cn(
          'p-4 rounded-xl border-2 flex items-center gap-3',
          isCorrect
            ? 'bg-accent/10 border-accent/30'
            : 'bg-error/10 border-error/30'
        )}
      >
        {isCorrect ? (
          <>
            <CheckCircle2 className="text-accent" size={24} />
            <div>
              <p className="font-bold text-accent">정답입니다!</p>
              <p className="text-sm text-text-secondary">
                이제 이 개념을 확실히 이해하셨네요.
              </p>
            </div>
          </>
        ) : (
          <>
            <XCircle className="text-error" size={24} />
            <div>
              <p className="font-bold text-error">오답입니다</p>
              <p className="text-sm text-text-secondary">
                해설을 다시 확인해보세요.
              </p>
            </div>
          </>
        )}
      </div>

      {/* 목록으로 돌아가기 버튼 */}
      <button
        onClick={onFinish}
        className="w-full py-3 rounded-xl font-bold bg-secondary hover:bg-secondary-dark text-white transition-colors"
      >
        목록으로 돌아가기
      </button>
    </motion.div>
  )
);

ResultSection.displayName = 'ResultSection';

export default QuizRetryView;
