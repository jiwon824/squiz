import React from 'react';
import { motion } from 'framer-motion';
import { TechStackProficiency as TechStackType } from '../types';

// 기술 스택 숙련도 Mock 데이터 (공식 브랜드 컬러 적용)
export const TECH_STACK_DATA: TechStackType[] = [
  {
    name: 'React',
    level: 75,
    brandColor: '#61DBFB',
    quizCount: 24,
    correctRate: 78,
    logo: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
        <path d="M12 10.11c1.03 0 1.87.84 1.87 1.89 0 1-.84 1.85-1.87 1.85S10.13 13 10.13 12c0-1.05.84-1.89 1.87-1.89M7.37 20c.63.38 2.01-.2 3.6-1.7-.52-.59-1.03-1.23-1.51-1.9a22.7 22.7 0 01-2.4-.36c-.51 2.14-.32 3.61.31 3.96m.71-5.74l-.29-.51c-.11.29-.22.58-.29.86.27.06.57.11.88.16l-.3-.51m6.54-.76l.81-1.5-.81-1.5c-.3-.53-.62-1-.91-1.47C13.17 9 12.6 9 12 9s-1.17 0-1.71.03c-.29.47-.61.94-.91 1.47L8.57 12l.81 1.5c.3.53.62 1 .91 1.47.54.03 1.11.03 1.71.03s1.17 0 1.71-.03c.29-.47.61-.94.91-1.47M12 6.78c-.19.22-.39.45-.59.72h1.18c-.2-.27-.4-.5-.59-.72m0 10.44c.19-.22.39-.45.59-.72h-1.18c.2.27.4.5.59.72M16.62 4c-.62-.38-2 .2-3.59 1.7.52.59 1.03 1.23 1.51 1.9.82.08 1.63.2 2.4.36.51-2.14.32-3.61-.32-3.96m-.7 5.74l.29.51c.11-.29.22-.58.29-.86-.27-.06-.57-.11-.88-.16l.3.51m1.45-7.05c1.47.84 1.63 3.05 1.01 5.63 2.54.75 4.37 1.99 4.37 3.68s-1.83 2.93-4.37 3.68c.62 2.58.46 4.79-1.01 5.63-1.46.84-3.45-.12-5.37-1.95-1.92 1.83-3.91 2.79-5.38 1.95-1.46-.84-1.62-3.05-1-5.63-2.54-.75-4.37-1.99-4.37-3.68s1.83-2.93 4.37-3.68c-.62-2.58-.46-4.79 1-5.63 1.47-.84 3.46.12 5.38 1.95 1.92-1.83 3.91-2.79 5.37-1.95M17.08 12c.34.75.64 1.5.89 2.26 2.1-.63 3.28-1.53 3.28-2.26s-1.18-1.63-3.28-2.26c-.25.76-.55 1.51-.89 2.26M6.92 12c-.34-.75-.64-1.5-.89-2.26-2.1.63-3.28 1.53-3.28 2.26s1.18 1.63 3.28 2.26c.25-.76.55-1.51.89-2.26m9 2.26l-.3.51c.31-.05.61-.1.88-.16-.07-.28-.18-.57-.29-.86l-.29.51m-9.46.86c.27.06.57.11.88.16l-.3-.51-.29.51c-.11.29-.22.58-.29.86m9.46-5.24l.3-.51c-.31.05-.61.1-.88.16.07.28.18.57.29.86l.29-.51m-9.46-.86c-.27-.06-.57-.11-.88-.16l.3.51.29-.51c.11-.29.22-.58.29-.86" />
      </svg>
    ),
  },
  {
    name: 'TypeScript',
    level: 60,
    brandColor: '#3178C6',
    quizCount: 18,
    correctRate: 65,
    logo: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
        <path d="M1.125 0C.502 0 0 .502 0 1.125v21.75C0 23.498.502 24 1.125 24h21.75c.623 0 1.125-.502 1.125-1.125V1.125C24 .502 23.498 0 22.875 0zm17.363 9.75c.612 0 1.154.037 1.627.111a6.38 6.38 0 0 1 1.306.34v2.458a3.95 3.95 0 0 0-.643-.361 5.093 5.093 0 0 0-.717-.26 5.453 5.453 0 0 0-1.426-.2c-.3 0-.573.028-.819.086a2.1 2.1 0 0 0-.623.242c-.17.104-.3.229-.393.374a.888.888 0 0 0-.14.49c0 .196.053.373.156.529.104.156.252.304.443.444s.423.276.696.41c.273.135.582.274.926.416.47.197.892.407 1.266.628.374.222.695.473.963.753.268.279.472.598.614.957.142.359.214.776.214 1.253 0 .657-.125 1.21-.373 1.656a3.033 3.033 0 0 1-1.012 1.085 4.38 4.38 0 0 1-1.487.596c-.566.12-1.163.18-1.79.18a9.916 9.916 0 0 1-1.84-.164 5.544 5.544 0 0 1-1.512-.493v-2.63a5.033 5.033 0 0 0 3.237 1.2c.333 0 .624-.03.872-.09.249-.06.456-.144.623-.25.166-.108.29-.234.373-.38a1.023 1.023 0 0 0-.074-1.089 2.12 2.12 0 0 0-.537-.5 5.597 5.597 0 0 0-.807-.444 27.72 27.72 0 0 0-1.007-.436c-.918-.383-1.602-.852-2.053-1.405-.45-.553-.676-1.222-.676-2.005 0-.614.123-1.141.369-1.582.246-.441.58-.804 1.004-1.089a4.494 4.494 0 0 1 1.47-.629 7.536 7.536 0 0 1 1.77-.201zm-15.113.188h9.563v2.166H9.506v9.646H6.789v-9.646H3.375z" />
      </svg>
    ),
  },
  {
    name: 'JavaScript',
    level: 85,
    brandColor: '#F7DF1E',
    quizCount: 32,
    correctRate: 88,
    logo: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
        <path d="M0 0h24v24H0V0zm22.034 18.276c-.175-1.095-.888-2.015-3.003-2.873-.736-.345-1.554-.585-1.797-1.14-.091-.33-.105-.51-.046-.705.15-.646.915-.84 1.515-.66.39.12.75.42.976.9 1.034-.676 1.034-.676 1.755-1.125-.27-.42-.404-.601-.586-.78-.63-.705-1.469-1.065-2.834-1.034l-.705.089c-.676.165-1.32.525-1.71 1.005-1.14 1.291-.811 3.541.569 4.471 1.365 1.02 3.361 1.244 3.616 2.205.24 1.17-.87 1.545-1.966 1.41-.811-.18-1.26-.586-1.755-1.336l-1.83 1.051c.21.48.45.689.81 1.109 1.74 1.756 6.09 1.666 6.871-1.004.029-.09.24-.705.074-1.65l.046.067zm-8.983-7.245h-2.248c0 1.938-.009 3.864-.009 5.805 0 1.232.063 2.363-.138 2.711-.33.689-1.18.601-1.566.48-.396-.196-.597-.466-.83-.855-.063-.105-.11-.196-.127-.196l-1.825 1.125c.305.63.75 1.172 1.324 1.517.855.51 2.004.675 3.207.405.783-.226 1.458-.691 1.811-1.411.51-.93.402-2.07.397-3.346.012-2.054 0-4.109 0-6.179l.004-.056z" />
      </svg>
    ),
  },
  {
    name: 'Tailwind',
    level: 70,
    brandColor: '#06B6D4',
    quizCount: 12,
    correctRate: 72,
    logo: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
        <path d="M12.001 4.8c-3.2 0-5.2 1.6-6 4.8 1.2-1.6 2.6-2.2 4.2-1.8.913.228 1.565.89 2.288 1.624C13.666 10.618 15.027 12 18.001 12c3.2 0 5.2-1.6 6-4.8-1.2 1.6-2.6 2.2-4.2 1.8-.913-.228-1.565-.89-2.288-1.624C16.337 6.182 14.976 4.8 12.001 4.8zm-6 7.2c-3.2 0-5.2 1.6-6 4.8 1.2-1.6 2.6-2.2 4.2-1.8.913.228 1.565.89 2.288 1.624 1.177 1.194 2.538 2.576 5.512 2.576 3.2 0 5.2-1.6 6-4.8-1.2 1.6-2.6 2.2-4.2 1.8-.913-.228-1.565-.89-2.288-1.624C10.337 13.382 8.976 12 6.001 12z" />
      </svg>
    ),
  },
  {
    name: 'Node.js',
    level: 45,
    brandColor: '#339933',
    quizCount: 8,
    correctRate: 50,
    logo: (
      <svg viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
        <path d="M11.998 24c-.321 0-.641-.084-.922-.247L8.14 22.016c-.438-.245-.224-.332-.08-.383.548-.19.659-.233 1.243-.563.062-.034.142-.021.205.016l2.255 1.339c.082.045.198.045.275 0l8.795-5.076c.082-.047.134-.141.134-.238V6.921c0-.099-.053-.193-.137-.242l-8.791-5.072c-.081-.047-.189-.047-.271 0L3.075 6.68c-.085.049-.139.143-.139.242v10.19c0 .097.054.189.137.236l2.409 1.392c1.307.654 2.108-.116 2.108-.891V7.787c0-.142.114-.253.256-.253h1.115c.139 0 .255.112.255.253v10.064c0 1.745-.951 2.745-2.604 2.745-.509 0-.909 0-2.026-.551L2.28 18.675c-.57-.329-.922-.943-.922-1.604V6.881c0-.66.351-1.274.922-1.603L11.075.203c.559-.321 1.303-.321 1.858 0l8.794 5.075c.57.329.924.943.924 1.603v10.19c0 .66-.354 1.273-.924 1.604l-8.794 5.076c-.28.163-.6.247-.924.247h-.011zm2.722-7.022c-3.863 0-4.673-1.774-4.673-3.262 0-.141.113-.253.255-.253h1.137c.127 0 .232.092.251.215.171 1.158.679 1.739 2.989 1.739 1.839 0 2.621-.416 2.621-1.391 0-.562-.222-.979-3.078-1.26-2.386-.234-3.862-.763-3.862-2.671 0-1.76 1.484-2.806 3.972-2.806 2.795 0 4.178.97 4.352 3.055a.257.257 0 01-.064.189.258.258 0 01-.182.078h-1.144c-.12 0-.226-.085-.249-.201-.277-1.227-.944-1.62-2.712-1.62-1.997 0-2.231.696-2.231 1.217 0 .632.275.816 2.983 1.174 2.68.355 3.958.858 3.958 2.741 0 1.903-1.586 2.984-4.352 2.984l.029-.028z" />
      </svg>
    ),
  },
];

