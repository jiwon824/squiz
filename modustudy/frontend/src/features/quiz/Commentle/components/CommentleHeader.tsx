import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Info } from 'lucide-react';
import { ArrowButton } from '@/shared/components';

interface CommentleHeaderProps {
    onInfoClick?: () => void;
}

export const CommentleHeader: React.FC<CommentleHeaderProps> = ({ onInfoClick }) => {
    const navigate = useNavigate();

    return (
        <div className="flex items-center justify-between mb-8">
            <div className="flex items-center gap-4">
                <ArrowButton
                    direction="left"
                    onClick={() => navigate('/quiz')}
                    size="md"
                />
                <h1 className="text-2xl font-black text-text-primary tracking-tight">Commentle Quiz</h1>
            </div>

            <div className="relative group/info">
                <button
                    onClick={onInfoClick}
                    className="flex items-center justify-center w-10 h-10 rounded-full border border-border-light bg-surface text-text-secondary hover:bg-primary hover:text-white hover:border-primary transition-all shadow-sm"
                >
                    <Info size={20} />
                </button>

                {/* 엔진 정보 및 점수 산정 툴팁 */}
                <div className="absolute top-12 right-0 w-80 p-5 bg-white rounded-2xl shadow-xl border border-border-light opacity-0 invisible translate-y-[-10px] group-hover/info:opacity-100 group-hover/info:visible group-hover/info:translate-y-0 transition-all z-[60]">
                    <h4 className="font-black text-text-primary mb-2 flex items-center gap-2">
                        <span className="w-1.5 h-4 bg-primary rounded-full"></span>
                        AI 시맨틱 엔진 로직
                    </h4>
                    <p className="text-[11px] text-text-secondary leading-relaxed mb-4 bg-background-secondary/50 p-2.5 rounded-xl border border-border-light/50">
                        <strong>S-ROBERTA 임베딩</strong> 분석을 통해 단어의 의미적 좌표를 생성하고, 정답 단어와의 <strong>코사인 유사도</strong>를 측정하는 하이브리드 산정 방식입니다.
                    </p>

                    <ul className="space-y-2">
                        <li className="flex items-center justify-between text-xs text-text-secondary">
                            <div className="flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rounded-full bg-[#34A853]"></span>
                                <span className="font-bold">95~100</span>
                            </div>
                            <span className="text-[10px] font-bold text-[#34A853] uppercase">Perfect (Correct)</span>
                        </li>
                        <li className="flex items-center justify-between text-xs text-text-secondary">
                            <div className="flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rounded-full bg-[#4ade80]"></span>
                                <span className="font-bold">85~94</span>
                            </div>
                            <span className="text-[10px] font-bold text-[#4ade80] uppercase">Very Close</span>
                        </li>
                        <li className="flex items-center justify-between text-xs text-text-secondary">
                            <div className="flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rounded-full bg-[#4285F4]"></span>
                                <span className="font-bold">70~84</span>
                            </div>
                            <span className="text-[10px] font-bold text-[#4285F4] uppercase">Close</span>
                        </li>
                        <li className="flex items-center justify-between text-xs text-text-secondary">
                            <div className="flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rounded-full bg-[#a855f7]"></span>
                                <span className="font-bold">50~69</span>
                            </div>
                            <span className="text-[10px] font-bold text-[#a855f7] uppercase">Medium</span>
                        </li>
                        <li className="flex items-center justify-between text-xs text-text-secondary">
                            <div className="flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rounded-full bg-[#FBBC04]"></span>
                                <span className="font-bold">30~49</span>
                            </div>
                            <span className="text-[10px] font-bold text-[#FBBC04] uppercase">Far</span>
                        </li>
                        <li className="flex items-center justify-between text-xs text-text-secondary">
                            <div className="flex items-center gap-2">
                                <span className="w-2.5 h-2.5 rounded-full bg-[#EA4335]"></span>
                                <span className="font-bold">0~29</span>
                            </div>
                            <span className="text-[10px] font-bold text-[#EA4335] uppercase">Very Far</span>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    );
};
