// QuizWidget.tsx - quizService와 연동
import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { fetchYesterdayWord, fetchDailyWord } from '../../quiz/services/quizService';
import './QuizWidget.css';

export const QuizWidget = () => {
    const navigate = useNavigate();

    // 어제의 정답 및 오늘의 문제 상태
    const [yesterdayInfo, setYesterdayInfo] = useState({ answer: '...', category: 'CS' });
    const [todayInfo, setTodayInfo] = useState({ category: 'CS', hintCount: 0 });
    const [loading, setLoading] = useState(true);

    // 컴포넌트 마운트 시 API 호출
    useEffect(() => {
        const loadQuizData = async () => {
            setLoading(true);
            try {
                // 어제의 정답 가져오기
                const yesterdayData = await fetchYesterdayWord();
                setYesterdayInfo({
                    answer: yesterdayData.answer || '알고리즘',
                    category: yesterdayData.category || 'CS'
                });

                // 오늘의 문제 정보 가져오기
                const todayData = await fetchDailyWord();
                setTodayInfo({
                    category: todayData.category || 'CS',
                    hintCount: todayData.hints?.length || 0
                });
            } catch (error) {
                console.error('Failed to load quiz data:', error);
                // 에러 시 기본값 유지
            } finally {
                setLoading(false);
            }
        };

        loadQuizData();
    }, []);

    return (
        <div className="quiz-action-widget" onClick={() => navigate('/quiz-commentle')}>
            <div className="quiz-action-content">
                <div className="action-info">
                    <h3>꼬멘틀 <span className="accent-blue">CS ver</span></h3>
                    <p>오늘의 카테고리: {loading ? '...' : todayInfo.category}</p>
                </div>

                {/* 어제의 정답 표시 */}
                <div className="yesterday-info">
                    <span className="label">YESTERDAY :</span>
                    <span className="answer">{loading ? '...' : yesterdayInfo.answer}</span>
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
