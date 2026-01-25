// QuizInputlist.jsx - 2열 그리드 + 정렬 기능
import React, { useState, useMemo } from 'react';
import { List, ArrowDownWideNarrow, Clock } from 'lucide-react';

const QuizInputList = ({ guesses }) => {
    // 정렬 상태: 'recent' (입력순) 또는 'score' (유사도 높은 순)
    const [sortBy, setSortBy] = useState('recent');

    // 점수별 색상 매핑
    const getScoreColor = (score) => {
        if (score >= 90) return '#22c55e'; // 초록
        if (score >= 75) return '#3b82f6'; // 파랑
        if (score >= 50) return '#a855f7'; // 보라
        if (score >= 25) return '#f59e0b'; // 주황
        return '#ef4444'; // 빨강
    };

    // 정렬된 목록
    const sortedGuesses = useMemo(() => {
        if (!guesses || guesses.length === 0) return [];

        if (sortBy === 'score') {
            // 유사도 높은 순
            return [...guesses].sort((a, b) => b.score - a.score);
        }
        // 입력순 (최신순 - 기본)
        return guesses;
    }, [guesses, sortBy]);

    if (!guesses || guesses.length === 0) {
        return (
            <div className="guess-list-container">
                <div className="text-center py-8 text-gray-400">
                    <span className="text-4xl block mb-2">⌨️</span>
                    <p className="text-sm">단어를 입력하여 유사도를 확인해보세요!<br />100점에 가까울수록 정답입니다.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="guess-list-container">
            {/* 헤더 */}
            <div className="guess-list-header">
                <h3>
                    <List size={16} />
                    입력 기록
                </h3>

                {/* 정렬 토글 버튼 */}
                <div className="sort-toggle">
                    <button
                        className={`sort-btn ${sortBy === 'recent' ? 'active' : ''}`}
                        onClick={() => setSortBy('recent')}
                        title="입력순 (최신)"
                    >
                        <Clock size={14} />
                        <span>입력순</span>
                    </button>
                    <button
                        className={`sort-btn ${sortBy === 'score' ? 'active' : ''}`}
                        onClick={() => setSortBy('score')}
                        title="유사도 높은 순"
                    >
                        <ArrowDownWideNarrow size={14} />
                        <span>유사도순</span>
                    </button>
                </div>
            </div>

            {/* 2열 그리드 */}
            <div className="guess-grid custom-scrollbar">
                {sortedGuesses.map((guess, index) => {
                    const color = getScoreColor(guess.score);
                    // 입력순일 때는 원래 순서, 유사도순일 때는 정렬된 순서
                    const displayNum = sortBy === 'recent'
                        ? guesses.length - guesses.indexOf(guess)
                        : index + 1;

                    return (
                        <div key={guess.id || index} className="guess-item-compact">
                            <div className="item-header">
                                <span className="item-num">
                                    {sortBy === 'recent' ? `#${displayNum}` : `${displayNum}위`}
                                </span>
                                <span className="score" style={{ color }}>
                                    {typeof guess.score === 'number' ? guess.score.toFixed(1) : guess.score}
                                </span>
                            </div>
                            <div className="word">{guess.word}</div>
                            <div className="progress-bar">
                                <div
                                    className="progress-fill"
                                    style={{
                                        width: `${guess.score}%`,
                                        backgroundColor: color
                                    }}
                                />
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default QuizInputList;
