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

                {/* Tooltip */}
                <div className="absolute top-12 right-0 w-72 p-4 bg-white rounded-2xl shadow-xl border border-border-light opacity-0 invisible translate-y-[-10px] group-hover/info:opacity-100 group-hover/info:visible group-hover/info:translate-y-0 transition-all z-[60]">
                    <h4 className="font-bold text-text-primary mb-2">🎯 점수 산정 방식</h4>
                    <p className="text-sm text-text-secondary leading-relaxed mb-3">
                        AI가 정답 단어와 입력 단어의 <strong>의미적 유사도</strong>를 분석합니다.
                    </p>
                    <ul className="space-y-2">
                        <li className="flex items-center gap-2 text-xs text-text-secondary">
                            <span className="w-2.5 h-2.5 rounded-full bg-[#22c55e]"></span>
                            <strong>90~100:</strong> 정답!
                        </li>
                        <li className="flex items-center gap-2 text-xs text-text-secondary">
                            <span className="w-2.5 h-2.5 rounded-full bg-[#3b82f6]"></span>
                            <strong>75~89:</strong> 아주 가까움
                        </li>
                        <li className="flex items-center gap-2 text-xs text-text-secondary">
                            <span className="w-2.5 h-2.5 rounded-full bg-[#a855f7]"></span>
                            <strong>50~74:</strong> 좋은 방향
                        </li>
                        <li className="flex items-center gap-2 text-xs text-text-secondary">
                            <span className="w-2.5 h-2.5 rounded-full bg-[#f59e0b]"></span>
                            <strong>25~49:</strong> 보통
                        </li>
                        <li className="flex items-center gap-2 text-xs text-text-secondary">
                            <span className="w-2.5 h-2.5 rounded-full bg-[#ef4444]"></span>
                            <strong>0~24:</strong> 멀어요
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    );
};
