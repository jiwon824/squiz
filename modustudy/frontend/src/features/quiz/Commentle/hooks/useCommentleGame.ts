import { useState, useEffect } from 'react';
import {
    checkSimilarity,
    fetchDailyWord,
    fetchLeaderboard,
    saveToLeaderboard,
    getLocalUserGuesses,
    saveLocalUserGuesses,
    cleanupOldGuesses
} from '../../services/quizService';
import { useAuthStore } from '@/store/authStore';

export interface Guess {
    id: number;
    word: string;
    score: number;
    rank: number;
    attemptNum: number;
}

export interface Problem {
    id: number | null;
    category: string;
    difficulty: string;
    hints: string[];
}

export const useCommentleGame = () => {
    const { user, isLoggedIn } = useAuthStore();

    const [guesses, setGuesses] = useState<Guess[]>([]);
    const [loading, setLoading] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);
    const [leaderboard, setLeaderboard] = useState<any[]>([]);
    const [problem, setProblem] = useState<Problem>({
        id: null,
        category: 'CS',
        difficulty: 'Medium',
        hints: [],
    });
    const [problemLoading, setProblemLoading] = useState(true);
    const [currentDate, setCurrentDate] = useState(() => new Date().toISOString().split('T')[0]);
    const [startTime, setStartTime] = useState(() => Date.now());

    useEffect(() => {
        const loadQuizData = async () => {
            setProblemLoading(true);
            const today = new Date().toISOString().split('T')[0];

            if (today !== currentDate) {
                setCurrentDate(today);
                setGuesses([]);
                setStartTime(Date.now());
            }

            // 문제와 리더보드를 병렬로 로드 (하나가 실패해도 다른 하나는 로드됨)
            const [problemResult, leaderboardResult] = await Promise.allSettled([
                fetchDailyWord(),
                fetchLeaderboard(today, 10)
            ]);

            // 문제 처리
            if (problemResult.status === 'fulfilled') {
                const data = problemResult.value;
                setProblem({
                    id: data.id,
                    category: data.category,
                    difficulty: data.difficulty || 'Medium',
                    hints: data.hints || [],
                });
            } else {
                console.error('Failed to load problem:', problemResult.reason);
            }

            // 리더보드 처리 (문제 실패와 무관하게 항상 시도)
            if (leaderboardResult.status === 'fulfilled') {
                setLeaderboard(leaderboardResult.value.rankings || []);
            } else {
                console.error('Failed to load leaderboard:', leaderboardResult.reason);
                setLeaderboard([]);
            }

            // 유저 데이터 로드
            if (isLoggedIn && user?.id) {
                const savedGuesses = getLocalUserGuesses(user.id, today);
                if (savedGuesses.length > 0) {
                    setGuesses(savedGuesses);
                }
            }

            cleanupOldGuesses();
            setProblemLoading(false);
        };
        loadQuizData();
    }, [currentDate, isLoggedIn, user?.id]);

    const handleGuess = async (word: string) => {
        if (!word.trim()) return;
        setLoading(true);

        try {
            const result = await checkSimilarity(word);

            const newGuess: Guess = {
                id: Date.now(),
                word: result.userWord,
                score: result.score,
                rank: guesses.length + 1,
                attemptNum: guesses.length + 1
            };

            const updatedGuesses = [newGuess, ...guesses];
            setGuesses(updatedGuesses);

            if (isLoggedIn && user?.id) {
                saveLocalUserGuesses(user.id, updatedGuesses);
            }

            if (result.isCorrect) {
                // 로그인 사용자만 리더보드에 기록 등록
                if (isLoggedIn) {
                    updateLeaderboard(guesses.length + 1);
                }
                setShowSuccess(true);
            }
        } catch (error) {
            console.error('Error:', error);
        } finally {
            setLoading(false);
        }
    };

    const updateLeaderboard = async (attemptCount: number) => {
        const nickname = isLoggedIn && user?.nickname
            ? user.nickname
            : `Guest_${Date.now().toString().slice(-4)}`;

        const elapsedTime = Math.floor((Date.now() - startTime) / 1000);

        try {
            const result = await saveToLeaderboard(nickname, attemptCount, elapsedTime);
            if (result.success) {
                const leaderboardData = await fetchLeaderboard(currentDate, 10);
                setLeaderboard(leaderboardData.rankings || []);
            }
        } catch (error) {
            console.error('Failed to update leaderboard:', error);
        }
    };

    return {
        guesses,
        loading,
        showSuccess,
        setShowSuccess,
        leaderboard,
        problem,
        problemLoading,
        handleGuess,
    };
};
