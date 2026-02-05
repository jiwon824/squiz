import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Brain, Play } from 'lucide-react';
import { WeakConcept } from '../types';

interface LearningRecommendationProps {
  weakConcepts: WeakConcept[];
}

/**
 * 학습 제안을 표시하는 컴포넌트
 */
export const LearningRecommendation: React.FC<LearningRecommendationProps> = React.memo(
  ({ weakConcepts }) => {
    const navigate = useNavigate();
    const topWeakConcept = weakConcepts[0];

    // 네비게이션 핸들러
    const handleNavigate = React.useCallback(() => {
      if (topWeakConcept?.courseId) {
        navigate(`/quiz-practice/${topWeakConcept.courseId}`);
      }
    }, [navigate, topWeakConcept?.courseId]);

    if (!topWeakConcept) {
      return (
        <div className="rounded-xl border border-gray-100 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
            <h3 className="font-semibold text-text-primary mb-0">학습 제안</h3>
          </div>
          <div className="p-5">
            <div className="px-5 py-4 bg-gray-50 rounded-xl text-center">
              <p className="text-text-tertiary">
                취약 개념 분석 데이터가 충분하지 않습니다.
              </p>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="rounded-xl border border-gray-100 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
          <h3 className="font-semibold text-text-primary mb-0">학습 제안</h3>
        </div>
        <div className="p-5">
          <div className="px-5 py-4 bg-primary/5 rounded-xl">
            <div className="flex items-start gap-4">
              <div className="w-11 h-11 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                <Brain className="text-primary/80" size={20} />
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-text-primary mb-1">
                  {topWeakConcept.concept} 집중 학습 권장
                </h4>
                <p className="text-sm text-text-secondary leading-relaxed">
                  가장 많이 틀린 개념입니다. 관련 문제를 다시 풀어보고,
                  해당 개념에 대한 추가 학습을 권장합니다.
                </p>
                {topWeakConcept.courseId && (
                  <button
                    onClick={handleNavigate}
                    className="mt-4 flex items-center gap-2 text-sm font-medium text-primary hover:text-primary-dark transition-colors"
                  >
                    <Play size={14} />
                    관련 문제 풀기
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
);

LearningRecommendation.displayName = 'LearningRecommendation';

export default LearningRecommendation;
