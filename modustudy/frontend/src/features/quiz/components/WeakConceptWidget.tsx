import React, { useEffect, useState } from 'react';
import { fetchWeakConcepts, WeakConcept } from '../../../api/endpoints/quizCourseApi';
import { FaBookOpen, FaExclamationTriangle } from 'react-icons/fa';
import { cn } from '@/shared/utils/cn';

export const WeakConceptWidget: React.FC = () => {
  const [concepts, setConcepts] = useState<WeakConcept[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadConcepts = async () => {
      try {
        const data = await fetchWeakConcepts(5);
        setConcepts(data);
      } catch (err) {
        setError('취약 개념을 불러오는데 실패했습니다.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadConcepts();
  }, []);

  if (loading) {
    return (
      <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100 h-full flex flex-col items-center justify-center">
        <p className="text-text-secondary text-base animate-pulse">취약 개념 분석 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100 h-full flex flex-col items-center justify-center">
        <FaExclamationTriangle className="text-error mb-2" size={24} />
        <p className="text-text-secondary text-sm">{error}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl p-6 shadow-md border border-gray-100 h-full flex flex-col">
      <div className="flex items-center gap-2 mb-4">
        <div className="p-2 bg-red-50 rounded-lg">
          <FaExclamationTriangle className="text-error" size={20} />
        </div>
        <h3 className="text-lg font-bold text-text-primary">
          집중 학습 필요
        </h3>
      </div>

      {concepts.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center text-center p-4">
          <FaBookOpen className="text-text-tertiary mb-3 opacity-50" size={32} />
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
