// QuizSubHeader.jsx - Material 디자인 적용
import React from 'react';
import { Lightbulb, Tag, BarChart2, Lock } from 'lucide-react';

const QuizSubHeader = ({ problem, attemptCount = 0 }) => {
    // 기본값 방어
    const { category = 'CS', difficulty = 'Medium', hints = [] } = problem || {};

    // 난이도 색상 매핑
    const difficultyColors = {
        Easy: '#22c55e',
        Medium: '#f59e0b',
        Hard: '#ef4444'
    };

    return (
        <div className="quiz-subheader">
            {/* 오늘의 단어 정보 */}
            <div className="subheader-info-row">
                <div className="info-card category">
                    <Tag size={14} className="text-blue-500" />
                    <div>
                        <span className="label">카테고리 </span>
                        <span className="value">{category}</span>
                    </div>
                </div>

                <div className="info-card difficulty">
                    <BarChart2 size={14} style={{ color: difficultyColors[difficulty] }} />
                    <div>
                        <span className="label">난이도 </span>
                        <span className="value" style={{ color: difficultyColors[difficulty] }}>
                            {difficulty.toUpperCase()}
                        </span>
                    </div>
                </div>
            </div>

            {/* 힌트 영역 */}
            <div className="hints-section">
                <div className="hints-header">
                    <div className="icon">
                        <Lightbulb size={16} />
                    </div>
                    <span className="title">힌트</span>
                    <span className="subtitle">마우스를 올려 확인하세요</span>
                </div>

                <div className="hint-chips">
                    {hints.map((hint, index) => {
                        const unlockStep = (index + 1) * 10;
                        const isUnlocked = attemptCount >= unlockStep;

                        return (
                            <span
                                key={index}
                                className={`hint-chip ${!isUnlocked ? 'locked' : ''}`}
                                title={isUnlocked ? hint : `${unlockStep}회 시도 후 공개`}
                            >
                                {isUnlocked ? (
                                    hint
                                ) : (
                                    <>
                                        <Lock size={12} style={{ marginRight: 4 }} />
                                        {unlockStep}회 후 공개
                                    </>
                                )}
                            </span>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default QuizSubHeader;
