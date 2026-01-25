// QuizGuessInput.jsx - 제출 후 자동 포커스 추가
import React, { useState, useRef, useEffect } from 'react';
import { Send } from 'lucide-react';

const QuizGuessInput = ({ onGuess, loading }) => {
    const [input, setInput] = useState('');
    const [isFlying, setIsFlying] = useState(false);
    const inputRef = useRef(null);

    // 컴포넌트 마운트 시 자동 포커스
    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    // 로딩 완료 후 자동 포커스
    useEffect(() => {
        if (!loading) {
            inputRef.current?.focus();
        }
    }, [loading]);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!input.trim() || loading) return;

        // 애니메이션 트리거
        setIsFlying(true);
        setTimeout(() => setIsFlying(false), 800);

        onGuess(input);
        setInput('');

        // 제출 후 즉시 포커스 (로딩 시작 전)
        setTimeout(() => inputRef.current?.focus(), 0);
    };

    return (
        <form onSubmit={handleSubmit} className="grid grid-cols-4 gap-2 w-full max-w-lg mx-auto guess-input-container">
            <input
                ref={inputRef}
                type="text"
                className="col-span-3 form-input"
                placeholder="정답을 입력하세요 (예: 스택)"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                disabled={loading}
                autoComplete="off"
                style={{ height: '48px', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '0 1rem' }}
            />
            <button
                type="submit"
                className="col-span-1 flex justify-center items-center gap-2 font-bold rounded-lg transition-colors"
                style={{
                    background: '#6366f1', // Indigo-500
                    color: 'white',
                    boxShadow: 'none', // 형광 제거
                    border: 'none',
                    height: '48px',
                    cursor: (loading || !input.trim()) ? 'not-allowed' : 'pointer',
                    opacity: (loading || !input.trim()) ? 0.7 : 1
                }}
                disabled={loading || !input.trim()}
            >
                <span className="hidden sm:inline">제출</span>
                <div className={`fly-icon ${isFlying ? 'flying' : ''}`}>
                    <Send size={18} />
                </div>
            </button>
        </form>
    );
};

export default QuizGuessInput;
