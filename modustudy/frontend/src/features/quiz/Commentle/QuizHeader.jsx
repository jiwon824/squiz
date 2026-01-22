import React from 'react';
import { Terminal } from 'lucide-react';

const QuizHeader = () => {
    return (
        <div className="quiz-header">
            <div className="header-left">
                <div className="icon-box">
                    <Terminal size={24} />
                </div>
                <div className="title-area">
                    <div className="title-row">
                        <h1>COMMENTLE</h1>
                        <span className="version-badge">v1.0 BETA</span>
                    </div>
                    <p className="subtitle">CS Knowledge Integration Test</p>
                </div>
            </div>
        </div>
    );
};

export default QuizHeader;
