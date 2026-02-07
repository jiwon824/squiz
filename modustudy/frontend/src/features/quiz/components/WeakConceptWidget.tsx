import React, { useEffect, useState } from 'react';
import { fetchWeakConcepts, WeakConcept } from '../../../api/endpoints/quizCourseApi';
import { BookOpen, AlertTriangle, AlertCircle, RefreshCw } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';

export const WeakConceptWidget: React.FC = () => {
  const [concepts, setConcepts] = useState<WeakConcept[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadConcepts = async () => {
    setLoading(true);
    setError(false);
    try {
      const data = await fetchWeakConcepts(5);
      setConcepts(data);
    } catch (err) {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConcepts();
  }, []);

  if (loading) {
    return (
      <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100 h-full flex flex-col items-center justify-center">
        <Spinner size="md" color="#f87171" className="mb-2" />
        <p className="text-xs text-gray-400">불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100 h-full flex flex-col items-center justify-center">
        <div className="text-center py-12">
          <AlertCircle className="mx-auto text-gray-300 mb-4" size={48} />
          <p className="text-text-secondary">불러오지 못했어요</p>
          <p className="text-sm text-text-tertiary mt-1">네트워크 상태를 확인해주세요</p>
          <button
            onClick={loadConcepts}
            className="inline-flex items-center gap-1.5 mt-4 px-4 py-2 text-sm font-medium text-text-tertiary hover:text-text-secondary transition-colors"
          >
            <RefreshCw size={14} />
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100 h-full flex flex-col">
      <div className="flex items-center gap-2 mb-4">
        <div className="p-2 bg-red-50 rounded-lg">
          <AlertTriangle className="text-error" size={20} />
        </div>
        <h3 className="text-lg font-bold text-text-primary">
          집중 학습 필요
        </h3>
      </div>

      {concepts.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
          <BookOpen className="text-text-tertiary mb-3 opacity-50" size={32} />
          <p className="text-text-secondary font-medium">발견된 취약점이 없습니다.</p>
          <p className="text-text-tertiary text-sm mt-1">완벽하게 이해하고 계시네요! 🎉</p>
        </div>
      ) : (
        <ul className="space-y-4 flex-1 overflow-y-auto pr-2 custom-scrollbar">
          {concepts.map((concept) => (
            <li
              key={`${concept.courseId}-${concept.sectionNumber}`}
              className="group p-3 rounded-xl hover:bg-gray-50 transition-colors border border-transparent hover:border-gray-100"
            >
              <div className="flex justify-between items-start mb-2">
                <div className="flex flex-col">
                  <span className="text-xs font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded-md w-fit mb-1">
                    {concept.courseName}
                  </span>
                  <span className="text-sm font-medium text-text-primary group-hover:text-primary transition-colors">
                    {concept.sectionNumber}. {concept.sectionName}
                  </span>
                </div>
                <span className="text-xs font-bold text-error whitespace-nowrap bg-error/5 px-2 py-1 rounded-full">
                  취약도 {Math.round(concept.weaknessScore)}
                </span>
              </div>

              <div className="h-2 w-full bg-gray-100 rounded-full overflow-hidden">
                <div
                  className={cn(
                    "h-full rounded-full transition-all duration-500 ease-out",
                    concept.weaknessScore >= 80 ? "bg-error" :
                      concept.weaknessScore >= 50 ? "bg-accent" : "bg-primary"
                  )}
                  style={{ width: `${Math.min(concept.weaknessScore, 100)}%` }}
                />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
