import React, { useState, useMemo, useEffect } from 'react';
import { X, Calendar, CheckCircle, Clock } from 'lucide-react';

// 커리큘럼 정류장 타입
export interface CurriculumStop {
    session: number;
    title: string;
    date: string;
    description?: string;
    isCompleted?: boolean;
    scheduledAt?: string; // ISO 날짜 문자열 (시간 기반 상태 판단용)
    durationMinutes?: number; // 세션 진행 시간 (분)
    status?: string; // 백엔드 세션 상태 (SCHEDULED, IN_PROGRESS, COMPLETED)
}

interface CurriculumRoadmapProps {
    curriculum: CurriculumStop[];
    currentSession?: number; // 현재 진행 중인 회차
    onStopClick?: (session: number, stop: CurriculumStop) => void;
}

/**
 * CurriculumRoadmap - S자 도로 스타일의 커리큘럼 시각화 컴포넌트
 *
 * 자동차가 현재 진행 상황을 나타내며, 각 회차는 정류장으로 표시됩니다.
 */
export const CurriculumRoadmap: React.FC<CurriculumRoadmapProps> = ({
    curriculum,
    currentSession: _currentSession = 1,
    onStopClick
}) => {
    const [selectedStop, setSelectedStop] = useState<CurriculumStop | null>(null);
    const [currentTime, setCurrentTime] = useState(() => new Date());
    const totalStops = curriculum.length;

    // 실시간 상태 업데이트를 위한 타이머 (30초마다 체크)
    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentTime(new Date());
        }, 30000); // 30초마다 업데이트

        return () => clearInterval(timer);
    }, []);

    // SVG 크기 설정 (카드 크기에 맞게 여유있게)
    const svgWidth = 1100;
    const svgHeight = Math.max(450, totalStops * 140);
    const roadWidth = 55;

    // S자 도로 경로 생성
    const generateRoadPath = () => {
        const segments: string[] = [];
        const startX = 300;
        const endX = svgWidth - 300;
        const segmentHeight = 140;

        segments.push(`M ${startX} 70`);

        for (let i = 0; i < totalStops; i++) {
            const y = 70 + i * segmentHeight;
            const nextY = y + segmentHeight;
            const isLeftToRight = i % 2 === 0;

            if (i < totalStops - 1) {
                if (isLeftToRight) {
                    segments.push(`L ${endX - 60} ${y}`);
                    segments.push(`Q ${endX} ${y} ${endX} ${y + 70}`);
                    segments.push(`L ${endX} ${nextY - 70}`);
                    segments.push(`Q ${endX} ${nextY} ${endX - 60} ${nextY}`);
                } else {
                    segments.push(`L ${startX + 60} ${y}`);
                    segments.push(`Q ${startX} ${y} ${startX} ${y + 70}`);
                    segments.push(`L ${startX} ${nextY - 70}`);
                    segments.push(`Q ${startX} ${nextY} ${startX + 60} ${nextY}`);
                }
            } else {
                if (isLeftToRight) {
                    segments.push(`L ${endX} ${y}`);
                } else {
                    segments.push(`L ${startX} ${y}`);
                }
            }
        }

        return segments.join(' ');
    };

    // 각 정류장의 위치 계산
    const getStopPosition = (index: number) => {
        const segmentHeight = 140;
        const startX = 300;
        const endX = svgWidth - 300;
        const y = 70 + index * segmentHeight;
        const isLeftToRight = index % 2 === 0;

        const x = isLeftToRight ? startX + 50 : endX - 50;

        return { x, y };
    };

    const roadPath = generateRoadPath();

    // 각 정류장의 시간 기반 상태 계산
    const stopsWithStatus = useMemo(() => {
        return curriculum.map((stop, index) => {
            // 완료 상태 계산
            const isCompleted = (() => {
                if (stop.status === 'COMPLETED') return true;
                if (stop.isCompleted) return true;

                if (stop.status === 'IN_PROGRESS' && stop.scheduledAt) {
                    const now = new Date();
                    const scheduled = new Date(stop.scheduledAt);
                    const durationMs = (stop.durationMinutes || 60) * 60 * 1000;
                    const endTime = new Date(scheduled.getTime() + durationMs);
                    if (now >= endTime) return true;
                }

                if (stop.scheduledAt && stop.status !== 'IN_PROGRESS') {
                    const now = new Date();
                    const scheduled = new Date(stop.scheduledAt);
                    const durationMs = (stop.durationMinutes || 60) * 60 * 1000;
                    const endTime = new Date(scheduled.getTime() + durationMs);

                    const today = new Date();
                    today.setHours(0, 0, 0, 0);
                    const scheduledDate = new Date(scheduled);
                    scheduledDate.setHours(0, 0, 0, 0);
                    if (scheduledDate < today) return true;
                    if (now >= endTime) return true;
                }

                return false;
            })();

            // 진행중 상태 계산
            const isInProgress = (() => {
                if (isCompleted) return false;

                if (stop.status === 'IN_PROGRESS' && stop.scheduledAt) {
                    const now = new Date();
                    const scheduled = new Date(stop.scheduledAt);
                    const durationMs = (stop.durationMinutes || 60) * 60 * 1000;
                    const endTime = new Date(scheduled.getTime() + durationMs);
                    if (now >= scheduled && now < endTime) return true;
                }

                // SCHEDULED 상태에서 시간이 시작 ~ 종료 사이면 진행중
                if (stop.scheduledAt && stop.status === 'SCHEDULED') {
                    const now = new Date();
                    const scheduled = new Date(stop.scheduledAt);
                    const durationMs = (stop.durationMinutes || 60) * 60 * 1000;
                    const endTime = new Date(scheduled.getTime() + durationMs);
                    if (now >= scheduled && now < endTime) return true;
                }

                return false;
            })();

            return { ...stop, index, isCompleted, isInProgress };
        });
    }, [curriculum, currentTime]); // currentTime 추가로 실시간 업데이트

    // 자동차 진행률 계산 (시간 기반)
    const { carProgress, activeSessionIndex } = useMemo(() => {
        if (totalStops <= 1) return { carProgress: 3, activeSessionIndex: 0 };

        const segmentPercent = 100 / totalStops;

        // 진행중인 세션 찾기
        const inProgressStop = stopsWithStatus.find(s => s.isInProgress);
        if (inProgressStop) {
            // 진행중이면 해당 정류장과 다음 정류장 사이 (50% 위치)
            const progress = inProgressStop.index * segmentPercent + segmentPercent * 0.5;
            return {
                carProgress: Math.max(2, Math.min(98, progress)),
                activeSessionIndex: inProgressStop.index
            };
        }

        // 완료된 세션 개수 세기
        const completedCount = stopsWithStatus.filter(s => s.isCompleted).length;

        if (completedCount === totalStops) {
            // 모두 완료면 마지막 정류장 끝
            const progress = (totalStops - 1) * segmentPercent + segmentPercent * 0.85;
            return {
                carProgress: Math.max(2, Math.min(98, progress)),
                activeSessionIndex: totalStops - 1
            };
        }

        // 첫 번째 미완료 세션 찾기
        const firstIncomplete = stopsWithStatus.find(s => !s.isCompleted);
        if (firstIncomplete) {
            // 미완료 세션 시작 위치
            const progress = firstIncomplete.index * segmentPercent + segmentPercent * 0.15;
            return {
                carProgress: Math.max(2, Math.min(98, progress)),
                activeSessionIndex: firstIncomplete.index
            };
        }

        return { carProgress: 3, activeSessionIndex: 0 };
    }, [stopsWithStatus, totalStops]);

    // 현재 세션이 역방향인지 확인
    const isReversed = activeSessionIndex % 2 !== 0;

    // 정류장 클릭 핸들러
    const handleStopClick = (stop: CurriculumStop) => {
        setSelectedStop(stop);
        onStopClick?.(stop.session, stop);
    };

    return (
        <div className="relative w-full overflow-x-auto">
            <svg
                width={svgWidth}
                height={svgHeight}
                viewBox={`0 0 ${svgWidth} ${svgHeight}`}
                className="mx-auto"
            >
                {/* 배경 나무들 - 도로 양쪽에 배치 */}
                {Array.from({ length: Math.ceil(svgHeight / 120) }).map((_, i) => (
                    <React.Fragment key={`trees-${i}`}>
                        {/* 왼쪽 나무들 */}
                        <g transform={`translate(${80 + (i % 3) * 30}, ${60 + i * 120})`}>
                            {/* 나무 그림자 */}
                            <ellipse cx={18} cy={52} rx={12} ry={4} fill="#000" opacity={0.1} />
                            {/* 나무 줄기 */}
                            <rect x={14} y={30} width={8} height={22} rx={2} fill="#92400e" />
                            {/* 나무 잎 (둥근 형태) */}
                            <circle cx={18} cy={18} r={18} fill="#22c55e" opacity={0.7} />
                            <circle cx={10} cy={24} r={12} fill="#16a34a" opacity={0.6} />
                            <circle cx={26} cy={24} r={12} fill="#16a34a" opacity={0.6} />
                        </g>
                        <g transform={`translate(${140 + (i % 2) * 40}, ${100 + i * 120})`}>
                            <ellipse cx={15} cy={42} rx={10} ry={3} fill="#000" opacity={0.1} />
                            <rect x={12} y={25} width={6} height={17} rx={1} fill="#a16207" />
                            <circle cx={15} cy={14} r={14} fill="#4ade80" opacity={0.6} />
                            <circle cx={8} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                            <circle cx={22} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                        </g>

                        {/* 오른쪽 나무들 */}
                        <g transform={`translate(${svgWidth - 100 - (i % 3) * 30}, ${80 + i * 120})`}>
                            <ellipse cx={18} cy={52} rx={12} ry={4} fill="#000" opacity={0.1} />
                            <rect x={14} y={30} width={8} height={22} rx={2} fill="#92400e" />
                            <circle cx={18} cy={18} r={18} fill="#22c55e" opacity={0.7} />
                            <circle cx={10} cy={24} r={12} fill="#16a34a" opacity={0.6} />
                            <circle cx={26} cy={24} r={12} fill="#16a34a" opacity={0.6} />
                        </g>
                        <g transform={`translate(${svgWidth - 160 - (i % 2) * 40}, ${40 + i * 120})`}>
                            <ellipse cx={15} cy={42} rx={10} ry={3} fill="#000" opacity={0.1} />
                            <rect x={12} y={25} width={6} height={17} rx={1} fill="#a16207" />
                            <circle cx={15} cy={14} r={14} fill="#4ade80" opacity={0.6} />
                            <circle cx={8} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                            <circle cx={22} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                        </g>
                    </React.Fragment>
                ))}

                {/* 도로 중간 장식 - 커브 안쪽 빈 공간 */}
                {Array.from({ length: totalStops }).map((_, i) => {
                    const segmentY = 70 + i * 140;
                    const isEvenCurve = i % 2 === 0;

                    // 마지막 세션이면 커브가 없으므로 스킵
                    if (i >= totalStops - 1) return null;

                    return (
                        <React.Fragment key={`road-middle-${i}`}>
                            {/* 커브 안쪽 나무 */}
                            {isEvenCurve ? (
                                // 짝수(우측 커브) - 우측 안쪽 공간
                                <>
                                    <g transform={`translate(${svgWidth - 220}, ${segmentY + 70})`}>
                                        <ellipse cx={15} cy={42} rx={10} ry={3} fill="#000" opacity={0.1} />
                                        <rect x={12} y={25} width={6} height={17} rx={1} fill="#a16207" />
                                        <circle cx={15} cy={14} r={14} fill="#4ade80" opacity={0.65} />
                                        <circle cx={8} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                                        <circle cx={22} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                                    </g>
                                    {/* 돌 */}
                                    <g transform={`translate(${svgWidth - 250}, ${segmentY + 110})`}>
                                        <ellipse cx={12} cy={18} rx={14} ry={6} fill="#6b7280" />
                                        <ellipse cx={12} cy={16} rx={12} ry={5} fill="#9ca3af" />
                                        <ellipse cx={8} cy={14} rx={4} ry={2} fill="#d1d5db" opacity={0.5} />
                                    </g>
                                    <g transform={`translate(${svgWidth - 195}, ${segmentY + 95})`}>
                                        <ellipse cx={8} cy={12} rx={10} ry={4} fill="#6b7280" />
                                        <ellipse cx={8} cy={10} rx={8} ry={3} fill="#9ca3af" />
                                    </g>
                                </>
                            ) : (
                                // 홀수(좌측 커브) - 좌측 안쪽 공간
                                <>
                                    <g transform={`translate(${220}, ${segmentY + 70})`}>
                                        <ellipse cx={15} cy={42} rx={10} ry={3} fill="#000" opacity={0.1} />
                                        <rect x={12} y={25} width={6} height={17} rx={1} fill="#a16207" />
                                        <circle cx={15} cy={14} r={14} fill="#4ade80" opacity={0.65} />
                                        <circle cx={8} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                                        <circle cx={22} cy={18} r={9} fill="#22c55e" opacity={0.5} />
                                    </g>
                                    {/* 돌 */}
                                    <g transform={`translate(${250}, ${segmentY + 110})`}>
                                        <ellipse cx={12} cy={18} rx={14} ry={6} fill="#6b7280" />
                                        <ellipse cx={12} cy={16} rx={12} ry={5} fill="#9ca3af" />
                                        <ellipse cx={8} cy={14} rx={4} ry={2} fill="#d1d5db" opacity={0.5} />
                                    </g>
                                    <g transform={`translate(${195}, ${segmentY + 95})`}>
                                        <ellipse cx={8} cy={12} rx={10} ry={4} fill="#6b7280" />
                                        <ellipse cx={8} cy={10} rx={8} ry={3} fill="#9ca3af" />
                                    </g>
                                </>
                            )}
                        </React.Fragment>
                    );
                })}

                {/* 도로 그림자 */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#1f2937"
                    strokeWidth={roadWidth + 12}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    opacity={0.15}
                    transform="translate(5, 5)"
                />

                {/* 도로 본체 */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#374151"
                    strokeWidth={roadWidth}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                />

                {/* 도로 중앙선 (점선) */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#fbbf24"
                    strokeWidth={4}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeDasharray="25, 18"
                />

                {/* 도로 가장자리 선 */}
                <path
                    d={roadPath}
                    fill="none"
                    stroke="#f3f4f6"
                    strokeWidth={roadWidth}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    opacity={0.25}
                    strokeDasharray="6, 6"
                />

                {/* 정류장들 - stopsWithStatus 사용으로 실시간 업데이트 */}
                {stopsWithStatus.map((stopWithStatus) => {
                    const { index, isCompleted, isInProgress: isCurrent, ...stop } = stopWithStatus;
                    const pos = getStopPosition(index);
                    const isLeftSide = index % 2 !== 0;

                    return (
                        <g
                            key={stop.session}
                            className="cursor-pointer transition-transform hover:scale-105"
                            onClick={() => handleStopClick(stop)}
                            style={{ transformOrigin: `${pos.x}px ${pos.y}px` }}
                        >
                            {/* 정류장 연결선 - 완료=녹색, 진행중=파란색, 예정=회색 */}
                            <line
                                x1={pos.x}
                                y1={pos.y}
                                x2={isLeftSide ? pos.x + 100 : pos.x - 100}
                                y2={pos.y - 35}
                                stroke={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#9ca3af'}
                                strokeWidth={3}
                                strokeDasharray={isCompleted ? 'none' : '5, 5'}
                            />

                            {/* 정류장 핀 (확대) */}
                            <g transform={`translate(${pos.x}, ${pos.y - 18})`}>
                                {/* 핀 그림자 */}
                                <ellipse cx={0} cy={22} rx={10} ry={5} fill="#000" opacity={0.2} />

                                {/* 핀 본체 - 완료=녹색, 진행중=파란색, 예정=회색 */}
                                <path
                                    d="M0,-25 C-15,-25 -18,-10 -18,0 C-18,15 0,32 0,32 C0,32 18,15 18,0 C18,-10 15,-25 0,-25"
                                    fill={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#9ca3af'}
                                    className="drop-shadow-lg"
                                />

                                {/* 핀 내부 원 */}
                                <circle
                                    cx={0}
                                    cy={-6}
                                    r={10}
                                    fill="white"
                                />

                                {/* 회차 번호 - 완료=녹색, 진행중=파란색, 예정=회색 */}
                                <text
                                    x={0}
                                    y={-2}
                                    textAnchor="middle"
                                    fontSize={13}
                                    fontWeight="bold"
                                    fill={isCompleted ? '#22c55e' : isCurrent ? '#2563eb' : '#6b7280'}
                                >
                                    {stop.session}
                                </text>
                            </g>

                            {/* 정류장 정보 카드 (확대) */}
                            <g transform={`translate(${isLeftSide ? pos.x + 110 : pos.x - 280}, ${pos.y - 65})`}>
                                {/* 카드 배경 - 완료=녹색, 진행중=파란색, 예정=회색 */}
                                <rect
                                    x={0}
                                    y={0}
                                    width={170}
                                    height={65}
                                    rx={10}
                                    fill="white"
                                    stroke={isCompleted ? '#22c55e' : isCurrent ? '#3b82f6' : '#9ca3af'}
                                    strokeWidth={2}
                                    className="drop-shadow-md"
                                />

                                {/* 제목 (확대) */}
                                <text
                                    x={14}
                                    y={26}
                                    fontSize={16}
                                    fontWeight="bold"
                                    fill="#1f2937"
                                >
                                    {stop.title.length > 10 ? stop.title.slice(0, 10) + '...' : stop.title}
                                </text>

                                {/* 날짜 (확대) */}
                                <text
                                    x={14}
                                    y={50}
                                    fontSize={14}
                                    fill="#6b7280"
                                >
                                    {stop.date}
                                </text>

                                {/* 완료 체크 / 클릭 힌트 - 완료=녹색, 진행중=파란색, 예정=회색 */}
                                {isCompleted ? (
                                    <g transform="translate(145, 14)">
                                        <circle cx={0} cy={0} r={12} fill="#22c55e" />
                                        <path
                                            d="M-6,0 L-2,5 L6,-5"
                                            stroke="white"
                                            strokeWidth={2.5}
                                            fill="none"
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                        />
                                    </g>
                                ) : (
                                    <text
                                        x={156}
                                        y={54}
                                        fontSize={11}
                                        fill={isCurrent ? '#2563eb' : '#6b7280'}
                                        textAnchor="end"
                                    >
                                        클릭
                                    </text>
                                )}
                            </g>
                        </g>
                    );
                })}

                {/* 자동차 */}
                <g
                    className="car-on-road"
                    style={{
                        offsetPath: `path("${roadPath}")`,
                        offsetDistance: `${carProgress}%`,
                        offsetRotate: '0deg',
                        transition: 'offset-distance 0.6s ease-in-out',
                    } as React.CSSProperties}
                >
                    <g transform={`scale(${isReversed ? -0.9 : 0.9}, 0.9) translate(${isReversed ? 22 : -22}, -14)`}>
                        {/* 차체 그림자 */}
                        <ellipse cx={25} cy={30} rx={24} ry={7} fill="#000" opacity={0.25} />

                        {/* 차체 */}
                        <rect x={5} y={8} width={42} height={20} rx={5} fill="#ef4444" />
                        <rect x={12} y={1} width={28} height={14} rx={4} fill="#ef4444" />

                        {/* 창문 */}
                        <rect x={14} y={3} width={11} height={9} rx={2} fill="#bfdbfe" />
                        <rect x={27} y={3} width={11} height={9} rx={2} fill="#bfdbfe" />

                        {/* 헤드라이트 */}
                        <rect x={44} y={12} width={5} height={5} rx={1} fill="#fef08a" />
                        <rect x={44} y={19} width={5} height={5} rx={1} fill="#fef08a" />

                        {/* 테일라이트 */}
                        <rect x={3} y={12} width={4} height={12} rx={1} fill="#fca5a5" />

                        {/* 바퀴 */}
                        <circle cx={16} cy={28} r={6} fill="#1f2937" />
                        <circle cx={16} cy={28} r={2.5} fill="#6b7280" />
                        <circle cx={36} cy={28} r={6} fill="#1f2937" />
                        <circle cx={36} cy={28} r={2.5} fill="#6b7280" />
                    </g>
                </g>
            </svg>

            {/* 상세 모달 - stopsWithStatus에서 실시간 상태 가져오기 */}
            {selectedStop && (() => {
                // stopsWithStatus에서 현재 선택된 세션의 실시간 상태 가져오기
                const currentStatus = stopsWithStatus.find(s => s.session === selectedStop.session);
                const isCompleted = currentStatus?.isCompleted ?? false;
                const isInProgress = currentStatus?.isInProgress ?? false;

                // 상태에 따른 배경색 클래스 - 완료=녹색, 진행중=파란색, 예정=회색
                const headerBgClass = isCompleted ? 'bg-green-500' : isInProgress ? 'bg-blue-500' : 'bg-gray-500';

                return (
                <div
                    className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
                    onClick={() => setSelectedStop(null)}
                >
                    <div
                        className="bg-white rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl animate-fadeIn"
                        onClick={e => e.stopPropagation()}
                    >
                        {/* 모달 헤더 */}
                        <div className="flex items-center justify-between mb-4">
                            <div className="flex items-center gap-3">
                                <div className={`w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg ${headerBgClass}`}>
                                    {selectedStop.session}
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-gray-900">
                                        {selectedStop.title}
                                    </h3>
                                    <p className="text-sm text-gray-500 flex items-center gap-1">
                                        <Calendar size={14} />
                                        {selectedStop.date}
                                    </p>
                                </div>
                            </div>
                            <button
                                onClick={() => setSelectedStop(null)}
                                className="p-2 hover:bg-gray-100 rounded-full transition-colors"
                            >
                                <X size={20} className="text-gray-500" />
                            </button>
                        </div>

                        {/* 상태 배지 - 완료=녹색, 진행중=파란색, 예정=회색 */}
                        <div className="mb-4">
                            {isCompleted ? (
                                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-green-50 text-green-700 rounded-full text-sm font-medium">
                                    <CheckCircle size={16} />
                                    완료됨
                                </span>
                            ) : isInProgress ? (
                                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
                                    <Clock size={16} />
                                    진행중
                                </span>
                            ) : (
                                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 text-gray-700 rounded-full text-sm font-medium">
                                    <Calendar size={16} />
                                    예정됨
                                </span>
                            )}
                        </div>

                        {/* 설명 */}
                        <div className="bg-gray-50 rounded-xl p-4">
                            <h4 className="text-sm font-semibold text-gray-700 mb-2">회차 내용</h4>
                            <p className="text-gray-600 leading-relaxed whitespace-pre-wrap">
                                {selectedStop.description || '등록된 내용이 없습니다.'}
                            </p>
                        </div>

                        {/* 닫기 버튼 */}
                        <button
                            onClick={() => setSelectedStop(null)}
                            className="w-full mt-4 py-3 bg-gray-900 text-white rounded-xl font-medium hover:bg-gray-800 transition-colors"
                        >
                            닫기
                        </button>
                    </div>
                </div>
                );
            })()}

            {/* CSS for offset-path */}
            <style>{`
                .car-on-road {
                    position: absolute;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: scale(0.95); }
                    to { opacity: 1; transform: scale(1); }
                }
                .animate-fadeIn {
                    animation: fadeIn 0.2s ease-out;
                }
            `}</style>
        </div>
    );
};

export default CurriculumRoadmap;
