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
        <div className="flex items-center justify-between mb-10">
            <div className="flex items-center gap-5">
                <ArrowButton
                    direction="left"
                    onClick={() => navigate('/quiz')}
                    size="lg"
                />
                <h1 className="text-3xl lg:text-4xl font-black text-text-primary tracking-tight">Commentle Quiz</h1>
            </div>

            <div className="relative group/info">
                <button
                    onClick={onInfoClick}
                    className="flex items-center justify-center w-10 h-10 rounded-full bg-transparent text-text-secondary hover:bg-primary hover:text-white transition-all"
                >
                    <Info size={20} />
                </button>

                {/* 엔진 정보 및 점수 산정 툴팁 */}
                <div className="absolute top-12 right-0 w-96 p-6 bg-white rounded-2xl shadow-xl border border-border-light opacity-0 invisible translate-y-[-10px] group-hover/info:opacity-100 group-hover/info:visible group-hover/info:translate-y-0 transition-all z-[60]">
                    <h4 className="text-lg font-black text-text-primary mb-3 flex items-center gap-2">
                        <span className="w-2 h-5 bg-primary rounded-full"></span>
                        AI 시맨틱 엔진 로직
                    </h4>
                    <p className="text-sm text-text-secondary leading-relaxed mb-5 bg-background-secondary/50 p-3 rounded-xl border border-border-light/50">
                        <strong>S-ROBERTA 임베딩</strong> 분석을 통해 단어의 의미적 좌표를 생성하고, 정답 단어와의 <strong>코사인 유사도</strong>를 측정하는 하이브리드 산정 방식입니다.
                    </p>

                    <ul className="space-y-2.5">
                        <li className="flex items-center justify-between text-sm text-text-secondary">
                            <div className="flex items-center gap-2.5">
                                <span className="w-3 h-3 rounded-full bg-quiz-success"></span>
                                <span className="font-bold">90~100</span>
                            </div>
                            <span className="text-xs font-bold text-quiz-success uppercase">Correct / Very Close</span>
                        </li>
                        <li className="flex items-center justify-between text-sm text-text-secondary">
                            <div className="flex items-center gap-2.5">
                                <span className="w-3 h-3 rounded-full bg-quiz-info"></span>
                                <span className="font-bold">75~89</span>
                            </div>
                            <span className="text-xs font-bold text-quiz-info uppercase">Close</span>
                        </li>
                        <li className="flex items-center justify-between text-sm text-text-secondary">
                            <div className="flex items-center gap-2.5">
                                <span className="w-3 h-3 rounded-full bg-quiz-focus"></span>
                                <span className="font-bold">50~74</span>
                            </div>
                            <span className="text-xs font-bold text-quiz-focus uppercase">Medium</span>
                        </li>
                        <li className="flex items-center justify-between text-sm text-text-secondary">
                            <div className="flex items-center gap-2.5">
                                <span className="w-3 h-3 rounded-full bg-quiz-warning"></span>
                                <span className="font-bold">25~49</span>
                            </div>
                            <span className="text-xs font-bold text-quiz-warning uppercase">Far</span>
                        </li>
                        <li className="flex items-center justify-between text-sm text-text-secondary">
                            <div className="flex items-center gap-2.5">
                                <span className="w-3 h-3 rounded-full bg-quiz-danger"></span>
                                <span className="font-bold">0~24</span>
                            </div>
                            <span className="text-xs font-bold text-quiz-danger uppercase">Very Far</span>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    );
};
