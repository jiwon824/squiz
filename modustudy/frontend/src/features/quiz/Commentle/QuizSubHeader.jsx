import React from 'react';
import { Hash, Lightbulb, BarChart, Tag } from 'lucide-react';

const QuizSubHeader = ({ problem, attemptCount = 0 }) => {
    // 기본값 방어
    const { id = '000', category = 'CS', difficulty = 'Medium', hints = [] } = problem || {};

    return (
        <div className="quiz-subheader group">
            {/* 배경 데코레이션 */}
            <div style={{ position: 'absolute', top: 0, right: 0, padding: '16px', opacity: 0.05, pointerEvents: 'none' }}>
                <Hash size={120} />
            </div>

            <div className="subheader-top">
                <div className="tags">
                    <span className="tag-badge tag-category">
                        <Tag size={12} /> {category}
                    </span>
                    <span className={`tag-badge tag-difficulty ${difficulty}`}>
                        <BarChart size={12} /> {difficulty.toUpperCase()}
                    </span>
                </div>
            </div>

            <div className="hints-area">
                <div className="hints-label">
                    <Lightbulb size={16} style={{ color: '#f59e0b' }} />
                    <span>HINTS (Hover to reveal):</span>
                </div>
                <div className="hint-chips">
                    {hints.map((hint, index) => {
                        const unlockStep = (index + 1) * 10;
                        const isUnlocked = attemptCount >= unlockStep;

                        return (
                            <span
                                key={index}
                                className="hint-item"
                                style={isUnlocked
                                    ? {}
                                    : { filter: 'none', background: '#f1f5f9', color: '#94a3b8', cursor: 'default' }
                                }
                                title={isUnlocked ? "마우스를 올리면 힌트가 보입니다" : `${unlockStep}회 시도 후 열립니다`}
                            >
                                {isUnlocked ? hint : `🔒 ${unlockStep}회째 공개`}
                            </span>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default QuizSubHeader;
