import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import QuizHeader from './QuizHeader';
import QuizSubHeader from './QuizSubHeader';
import QuizInputList from './QuizInputlist';
import QuizGuessInput from './QuizGuessInput';
import { Modal } from '@/shared/components/Modal';
import { Trophy, Star, Info, Crown, ArrowLeft } from 'lucide-react';
import { checkSimilarity } from '../services/quizService';
import Embedding3DViewer from './Embedding3DViewer';
import './Commentle.css';

const CommentleQuiz = () => {
    const navigate = useNavigate();
    // 🔍 게임 상태 관리
    const [guesses, setGuesses] = useState([]);
    const [loading, setLoading] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);

    // 🏆 리더보드 데이터 (localStorage 기반)
    const [leaderboard, setLeaderboard] = useState(() => {
        const saved = localStorage.getItem('commentle-leaderboard');
        return saved ? JSON.parse(saved) : [];
    });

    // 💡 오늘의 문제 데이터 (Mock)
    const problem = {
        id: '20260115',
        category: '자료구조',
        difficulty: 'Medium',
        hints: ['LIFO(Last In First Out)', '함수 호출 스택', 'Back 버튼 구현'],
    };

    // 📤 단어 제출 핸들러
    const handleGuess = async (word) => {
        if (!word.trim()) return;
        setLoading(true);

        try {
            // Service를 통해 데이터 받아오기 (Mock API)
            const result = await checkSimilarity(word);

            // 랭크 시뮬레이션 (백엔드 데이터에 없어서 임의 생성)
            const mockRank = result.isCorrect ? 1 : Math.floor(Math.random() * 10000) + 10;

            const newGuess = {
                id: Date.now(),
                word: result.userWord,
                score: result.score,
                rank: mockRank,
                attemptNum: guesses.length + 1 // 시도 횟수 기록
            };

            setGuesses(prev => [newGuess, ...prev]);

            // 정답일 경우 리더보드 업데이트
            if (result.isCorrect) {
                updateLeaderboard(guesses.length + 1);
                setTimeout(() => setShowSuccess(true), 500);
                // 3초 후 자동 닫힘
                setTimeout(() => setShowSuccess(false), 3000);
            }
        } catch (error) {
            console.error('Error:', error);
        } finally {
            setLoading(false);
        }
    };

    // 리더보드 업데이트 함수
    const updateLeaderboard = (attemptCount) => {
        const userId = `Player_${Date.now().toString().slice(-6)}`; // 임시 유저 ID
        const newEntry = { id: userId, count: attemptCount };

        const updated = [...leaderboard, newEntry]
            .sort((a, b) => a.count - b.count) // 시도 횟수 오름차순
            .slice(0, 5); // 상위 5명만 유지

        setLeaderboard(updated);
        localStorage.setItem('commentle-leaderboard', JSON.stringify(updated));
    };

    return (
        <div className="w-full max-w-[1200px] mx-auto animate-fade-in">
            {/* 뒤로가기 버튼 */}
            <div className="py-2 px-4 mb-2">
                <button
                    onClick={() => navigate('/quiz')}
                    className="back-btn"
                >
                    <ArrowLeft size={20} />
                </button>
            </div>

            <div className="commentle-layout">
                {/* 메인 게임 영역 */}
                <div className="commentle-container bento-card p-6">
                    <div className="game-status-badge">
                        현재 <span className="highlight">{guesses.length + 1}</span>번째 시도 중
                    </div>

                    <QuizHeader />
                    <QuizSubHeader problem={problem} attemptCount={guesses.length} />

                    <div className="game-area mt-8">
                        <QuizGuessInput onGuess={handleGuess} loading={loading} />

                        {/* 🌌 3D 임베딩 시각화 */}
                        <div className="mt-6">
                            <Embedding3DViewer guesses={guesses} />
                        </div>

                        <br />
                        {/* QuizInputList 내부에서 score를 이용해 Progress Bar를 그리도록 props 전달 */}
                        <QuizInputList guesses={guesses} />
                    </div>
                </div>

                {/* 오른쪽 사이드바 영역 (포스트잇 & 리더보드) */}
                <aside className="commentle-sidebar">
                    {/* 📝 점수 기준 포스트잇 */}
                    <div className="sticky-note">
                        <div className="note-pin"></div>
                        <h3><Info size={18} /> 점수 기준</h3>
                        <p>정답 단어와 의미적으로 얼마나 유사한지 AI가 분석합니다.</p>
                        <ul>
                            <li><span className="dot" style={{ background: '#4ade80' }}></span> <strong>90~100:</strong> 정답!</li>
                            <li><span className="dot" style={{ background: '#60a5fa' }}></span> <strong>75~89:</strong> 아주 좋음</li>
                            <li><span className="dot" style={{ background: '#a78bfa' }}></span> <strong>50~74:</strong> 좋은 방향</li>
                            <li><span className="dot" style={{ background: '#fbbf24' }}></span> <strong>25~49:</strong> 보통</li>
                            <li><span className="dot" style={{ background: '#f87171' }}></span> <strong>0~24:</strong> 멈</li>
                        </ul>
                    </div>

                    {/* 👑 리더보드 (최소 도전 명예의 전당) */}
                    <div className="leaderboard-card">
                        <div className="card-header">
                            <Crown size={20} className="text-yellow-500" />
                            <h3>명예의 전당</h3>
                        </div>

                        {leaderboard.length > 0 ? (
                            <ul className="leader-list">
                                {leaderboard.map((user, index) => (
                                    <li key={user.id} className={`leader-item rank-${index + 1}`}>
                                        <span className="rank-num">{index + 1}</span>
                                        <span className="user-id">{user.id}</span>
                                        <span className="guess-count">{user.count}회</span>
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <div className="p-6 text-center text-slate-400 flex flex-col items-center">
                                <span className="text-3xl mb-2 grayscale opacity-50">🏆</span>
                                <p className="text-sm font-bold text-slate-600">아직 정복자가 없습니다</p>
                                <p className="text-xs text-blue-500 mt-1 font-bold animate-pulse">
                                    지금 1등에 도전해보세요!
                                </p>
                            </div>
                        )}
                    </div>
                </aside>

                {/* 정답 축하 모달 */}
                <Modal isOpen={showSuccess} onClose={() => setShowSuccess(false)} title="축하합니다! 🎉" size="sm">
                    <div className="flex flex-col items-center justify-center p-6 text-center">
                        <Trophy size={64} className="text-yellow-400 animate-bounce" />
                        <h2 className="text-2xl font-bold mt-4">정답입니다!</h2>
                        <p className="mt-2 text-gray-600">
                            단 <strong>{guesses.length}</strong>번 만에 맞추셨어요!
                        </p>
                        <button onClick={() => setShowSuccess(false)} className="btn-confirm mt-6">멋져요! 😎</button>
                        <p className="text-xs text-gray-400 mt-4">
                            (3초 후 자동으로 닫힙니다)
                        </p>
                    </div>
                </Modal>
            </div>
        </div>
    );
};

export default CommentleQuiz;