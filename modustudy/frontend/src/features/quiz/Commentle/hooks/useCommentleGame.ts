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

            try {
                const data = await fetchDailyWord();
                setProblem({
                    id: data.id,
                    category: data.category,
                    difficulty: data.difficulty || 'Medium',
                    hints: data.hints || [],
                });

                const leaderboardData = await fetchLeaderboard(today, 10);
                setLeaderboard(leaderboardData.rankings || []);

                if (isLoggedIn && user?.id) {
                    const savedGuesses = getLocalUserGuesses(user.id, today);
                    if (savedGuesses.length > 0) {
                        setGuesses(savedGuesses);
                    }
                }

                cleanupOldGuesses();
            } catch (error) {
                console.error('Failed to load quiz data:', error);
            } finally {
                setProblemLoading(false);
            }
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
                attemptNum: guesses.length + 1
            };

            const updatedGuesses = [newGuess, ...guesses];
            setGuesses(updatedGuesses);

            if (isLoggedIn && user?.id) {
                saveLocalUserGuesses(user.id, updatedGuesses);
            }

            if (result.isCorrect) {
                updateLeaderboard(guesses.length + 1);
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
