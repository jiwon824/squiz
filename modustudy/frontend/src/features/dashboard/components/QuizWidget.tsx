// QuizWidget.tsx - quizService와 연동
import { useNavigate } from 'react-router-dom';
import { getYesterdayAnswer, getTodayQuizInfo } from '@/features/quiz/services/quizService';
import './QuizWidget.css';

export const QuizWidget = () => {
    const navigate = useNavigate();

    // quizService에서 데이터 가져오기
    const yesterdayInfo = getYesterdayAnswer();
    const todayInfo = getTodayQuizInfo();

    return (
        <div className="quiz-action-widget" onClick={() => navigate('/commentle')}>
            <div className="quiz-action-content">
                <div className="action-info">
                    <h3>꼬멘틀 <span className="accent-blue">CS ver</span></h3>
                    <p>오늘의 카테고리: {todayInfo.category}</p>
                </div>

                {/* 어제의 정답 표시 */}
                <div className="yesterday-info">
                    <span className="label">YESTERDAY :</span>
                    <span className="answer">{yesterdayInfo.answer}</span>
                </div>

                {/* 하단 투명 화살표 버튼 */}
                <div className="widget-action-footer">
                    <div className="arrow-circle">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="9 18 15 12 9 6" />
                        </svg>
                    </div>
                </div>
            </div>
        </div>
    );
};
