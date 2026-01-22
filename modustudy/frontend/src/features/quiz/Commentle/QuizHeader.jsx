// QuizHeader.jsx - Material 디자인 적용
import React from 'react';
import { Terminal, Sparkles } from 'lucide-react';

const QuizHeader = () => {
    return (
        <div className="quiz-header-material">
            <div className="header-icon">
                <Terminal size={24} />
            </div>
            <div className="header-content">
                <div className="header-title-row">
                    <h1>COMMENTLE</h1>
                    <span className="badge-beta">
                        <Sparkles size={10} />
                        v1.0 BETA
                    </span>
                </div>
                <p className="header-subtitle">CS Knowledge Integration Test</p>
            </div>
        </div>
    );
};

export default QuizHeader;
