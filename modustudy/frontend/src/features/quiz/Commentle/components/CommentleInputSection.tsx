import React, { useState, useRef, useEffect } from 'react';
import { Send, Loader2 } from 'lucide-react';

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
        <section className="bg-gradient-to-br from-primary/5 to-secondary/5 border-2 border-primary/20 rounded-3xl p-8">
            <div className="flex items-center gap-3 mb-6">
                <div className="bg-primary/10 p-2 rounded-xl text-primary">
                    <Send size={20} />
                </div>
                <h3 className="text-xl font-bold text-text-primary">단어 제출</h3>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="relative">
                    <input
                        ref={inputRef}
                        type="text"
                        value={word}
                        onChange={(e) => setWord(e.target.value)}
                        disabled={loading}
                        placeholder="정답이라고 생각하는 단어를 입력하세요..."
                        className="w-full bg-white border-3 border-primary/30 focus:border-primary focus:ring-4 focus:ring-primary/10 rounded-2xl px-6 py-5 text-xl font-bold text-text-primary transition-all outline-none placeholder:text-text-tertiary/40 placeholder:font-normal shadow-sm"
                    />
                </div>

                <button
                    type="submit"
                    disabled={!word.trim() || loading}
                    className="w-full bg-gradient-to-r from-primary to-primary-dark text-white font-bold text-lg py-4 rounded-2xl hover:shadow-xl hover:shadow-primary/20 hover:scale-[1.02] active:scale-[0.98] disabled:from-gray-300 disabled:to-gray-400 disabled:cursor-not-allowed disabled:hover:scale-100 transition-all duration-200 flex items-center justify-center gap-3"
                >
                    {loading ? (
                        <>
                            <Loader2 size={24} className="animate-spin" />
                            <span>분석 중...</span>
                        </>
                    ) : (
                        <>
                            <Send size={24} />
                            <span>제출하기</span>
                        </>
                    )}
                </button>
            </form>

            <div className="mt-6 p-5 bg-white/60 backdrop-blur-sm rounded-xl border border-primary/10">
                <div className="space-y-2">
                    <p className="text-base font-bold text-primary">💡 TIP</p>
                    <p className="text-base text-text-secondary font-medium leading-relaxed">
                        한/영 대소문자 및 띄어쓰기 구분 없이 정답이 인정되며, 의미나 맥락이 유사할수록 점수가 높습니다.
                    </p>
                </div>
            </div>
        </section>
    );
};
