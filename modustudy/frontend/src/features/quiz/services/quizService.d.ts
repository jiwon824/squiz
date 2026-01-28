// quizService 타입 선언

export interface Problem {
    id: number;
    category: string;
    difficulty: string;
    hints: string[];
}

export interface SimilarityResult {
    userWord: string;
    answerWord: string | null;
    similarity: number;
    score: number;
    isCorrect: boolean;
    bonuses?: Record<string, unknown>;
}

export interface LeaderboardEntry {
    nickname: string;
    attempts: number;
    time: number;
    rank?: number;
}

export interface LeaderboardData {
    date: string;
    rankings: LeaderboardEntry[];
    total: number;
}

export interface Guess {
    id: number;
    word: string;
    score: number;
    rank: number;
    attemptNum: number;
}

export function fetchRandomWord(difficulty?: string | null, category?: string | null): Promise<Problem>;
export function checkSimilarity(userWord: string): Promise<SimilarityResult>;
export function getDailyProblem(): Problem;
export function initDailyProblem(difficulty?: string): Promise<Problem>;
export function getEmbedding3DBatch(answerWord: string, attemptWords: string[], category?: string | null): Promise<unknown>;
export function fetchDailyWord(): Promise<Problem>;
export function fetchYesterdayWord(): Promise<{ answer: string; category: string }>;
export function getYesterdayAnswer(): { answer: string; category: string };
export function fetchLeaderboard(date?: string | null, limit?: number): Promise<LeaderboardData>;
export function saveToLeaderboard(nickname: string, attempts: number, time: number): Promise<{ success: boolean; error?: string }>;
export function getLocalUserGuesses(userId: string | number, date?: string | null): Guess[];
export function saveLocalUserGuesses(userId: string | number, guesses: Guess[], date?: string | null): void;
export function cleanupOldGuesses(): void;
export function getTodayQuizInfo(): { category: string; hintCount: number };
export function getCategories(): Promise<{ categories: string[] }>;
export function getDifficulties(): Promise<{ difficulties: string[] }>;
