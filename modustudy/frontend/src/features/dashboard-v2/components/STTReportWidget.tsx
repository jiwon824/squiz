import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { FileText, Calendar, Users, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { WidgetHeader, WidgetContainer } from '@/shared/components/layouts';

// Mock 데이터
const MOCK_REPORTS = [
    {
        id: 1,
        studyName: 'React 스터디',
        meetingTitle: '주간 회의 - React Hooks 심화',
        date: '2026-01-25',
        duration: '1시간 30분',
        participants: 5,
        summary: 'useState와 useEffect의 실행 순서, 클린업 함수의 동작 원리에 대해 논의했습니다.',
        keywords: ['React Hooks', 'useEffect', '클린업 함수', '의존성 배열'],
        highlights: [
            '의존성 배열이 빈 배열일 때 컴포넌트 마운트 시 한 번만 실행',
            'useEffect 클린업 함수는 언마운트 시 또는 다음 effect 실행 전에 호출',
        ],
    },
    {
        id: 2,
        studyName: 'TypeScript 스터디',
        meetingTitle: '제네릭과 유틸리티 타입',
        date: '2026-01-24',
        duration: '2시간',
        participants: 4,
        summary: 'TypeScript의 제네릭 문법과 Partial, Pick 같은 유틸리티 타입 활용법을 학습했습니다.',
        keywords: ['제네릭', 'Partial', 'Pick', '타입 추론'],
        highlights: [
            '제네릭을 사용하면 재사용 가능한 컴포넌트를 만들 수 있음',
            'Partial<T>는 모든 속성을 선택적으로 만듦',
        ],
    },
];

export const STTReportWidget: React.FC = () => {
    const [selectedReport, setSelectedReport] = useState(MOCK_REPORTS[0]);

    return (
        <WidgetContainer>
            {/* 헤더 - 공통 컴포넌트 사용 */}
            <WidgetHeader
                icon={FileText}
                iconColor="primary"
                title="STT 미팅 리포트"
                subtitle="최근 스터디 요약"
                maximizePath="/stt-report"
            />

            <div className="flex">
                {/* 좌측: 미팅 리스트 */}
                <div className="w-1/3 border-r border-gray-100 bg-gray-50/50">
                    {MOCK_REPORTS.map((report) => (
                        <button
                            key={report.id}
                            onClick={() => setSelectedReport(report)}
                            className={cn(
                                'w-full p-4 text-left transition-all border-b border-gray-100',
                                selectedReport.id === report.id
                                    ? 'bg-primary/10 border-l-4 border-l-primary'
                                    : 'hover:bg-gray-100'
                            )}
                        >
                            <h4 className="font-bold text-text-primary text-sm mb-0">
                                {report.studyName}
                            </h4>
                            <p className="text-xs text-text-secondary truncate">
                                {report.meetingTitle}
                            </p>
                            <div className="flex items-center gap-2 mt-2 text-xs text-text-tertiary">
                                <Calendar size={12} />
                                <span>{report.date}</span>
                            </div>
                        </button>
                    ))}
                </div>

                {/* 우측: 리포트 상세 */}
                <div className="flex-1 p-6">
                    <motion.div
                        key={selectedReport.id}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.3 }}
                    >
                        <h3 className="text-xl font-bold text-text-primary mb-0">
                            {selectedReport.meetingTitle}
                        </h3>
                        <div className="flex items-center gap-4 text-sm text-text-secondary mb-4">
                            <div className="flex items-center gap-1">
                                <Calendar size={14} />
                                <span>{selectedReport.date}</span>
                            </div>
                            <div className="flex items-center gap-1">
                                <Users size={14} />
                                <span>{selectedReport.participants}명 참여</span>
                            </div>
                            <span>{selectedReport.duration}</span>
                        </div>

                        {/* 요약 */}
                        <div className="bg-gray-50 rounded-xl p-4 mb-4">
                            <h4 className="font-bold text-text-primary mb-0 text-sm">📝 요약</h4>
                            <p className="text-text-secondary text-sm leading-relaxed">
                                {selectedReport.summary}
                            </p>
                        </div>

                        {/* 키워드 */}
                        <div className="mb-4">
                            <h4 className="font-bold text-text-primary mb-0 text-sm">🔑 핵심 키워드</h4>
                            <div className="flex flex-wrap gap-2">
                                {selectedReport.keywords.map((keyword, idx) => (
                                    <span
                                        key={idx}
                                        className="px-3 py-1 bg-primary/10 text-primary text-xs font-medium rounded-full"
                                    >
                                        {keyword}
                                    </span>
                                ))}
                            </div>
                        </div>

                        {/* 하이라이트 */}
                        <div>
                            <h4 className="font-bold text-text-primary mb-0 text-sm">💡 주요 내용</h4>
                            <ul className="space-y-2">
                                {selectedReport.highlights.map((highlight, idx) => (
                                    <li
                                        key={idx}
                                        className="flex items-start gap-2 text-sm text-text-secondary"
                                    >
                                        <ChevronRight size={16} className="text-primary flex-shrink-0 mt-0.5" />
                                        <span>{highlight}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </motion.div>
                </div>
            </div>
        </WidgetContainer>
    );
};
