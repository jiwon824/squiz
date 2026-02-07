// CommentleQuiz.jsx - 1:1 레이아웃 + 반응형 + 점수 기준 통합
import React, { useState, useEffect, lazy, Suspense } from 'react';
import { useNavigate } from 'react-router-dom';
import QuizHeader from './QuizHeader';
import QuizSubHeader from './QuizSubHeader';
import QuizInputList from './QuizInputlist';
import QuizGuessInput from './QuizGuessInput';
import { Modal } from '@/shared/components/Modal';
import { BackButton } from '@/shared/components';
import { Trophy, Info, Crown, Box } from 'lucide-react';
import { checkSimilarity, fetchDailyWord, fetchLeaderboard, saveToLeaderboard, getLocalUserGuesses, saveLocalUserGuesses, cleanupOldGuesses } from '../services/quizService';
import { useAuthStore } from '@/store/authStore';
import './Commentle.css';

// 🚀 3D 시각화 컴포넌트 지연 로딩 (Three.js 번들 분리)
const Embedding3DViewer = lazy(() => import('./Embedding3DViewer'));

const CommentleQuiz = () => {
    const navigate = useNavigate();

    // � 사용자 정보 가져오기 (먼저 선언)
    const { user, isLoggedIn } = useAuthStore();

    // �🔍 게임 상태 관리
    const [guesses, setGuesses] = useState([]);
    const [loading, setLoading] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);

    // 📱 반응형: 콤팩트 모드 (1000px 이하)
    const [isCompactMode, setIsCompactMode] = useState(window.innerWidth <= 1000);

    useEffect(() => {
        const handleResize = () => setIsCompactMode(window.innerWidth <= 1000);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    // 🏆 리더보드 데이터 (API 기반)
    const [leaderboard, setLeaderboard] = useState([]);

    // 💡 오늘의 문제 데이터 (API 연동)
    const [problem, setProblem] = useState({
        id: null,
        category: 'CS',
        difficulty: 'Medium',
        hints: [],
    });
    const [problemLoading, setProblemLoading] = useState(true);

    // 📅 오늘 날짜 추적 (날짜별 리더보드 관리)
    const [currentDate, setCurrentDate] = useState(() => new Date().toISOString().split('T')[0]);

    // ⏱️ 퀴즈 시작 시간 추적
    const [startTime, setStartTime] = useState(() => Date.now());

    // 컴포넌트 마운트 시 문제 및 리더보드 가져오기
    useEffect(() => {
        const loadQuizData = async () => {
            setProblemLoading(true);
            const today = new Date().toISOString().split('T')[0];

            // 날짜가 바뀌었으면 초기화
            if (today !== currentDate) {
                setCurrentDate(today);
                setGuesses([]);
                setStartTime(Date.now());
            }

            try {
                // 오늘의 문제 가져오기
                const data = await fetchDailyWord();
                setProblem({
                    id: data.id,
                    category: data.category,
                    difficulty: data.difficulty || 'Medium',
                    hints: data.hints || [],
                });

                // 오늘 날짜의 리더보드 가져오기
                const leaderboardData = await fetchLeaderboard(today, 10);
                setLeaderboard(leaderboardData.rankings || []);

                // 🔐 로그인 유저라면 로컬 스토리지에서 오늘의 추측 기록 불러오기
                if (isLoggedIn && user?.id) {
                    const savedGuesses = getLocalUserGuesses(user.id, today);
                    if (savedGuesses.length > 0) {
                        setGuesses(savedGuesses);
                    }
                }

                // 오래된 추측 기록 정리
                cleanupOldGuesses();
            } catch (error) {
            } finally {
                setProblemLoading(false);
            }
        };
        loadQuizData();
    }, [currentDate, isLoggedIn, user?.id]);

    // 📤 단어 제출 핸들러
    const handleGuess = async (word) => {
        if (!word.trim()) return;
        setLoading(true);

        try {
            const result = await checkSimilarity(word);
            const mockRank = result.isCorrect ? 1 : Math.floor(Math.random() * 10000) + 10;

            const newGuess = {
                id: Date.now(),
                word: result.userWord,
                score: result.score,
                rank: mockRank,
                attemptNum: guesses.length + 1 // 시도 횟수 기록
            };

            const updatedGuesses = [newGuess, ...guesses];
            setGuesses(updatedGuesses);

            // 🔐 로그인 유저라면 로컬 스토리지에 추측 기록 저장
            if (isLoggedIn && user?.id) {
                saveLocalUserGuesses(user.id, updatedGuesses);
            }

            // 정답일 경우 리더보드 업데이트
            if (result.isCorrect) {
                updateLeaderboard(guesses.length + 1);
                setTimeout(() => setShowSuccess(true), 500);
                // 3초 후 자동 닫힘
                setTimeout(() => setShowSuccess(false), 3000);
            }
        } catch (error) {
        } finally {
            setLoading(false);
        }
    };

    // 리더보드 업데이트 함수 (API 기반)
    const updateLeaderboard = async (attemptCount) => {
        // 로그인 사용자는 닉네임, 비로그인은 Guest_XXXX
        const nickname = isLoggedIn && user?.nickname
            ? user.nickname
            : `Guest_${Date.now().toString().slice(-4)}`;

        // 소요 시간 계산 (초 단위)
        const elapsedTime = Math.floor((Date.now() - startTime) / 1000);

        try {
            // 리더보드에 저장
            const result = await saveToLeaderboard(nickname, attemptCount, elapsedTime);

            if (result.success) {

                // 리더보드 재조회
                const leaderboardData = await fetchLeaderboard(currentDate, 10);
                setLeaderboard(leaderboardData.rankings || []);
            }
        } catch (error) {
        }
    };

    // 점수 기준 데이터
    const scoreGuide = [
        { range: '90~100', color: '#22c55e', label: '정답!' },
        { range: '75~89', color: '#3b82f6', label: '아주 좋음' },
        { range: '50~74', color: '#a855f7', label: '좋은 방향' },
        { range: '25~49', color: '#f59e0b', label: '보통' },
        { range: '0~24', color: '#ef4444', label: '멀어요' },
    ];

    return (
        <div className="w-full max-w-[1400px] mx-auto animate-fade-in">
            {/* 상단 헤더: 뒤로가기 + 정보 아이콘 */}
            <div className="quiz-top-header">
                <BackButton variant="icon-only" onClick={() => navigate('/quiz')} />

                {/* 점수 산정 방식 안내 */}
                <div className="info-tooltip-wrapper">
                    <button className="info-btn">
                        <Info size={18} />
                    </button>
                    <div className="info-tooltip">
                        <h4>🎯 점수 산정 방식</h4>
                        <p>AI가 정답 단어와 입력 단어의 <strong>의미적 유사도</strong>를 분석합니다.</p>
                        <ul>
                            <li><span className="dot" style={{ background: '#22c55e' }}></span> <strong>90~100:</strong> 정답!</li>
                            <li><span className="dot" style={{ background: '#3b82f6' }}></span> <strong>75~89:</strong> 아주 가까움</li>
                            <li><span className="dot" style={{ background: '#a855f7' }}></span> <strong>50~74:</strong> 좋은 방향</li>
                            <li><span className="dot" style={{ background: '#f59e0b' }}></span> <strong>25~49:</strong> 보통</li>
                            <li><span className="dot" style={{ background: '#ef4444' }}></span> <strong>0~24:</strong> 멀어요</li>
                        </ul>
                    </div>
                </div>
            </div>

            {/* 1:1 레이아웃 (모바일에서는 세로 스택) */}
            <div className="commentle-layout">
                {/* 좌측: 게임 패널 */}
                <div className="commentle-game-panel">
                    {/* 메인 게임 컨테이너 */}
                    <div className="commentle-container">
                        <div className="game-status-badge">
                            현재 <span className="highlight">{guesses.length + 1}</span>번째 시도 중
                        </div>

                        <QuizHeader />
                        <QuizSubHeader problem={problem} attemptCount={guesses.length} />

                        <div className="mt-6">
                            <QuizGuessInput onGuess={handleGuess} loading={loading} />
                        </div>
                    </div>

                    {/* 정답 목록 */}
                    <QuizInputList guesses={guesses} />

                    {/* 점수 기준 카드 */}
                    <div className="score-guide-card">
                        <h4>
                            <Info size={16} />
                            점수 기준
                        </h4>
                        <div className="score-guide-list">
                            {scoreGuide.map((item, i) => (
                                <div key={i} className="score-guide-item">
                                    <div className="score-dot" style={{ background: item.color }} />
                                    <strong>{item.range}:</strong> {item.label}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 리더보드 (콤팩트 모드에서만 하단에 표시) */}
                    {isCompactMode && (
                        <div className="leaderboard-card">
                            <div className="card-header">
                                <Crown size={18} className="text-yellow-500" />
                                <span>명예의 전당</span>
                            </div>
                            {leaderboard.length > 0 ? (
                                <ul className="leader-list">
                                    {leaderboard.map((entry, index) => (
                                        <li key={`${entry.nickname}-${index}`} className={`leader-item rank-${index + 1}`}>
                                            <span className="rank-num">{entry.rank || index + 1}</span>
                                            <span className="user-id">{entry.nickname}</span>
                                            <span className="guess-count">{entry.attempts}회 · {entry.time}초</span>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <div className="text-center py-4 text-gray-400 text-sm">
                                    아직 정복자가 없습니다
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {/* 우측: 3D 뷰어 패널 */}
                <div className="commentle-3d-panel">
                    <div className="embedding-viewer-container">

                        <Suspense fallback={
                            <div className="w-full h-[500px] bg-slate-900 rounded-xl flex items-center justify-center">
                                <div className="text-center text-slate-400">
                                    <div className="w-12 h-12 border-4 border-slate-600 border-t-blue-500 rounded-full animate-spin mx-auto mb-4"></div>
                                    <p className="text-sm font-medium">3D 시각화 로딩 중...</p>
                                </div>
                            </div>
                        }>
                            <Embedding3DViewer guesses={guesses} />
                        </Suspense>

                        {/* 리더보드 (데스크탑에서만 3D 뷰어 아래 표시) */}
                        {!isCompactMode && (
                            <div className="leaderboard-card mt-4">
                                <div className="card-header">
                                    <Crown size={18} className="text-yellow-500" />
                                    <span>명예의 전당</span>
                                </div>
                                {leaderboard.length > 0 ? (
                                    <ul className="leader-list">
                                        {leaderboard.map((entry, index) => (
                                            <li key={`${entry.nickname}-${index}`} className={`leader-item rank-${index + 1}`}>
                                                <span className="rank-num">{entry.rank || index + 1}</span>
                                                <span className="user-id">{entry.nickname}</span>
                                                <span className="guess-count">{entry.attempts}회 · {entry.time}초</span>
                                            </li>
                                        ))}
                                    </ul>
                                ) : (
                                    <div className="text-center py-4 text-gray-400 text-sm">
                                        🏆 지금 1등에 도전해보세요!
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* 정답 축하 모달 */}
            <Modal isOpen={showSuccess} onClose={() => setShowSuccess(false)} title="축하합니다! 🎉">
                <div className="flex flex-col items-center justify-center p-6 text-center">
                    <Trophy size={64} className="text-yellow-400 animate-bounce" />
                    <h2 className="text-2xl font-bold mt-4">정답입니다!</h2>
                    <p className="mt-2 text-gray-600">
                        단 <strong>{guesses.length}</strong>번 만에 맞추셨어요!
                    </p>
                    <button onClick={() => setShowSuccess(false)} className="btn-confirm mt-6">멋져요! 😎</button>
                </div>
            </Modal>
        </div>
    );
};

export default CommentleQuiz;