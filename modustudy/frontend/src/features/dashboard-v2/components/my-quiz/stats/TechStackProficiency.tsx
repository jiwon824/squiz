import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import type { CourseQuizStat } from '@/api/endpoints/continuousQuizApi';
import { CATEGORY_CONFIG, DEFAULT_CATEGORY_CONFIG } from '@/features/quiz/types/QuizCourse.types';
import { TECH_ITEMS } from '@/shared/constants/techItems';
import { LucideIcon } from 'lucide-react';

// ─────────────────────────────────────────────────────────────────────────────
// 통계 전용 색상 매핑: courseCode → TECH_ITEMS id 매핑 테이블
// 브랜드 기술(React, Java 등)은 유색, CS 과목(OS, Algorithm 등)은 회색(#9CA3AF)
// ─────────────────────────────────────────────────────────────────────────────
const COURSE_TO_TECH_ID_MAP: Record<string, string> = {
    JAVA_SPRING: 'Spring Boot',
    REACT: 'React',
    PYTHON: 'Python',
    NODEJS: 'Node.js',
    KOTLIN: 'Kotlin',
    TYPESCRIPT: 'TypeScript',
    JAVASCRIPT: 'JavaScript',
    LINUX: 'Linux',
    GIT: 'Git',
};

// 통계용 회색 (CS 과목 및 미매핑 항목에 사용)
const STATS_GRAY_COLOR = '#9CA3AF';

/**
 * 통계 전용 색상 결정 함수
 * - TECH_ITEMS에 매핑되는 브랜드 기술: 해당 브랜드 색상 사용
 * - CS 과목 또는 TECH_ITEMS에 없는 항목: 회색(#9CA3AF) 사용
 */
const getStatsColor = (courseCode: string | undefined): string => {
    if (!courseCode) return STATS_GRAY_COLOR;

    // courseCode를 TECH_ITEMS의 id로 변환
    const techId = COURSE_TO_TECH_ID_MAP[courseCode];

    if (techId) {
        // TECH_ITEMS에서 해당 기술 찾기
        const techItem = TECH_ITEMS.find((item) => item.id === techId);
        if (techItem && techItem.id !== 'Other') {
            return techItem.color;
        }
    }

    // 매핑 안 되거나 Other인 경우 회색 반환
    return STATS_GRAY_COLOR;
};

interface TechStackProficiencyViewProps {
  courseStats?: CourseQuizStat[];
}

interface ProcessedTechStack {
  name: string;
  level: number;
  quizCount: number;
  correctRate: number;
  color: string;
  Icon: LucideIcon;
}

/**
 * 기술 스택 숙련도를 표시하는 컴포넌트
 *
 * courseCode를 기반으로 CATEGORY_CONFIG에서 스타일(아이콘, 색상, 그라데이션)을 가져옵니다.
 * 매칭되는 코드가 없을 경우 DEFAULT_CATEGORY_CONFIG를 사용하여 방어합니다.
 */
