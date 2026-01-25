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
        <section className="bg-surface border border-border-light rounded-3xl p-6 shadow-sm mb-8">
            <h3 className="text-sm font-bold text-text-tertiary uppercase tracking-widest mb-4">단어 제출</h3>
            <form onSubmit={handleSubmit} className="relative">
                <input
                    ref={inputRef}
                    type="text"
                    value={word}
                    onChange={(e) => setWord(e.target.value)}
                    disabled={loading}
                    placeholder="정답이라고 생각하는 단어를 입력하세요..."
                    className="w-full bg-background-secondary border-2 border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 pr-16 text-lg font-semibold transition-all outline-none placeholder:text-text-tertiary/50"
                />
                <button
                    type="submit"
                    disabled={!word.trim() || loading}
                    className="absolute right-2 top-2 bottom-2 px-4 rounded-xl bg-primary text-white hover:bg-primary-dark disabled:bg-border-light disabled:text-text-tertiary transition-all flex items-center justify-center min-w-[56px]"
                >
                    {loading ? <Loader2 size={24} className="animate-spin" /> : <Send size={24} />}
                </button>
            </form>
            <p className="mt-3 text-xs text-text-tertiary">
                TIP: 한/영 대소문자 및 띄어쓰기 구분 없이 정답이 인정되며, 의미나 맥락이 유사할수록 점수가 높습니다.
            </p>
        </section>
    );
};