interface TechStackProficiencyViewProps {
  data?: TechStackType[];
}

/**
 * 기술 스택 숙련도를 표시하는 컴포넌트
 */
export const TechStackProficiencyView: React.FC<TechStackProficiencyViewProps> = React.memo(
  ({ data = TECH_STACK_DATA }) => {
    return (
      <div className="rounded-xl border border-gray-100 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
          <h3 className="font-semibold text-text-primary mb-0">나의 기술 스택 범위</h3>
          <p className="text-xs text-text-tertiary mt-1">퀴즈 학습 데이터 기반 숙련도</p>
        </div>
        <div className="p-5">
          {/* 막대 그래프 */}
          <div className="space-y-5">
            {data.map((tech) => (
              <TechStackBar key={tech.name} tech={tech} />
            ))}
          </div>

          {/* 원형 차트 스타일 요약 */}
          <div className="mt-6 pt-5 border-t border-gray-50">
            <div className="grid grid-cols-5 gap-3">
              {data.map((tech) => (
                <TechStackCircle key={tech.name} tech={tech} />
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
  tech: TechStackType;
}

const TechStackBar: React.FC<TechStackBarProps> = React.memo(({ tech }) => (
  <div>
    <div className="flex items-center justify-between mb-2">
      <div className="flex items-center gap-3">
        <span
          className="flex items-center justify-center w-6 h-6 rounded-md"
          style={{ backgroundColor: `${tech.brandColor}15` }}
        >
          <span style={{ color: tech.brandColor }}>{tech.logo}</span>
        </span>
        <span className="text-sm font-medium text-text-primary">{tech.name}</span>
      </div>
      <div className="flex items-center gap-4 text-xs text-text-tertiary">
        <span>퀴즈 {tech.quizCount}개</span>
        <span>정답률 {tech.correctRate}%</span>
        <span className="font-semibold text-text-primary">{tech.level}%</span>
      </div>
    </div>
    <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
      <motion.div
        initial={{ width: 0 }}
        animate={{ width: `${tech.level}%` }}
        transition={{ duration: 1, ease: 'easeOut', delay: 0.1 }}
        className="h-2.5 rounded-full"
        style={{ backgroundColor: tech.brandColor }}
      />
    </div>
  </div>
));

TechStackBar.displayName = 'TechStackBar';

// === 원형 차트 아이템 ===
interface TechStackCircleProps {
  tech: TechStackType;
}

const TechStackCircle: React.FC<TechStackCircleProps> = React.memo(({ tech }) => {
  const circumference = 2 * Math.PI * 28;

  return (
    <div className="text-center">
      <div className="relative w-16 h-16 mx-auto mb-2">
        <svg className="w-full h-full -rotate-90" viewBox="0 0 64 64">
          <circle
            cx="32"
            cy="32"
            r="28"
            fill="none"
            stroke="#f3f4f6"
            strokeWidth="5"
          />
          <motion.circle
            cx="32"
            cy="32"
            r="28"
            fill="none"
            stroke={tech.brandColor}
            strokeWidth="5"
            strokeLinecap="round"
            strokeDasharray={circumference}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: circumference * (1 - tech.level / 100) }}
            transition={{ duration: 1.2, ease: 'easeOut', delay: 0.2 }}
          />
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <span style={{ color: tech.brandColor }}>{tech.logo}</span>
        </div>
      </div>
      <div className="flex flex-col items-center">
        <span className="text-xs font-medium text-text-secondary">{tech.name}</span>
        <span className="text-[10px] text-text-tertiary">{tech.level}%</span>
      </div>
    </div>
  );
});

TechStackCircle.displayName = 'TechStackCircle';

export default TechStackProficiencyView;