export const TechStackProficiencyView: React.FC<TechStackProficiencyViewProps> = React.memo(
  ({ courseStats = [] }) => {
    // 코스 통계 데이터를 시각화용 데이터로 변환 (useMemo)
    // 통계 전용 로직: 브랜드 기술은 유색, CS 과목은 회색
    const techStackData = useMemo<ProcessedTechStack[]>(() => {
      if (!courseStats || courseStats.length === 0) return [];

      return courseStats.map((stat) => {
        // 1. Config Lookup (아이콘, 라벨용)
        // 코드가 있으면 Config에서 찾고, 없으면 fallback 사용
        const config =
          (stat.courseCode && CATEGORY_CONFIG[stat.courseCode]) || DEFAULT_CATEGORY_CONFIG;

        // 2. 통계 전용 색상 결정 (TECH_ITEMS 기반)
        // - 브랜드 기술(React, Java 등): TECH_ITEMS의 브랜드 색상
        // - CS 과목(OS, Algorithm 등) 또는 미매핑: 회색(#9CA3AF)
        const statsColor = getStatsColor(stat.courseCode);

        // 3. Calculate Metrics
        const correctRate =
          stat.attemptedCount > 0
            ? Math.round((stat.correctCount / stat.attemptedCount) * 100)
            : 0;

        // 숙련도(Level)는 현재 정답률과 동일하게 설정 (추후 가중치 로직 추가 가능)
        const level = correctRate;

        return {
          name: config.label, // 한글 라벨 (예: "리액트", "자바")
          level,
          quizCount: stat.attemptedCount,
          correctRate,
          color: statsColor, // 통계 전용 색상 사용
          Icon: config.icon,
        };
      });
    }, [courseStats]);

    if (techStackData.length === 0) {
      return (
        <div className="rounded-xl border border-gray-100 p-8 text-center bg-gray-50/50">
          <p className="text-text-tertiary">아직 학습한 기술 스택 데이터가 없습니다.</p>
        </div>
      );
    }

    return (
      <div className="rounded-xl border border-gray-100 overflow-hidden bg-white shadow-sm">
        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50 flex items-center justify-between">
          <div>
            <h3 className="font-semibold text-text-primary mb-0">나의 기술 스택 범위</h3>
            <p className="text-xs text-text-tertiary mt-1">퀴즈 학습 데이터 기반 숙련도</p>
          </div>
        </div>
        <div className="p-5">
          {/* 막대 그래프 */}
          <div className="space-y-5">
            {techStackData.map((tech, index) => (
              <TechStackBar key={`${tech.name}-${index}`} tech={tech} />
            ))}
          </div>

          {/* 원형 차트 스타일 요약 - 5개까지만 노출 */}
          <div className="mt-8 pt-6 border-t border-gray-50">
            <div className="grid grid-cols-5 gap-4">
              {techStackData.slice(0, 5).map((tech, index) => (
                <TechStackCircle key={`${tech.name}-${index}-circle`} tech={tech} />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }
);

TechStackProficiencyView.displayName = 'TechStackProficiencyView';

// === 막대 그래프 아이템 ===
interface TechStackBarProps {
  tech: ProcessedTechStack;
}

const TechStackBar: React.FC<TechStackBarProps> = React.memo(({ tech }) => (
  <div>
    <div className="flex items-center justify-between mb-2">
      <div className="flex items-center gap-3">
        {/* 아이콘 박스 */}
        <span
          className="flex items-center justify-center w-8 h-8 rounded-lg shadow-sm"
          style={{
            background: `linear-gradient(135deg, ${tech.color}15 0%, ${tech.color}05 100%)`,
            border: `1px solid ${tech.color}20`,
          }}
        >
          <tech.Icon size={16} style={{ color: tech.color }} />
        </span>
        <span className="text-sm font-semibold text-text-primary">{tech.name}</span>
      </div>
      <div className="flex items-center gap-4 text-xs text-text-tertiary">
        <div className="flex items-center gap-1">
          <span className="w-1.5 h-1.5 rounded-full bg-gray-300" />
          <span>퀴즈 {tech.quizCount}개</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="w-1.5 h-1.5 rounded-full bg-gray-300" />
          <span>정답률 {tech.correctRate}%</span>
        </div>
        <span className="font-bold text-base" style={{ color: tech.color }}>
          {tech.level}%
        </span>
      </div>
    </div>
    {/* 프로그레스 바 트랙 */}
    <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden shadow-inner">
      {/* 프로그레스 바 Fill (Gradient 적용) */}
      <motion.div
        initial={{ width: 0 }}
        animate={{ width: `${tech.level}%` }}
        transition={{ duration: 1.2, ease: 'easeOut' }}
        className="h-full rounded-full relative"
        style={{ backgroundColor: tech.color }}
      >
        {/* 광택 효과 (Shine Effect) */}
        <div
          className="absolute top-0 left-0 right-0 h-[1px]"
          style={{ background: 'rgba(255,255,255,0.4)' }}
        />
      </motion.div>
    </div>
  </div>
));

TechStackBar.displayName = 'TechStackBar';

// === 원형 차트 아이템 ===
interface TechStackCircleProps {
  tech: ProcessedTechStack;
}

const TechStackCircle: React.FC<TechStackCircleProps> = React.memo(({ tech }) => {
  const radius = 24;
  const circumference = 2 * Math.PI * radius;

  return (
    <div className="flex flex-col items-center group cursor-default">
      <div className="relative w-16 h-16 mb-2 transition-transform duration-300 group-hover:scale-105">
        <svg className="w-full h-full -rotate-90 transform" viewBox="0 0 64 64">
          {/* 배경 원 */}
          <circle
            cx="32"
            cy="32"
            r={radius}
            fill="none"
            stroke="#f3f4f6"
            strokeWidth="4"
          />
          {/* 진행 원 (Gradient Stroke는 SVG에서 복잡하므로 solid color 사용하거나 defs 필요. 우선 color 사용) */}
          <motion.circle
            cx="32"
            cy="32"
            r={radius}
            fill="none"
            stroke={tech.color}
            strokeWidth="4"
            strokeLinecap="round"
            strokeDasharray={circumference}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: circumference * (1 - tech.level / 100) }}
            transition={{ duration: 1.5, ease: 'easeOut', delay: 0.2 }}
            className="drop-shadow-sm"
          />
        </svg>
        {/* 중앙 아이콘 */}
        <div className="absolute inset-0 flex items-center justify-center">
          <tech.Icon
            size={20}
            style={{ color: tech.color }}
            className="transition-opacity opacity-90 group-hover:opacity-100"
          />
        </div>
      </div>
      <span className="text-xs font-semibold text-text-secondary mb-0.5">{tech.name}</span>
      <span className="text-[10px] font-medium text-text-tertiary">{tech.level}%</span>
    </div>
  );
});

TechStackCircle.displayName = 'TechStackCircle';

export default TechStackProficiencyView;
