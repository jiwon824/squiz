import React, { useState, useEffect } from 'react';
import { Card } from '@/shared/components';
import { Box } from 'lucide-react';

interface LegoActivityGraphProps {
    data: number[]; // 0~4 사이의 활동 지수 배열
}

type LegoTheme = 'classic' | 'premium' | 'pastel' | 'ocean';

interface ThemeConfig {
    levels: string[];
    stud: string;
}

const THEMES: Record<LegoTheme, ThemeConfig> = {
    classic: {
        levels: ['#E5E7EB', '#FCD34D', '#F97316', '#EF4444', '#B91C1C'], // 그레이 -> 옐로우 -> 오렌지 -> 레드 -> 다크레드
        stud: 'rgba(255,255,255,0.3)'
    },
    premium: {
        levels: ['#334155', '#94A3B8', '#CBD5E1', '#F1F5F9', '#FFFFFF'], // 다크 -> 실버 -> 화이트 (다크모드 느낌)
        stud: 'rgba(0,0,0,0.1)'
    },
    pastel: {
        levels: ['#F3F4F6', '#A5B4FC', '#818CF8', '#6366F1', '#4F46E5'], // 연보라 -> 진보라
        stud: 'rgba(255,255,255,0.4)'
    },
    ocean: {
        levels: ['#F0F9FF', '#BAE6FD', '#7DD3FC', '#38BDF8', '#0EA5E9'], // 연하늘 -> 진파랑
        stud: 'rgba(255,255,255,0.3)'
    }
};

export const LegoActivityGraph: React.FC<LegoActivityGraphProps> = ({ data }) => {
    const [theme, setTheme] = useState<LegoTheme>('classic');
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
    }, []);

    const config = THEMES[theme];

    return (
        <Card variant="flat" className="lego-graph-card bg-white/50 backdrop-blur-xl border border-gray-100 p-6 rounded-[32px] overflow-hidden">
            <div className="flex justify-between items-center mb-6">
                <div className="flex items-center gap-3">
                    <div className="p-2 bg-blue-50 rounded-xl text-blue-600">
                        <Box size={20} />
                    </div>
                    <div>
                        <h3 className="text-lg font-bold text-gray-800 tracking-tight">나의 활동 지수</h3>
                        <p className="text-xs text-gray-400">최근 한 달간 쌓아올린 학습 블록</p>
                    </div>
                </div>

                <div className="flex gap-2">
                    {(Object.keys(THEMES) as LegoTheme[]).map((t) => (
                        <button
                            key={t}
                            onClick={() => setTheme(t)}
                            className={`w-6 h-6 rounded-full border-2 transition-all ${theme === t ? 'border-blue-500 scale-110 shadow-sm' : 'border-transparent opacity-60 hover:opacity-100'
                                }`}
                            style={{ background: THEMES[t].levels[3] }}
                            title={`${t} 테마`}
                        />
                    ))}
                </div>
            </div>

            <div className="lego-grid-container relative">
                <div className="lego-grid flex flex-wrap gap-2 justify-center">
                    {data.map((level, i) => (
                        <div
                            key={i}
                            className={`lego-brick group relative w-10 h-10 rounded-lg transition-all duration-500 ${mounted ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'
                                }`}
                            style={{
                                transitionDelay: `${i * 30}ms`,
                                backgroundColor: config.levels[level],
                                boxShadow: level > 0
                                    ? `inset -4px -4px 0 rgba(0,0,0,0.1), inset 4px 4px 0 rgba(255,255,255,0.2), 0 4px 6px rgba(0,0,0,0.05)`
                                    : 'none',
                                transform: level > 0 ? `translateY(-${level * 2}px)` : 'none'
                            }}
                        >
                            {/* 레고 돌기 (Stud) */}
                            {level > 0 && (
                                <div
                                    className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-4 h-4 rounded-full"
                                    style={{
                                        backgroundColor: config.stud,
                                        boxShadow: 'inset 1px 1px 2px rgba(255,255,255,0.2), 1px 1px 2px rgba(0,0,0,0.1)'
                                    }}
                                />
                            )}

                            {/* 툴팁 */}
                            <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-[10px] px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10 pointer-events-none">
                                활동 레벨: {level}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <div className="mt-8 flex justify-center items-center gap-6 text-[10px] text-gray-400 font-bold uppercase tracking-widest">
                <div className="flex items-center gap-2">
                    <span>Low</span>
                    <div className="flex gap-1">
                        {config.levels.map((c, i) => (
                            <div key={i} className="w-3 h-3 rounded-sm" style={{ background: c }} />
                        ))}
                    </div>
                    <span>High</span>
                </div>
            </div>

            <style>{`
                .lego-brick {
                    transform-style: preserve-3d;
                }
                .lego-brick:hover {
                    transform: scale(1.1) rotateX(10deg) rotateY(10deg) !important;
                    z-index: 2;
                }
            `}</style>
        </Card>
    );
};
