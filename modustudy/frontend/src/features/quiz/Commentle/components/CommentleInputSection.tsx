import React, { useState, useRef, useEffect } from 'react';
import { Send } from 'lucide-react';
import { ButtonSpinner } from '@/shared/components/Spinner';
import { cn, classBuilder } from '@/shared/utils/cn';

interface CommentleInputSectionProps {
    onGuess: (word: string) => void;
    loading: boolean;
}

export const CommentleInputSection: React.FC<CommentleInputSectionProps> = ({ onGuess, loading }) => {
    const [word, setWord] = useState('');
    const inputRef = useRef<HTMLInputElement>(null);

    // 컴포넌트 마운트 시 및 로딩 완료 후 자동 포커스
    useEffect(() => {
        if (!loading) {
            inputRef.current?.focus();
        }
    }, [loading]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (word.trim() && !loading) {
            onGuess(word.trim());
            setWord('');
        }
    };

    return (
        <section className={cn(classBuilder.card('elevated'), 'p-4')}>
            <div className="flex items-center gap-2.5 mb-3">
                <div className="bg-primary/10 p-1.5 rounded-lg text-primary">
                    <Send size={16} />
                </div>
                <h3 className="text-base font-bold text-text-primary">단어 제출</h3>
            </div>

            <form onSubmit={handleSubmit} className="space-y-3">
                <div className="relative">
                    <input
                        ref={inputRef}
                        type="text"
                        value={word}
                        onChange={(e) => setWord(e.target.value)}
                        disabled={loading}
                        placeholder="정답이라고 생각하는 단어를 입력하세요..."
                        className="w-full bg-white border-2 border-primary/30 focus:border-primary focus:ring-4 focus:ring-primary/10 rounded-xl px-4 py-3 text-base font-bold text-text-primary transition-all outline-none placeholder:text-text-tertiary/40 placeholder:font-normal shadow-sm"
                    />
                </div>

                <button
                    type="submit"
                    disabled={!word.trim() || loading}
                    className="w-full bg-gradient-to-r from-primary to-primary-dark text-white font-bold text-sm py-3 rounded-xl hover:shadow-xl hover:shadow-primary/20 hover:scale-[1.02] active:scale-[0.98] disabled:from-gray-300 disabled:to-gray-400 disabled:cursor-not-allowed disabled:hover:scale-100 transition-all duration-200 flex items-center justify-center gap-2"
                >
                    {loading ? (
                        <>
                            <ButtonSpinner className="text-white" />
                            <span>분석 중...</span>
                        </>
                    ) : (
                        <>
                            <Send size={18} />
                            <span>제출하기</span>
                        </>
                    )}
                </button>
            </form>

            <div className="mt-3 p-3 bg-white/60 backdrop-blur-sm rounded-lg border border-primary/10">
                <div className="space-y-1">
                    <p className="text-xs font-bold text-primary">💡 TIP</p>
                    <p className="text-xs text-text-secondary font-medium leading-relaxed">
                        한/영 대소문자 및 띄어쓰기 구분 없이 정답이 인정되며, 의미나 맥락이 유사할수록 점수가 높습니다.
                    </p>
                </div>
            </div>
        </section>
    );
};
