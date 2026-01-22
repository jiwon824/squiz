// quizService.js - CS 퀴즈 AI API 연동
// AI 서비스 API를 호출하여 유사도 계산 및 문제 데이터를 가져옵니다.

// 🔧 AI 서비스 Base URL (환경변수 또는 기본값 - 운영환경에서는 상대경로 사용)
const AI_SERVICE_URL = import.meta.env.VITE_AI_SERVICE_URL || '';

// 🎯 현재 문제 ID 저장 (세션 중 유지)
let currentWordId = null;
let currentProblem = null;

/**
 * 랜덤 단어 문제 가져오기
 * GET /api/words/random
 * @param {string} difficulty - 난이도 (easy, medium, hard) 선택
 * @param {string} category - 카테고리 선택
 * @returns {Promise<Object>} 문제 정보
 */
export const fetchRandomWord = async (difficulty = null, category = null) => {
    try {
        let url = `${AI_SERVICE_URL}/api/words/random`;
        const params = new URLSearchParams();
        if (difficulty) params.append('difficulty', difficulty);
        if (category) params.append('category', category);
        if (params.toString()) url += `?${params.toString()}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to fetch random word');

        const data = await response.json();
        currentWordId = data.id;
        currentProblem = data;
        return data;
    } catch (error) {
        console.error('fetchRandomWord error:', error);
        // 폴백: 기존 mock 데이터 사용
        return getFallbackProblem();
    }
};

/**
 * 단어 유사도 확인 (정답 시도)
 * POST /api/words/{id}/answer
 * @param {string} userWord - 사용자가 입력한 단어
 * @returns {Promise<Object>} 유사도 및 점수 결과
 */
export const checkSimilarity = async (userWord) => {
    // 현재 문제가 없으면 먼저 가져오기
    if (!currentWordId) {
        await fetchRandomWord();
    }

    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/words/${currentWordId}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userWord: userWord.trim() })
        });

        if (!response.ok) throw new Error('Failed to check similarity');

        const data = await response.json();

        return {
            userWord: data.userWord,
            answerWord: data.answerWord || null,
            similarity: data.similarity,
            score: data.score,
            isCorrect: data.isCorrect,
            bonuses: data.bonuses || {}
        };
    } catch (error) {
        console.error('checkSimilarity error:', error);
        // 폴백: 기존 mock 로직 사용
        return checkSimilarityFallback(userWord);
    }
};

/**
 * 오늘의 문제 정보 반환 (정답 제외)
 * @returns {Object} 문제 정보
 */
export const getDailyProblem = () => {
    // 캐시된 문제가 있으면 반환
    if (currentProblem) {
        return {
            id: currentProblem.id,
            category: currentProblem.category,
            difficulty: currentProblem.difficulty,
            hints: currentProblem.hints || [],
        };
    }
    // 없으면 폴백
    return getFallbackProblem();
};

/**
 * 문제 초기화 (새 게임 시작 시)
 */
export const initDailyProblem = async (difficulty = 'medium') => {
    const problem = await fetchRandomWord(difficulty);
    return {
        id: problem.id,
        category: problem.category,
        difficulty: problem.difficulty,
        hints: problem.hints || [],
    };
};

/**
 * 3D 임베딩 좌표 가져오기 (배치)
 * POST /api/embedding-3d-batch
 * @param {string} answerWord - 정답 단어
 * @param {string[]} attemptWords - 시도한 단어들
 * @param {string} category - 카테고리 (선택)
 * @returns {Promise<Object>} 3D 좌표 데이터
 */
export const getEmbedding3DBatch = async (answerWord, attemptWords, category = null) => {
    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/embedding-3d-batch`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                answerWord,
                attemptWords,
                category
            })
        });

        if (!response.ok) throw new Error('Failed to get embedding 3D batch');
        return await response.json();
    } catch (error) {
        console.error('getEmbedding3DBatch error:', error);
        return null;
    }
};

/**
 * 오늘의 문제 조회 (일일 퀴즈용)
 * GET /api/words/daily
 * @returns {Promise<Object>} 오늘의 문제 정보
 */
export const fetchDailyWord = async () => {
    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/words/daily`);
        if (!response.ok) throw new Error('Failed to fetch daily word');

        const data = await response.json();
        currentWordId = data.id;
        currentProblem = data;
        return data;
    } catch (error) {
        console.error('fetchDailyWord error:', error);
        // 폴백: 랜덤 단어 사용
        return await fetchRandomWord();
    }
};

/**
 * 어제의 정답 조회
 * GET /api/words/yesterday
 * @returns {Promise<Object>} 어제의 문제 및 정답 정보
 */
export const fetchYesterdayWord = async () => {
    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/words/yesterday`);
        if (!response.ok) throw new Error('Failed to fetch yesterday word');

        return await response.json();
    } catch (error) {
        console.error('fetchYesterdayWord error:', error);
        // 폴백: 로컬 계산
        return getYesterdayAnswerFallback();
    }
};

/**
 * 어제의 정답 반환 (동기 함수, 폴백용)
 * @returns {Object} 어제의 문제 정보
 */
export const getYesterdayAnswer = () => {
    // 폴백 로직 유지
    return getYesterdayAnswerFallback();
};

/**
 * 리더보드 조회
 * GET /api/leaderboard?date=YYYY-MM-DD&limit=10
 * @param {string} date - 조회할 날짜 (YYYY-MM-DD), 기본값: 오늘
 * @param {number} limit - 조회할 순위 개수, 기본값: 10
 * @returns {Promise<Object>} 리더보드 데이터
 */
export const fetchLeaderboard = async (date = null, limit = 10) => {
    try {
        const params = new URLSearchParams();
        if (date) params.append('date', date);
        params.append('limit', limit.toString());

        const url = `${AI_SERVICE_URL}/api/leaderboard?${params.toString()}`;
        console.log('🏆 리더보드 조회 URL:', url);
        
        const response = await fetch(url);
        console.log('🏆 리더보드 응답 상태:', response.status);

        if (!response.ok) throw new Error('Failed to fetch leaderboard');

        const data = await response.json();
        console.log('🏆 리더보드 데이터:', data);
        return data;
    } catch (error) {
        console.error('fetchLeaderboard error:', error);
        return { date: date || new Date().toISOString().split('T')[0], rankings: [], total: 0 };
    }
};

/**
 * 리더보드에 기록 저장
 * POST /api/leaderboard
 * @param {string} nickname - 사용자 닉네임
 * @param {number} attempts - 시도 횟수
 * @param {number} time - 소요 시간 (초)
 * @returns {Promise<Object>} 저장 결과
 */
export const saveToLeaderboard = async (nickname, attempts, time) => {
    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/leaderboard`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nickname, attempts, time })
        });

        if (!response.ok) throw new Error('Failed to save to leaderboard');

        return await response.json();
    } catch (error) {
        console.error('saveToLeaderboard error:', error);
        return { success: false, error: error.message };
    }
};

// ========================================
// 로컬 스토리지 기반 유저 추측 기록 관리
// ========================================

const GUESSES_STORAGE_KEY = 'commentle_guesses';

/**
 * 로컬 스토리지에서 유저별 추측 기록 조회
 * @param {string} userId - 유저 ID
 * @param {string} date - 조회할 날짜 (YYYY-MM-DD), 기본값: 오늘
 * @returns {Object} 유저의 추측 기록
 */
export const getLocalUserGuesses = (userId, date = null) => {
    const today = date || new Date().toISOString().split('T')[0];
    const storageKey = `${GUESSES_STORAGE_KEY}_${userId}_${today}`;

    try {
        const stored = localStorage.getItem(storageKey);
        if (stored) {
            return JSON.parse(stored);
        }
    } catch (error) {
        console.error('getLocalUserGuesses error:', error);
    }

    return [];
};

/**
 * 로컬 스토리지에 유저 추측 기록 저장
 * @param {string} userId - 유저 ID
 * @param {Array} guesses - 추측 기록 배열
 * @param {string} date - 저장할 날짜 (YYYY-MM-DD), 기본값: 오늘
 */
export const saveLocalUserGuesses = (userId, guesses, date = null) => {
    const today = date || new Date().toISOString().split('T')[0];
    const storageKey = `${GUESSES_STORAGE_KEY}_${userId}_${today}`;

    try {
        localStorage.setItem(storageKey, JSON.stringify(guesses));
    } catch (error) {
        console.error('saveLocalUserGuesses error:', error);
    }
};

/**
 * 로컬 스토리지에서 오래된 추측 기록 정리 (오늘이 아닌 모든 데이터 삭제)
 */
export const cleanupOldGuesses = () => {
    const today = new Date().toISOString().split('T')[0];

    try {
        const keysToRemove = [];

        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key && key.startsWith(GUESSES_STORAGE_KEY)) {
                // 오늘 날짜가 아닌 키는 삭제
                if (!key.endsWith(today)) {
                    keysToRemove.push(key);
                }
            }
        }

        keysToRemove.forEach(key => localStorage.removeItem(key));
    } catch (error) {
        console.error('cleanupOldGuesses error:', error);
    }
};

/**
 * 오늘의 퀴즈 정보 (위젯용)
 * @returns {Object} 카테고리와 힌트 수
 */
export const getTodayQuizInfo = () => {
    if (currentProblem) {
        return {
            category: currentProblem.category,
            hintCount: currentProblem.hints?.length || 0,
        };
    }
    return { category: 'CS', hintCount: 3 };
};

/**
 * 카테고리 목록 가져오기
 * GET /api/categories
 */
export const getCategories = async () => {
    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/categories`);
        if (!response.ok) throw new Error('Failed to fetch categories');
        return await response.json();
    } catch (error) {
        console.error('getCategories error:', error);
        return { categories: ['자료구조', '알고리즘', '네트워크', '데이터베이스', '운영체제'] };
    }
};

/**
 * 난이도 목록 가져오기
 * GET /api/difficulties
 */
export const getDifficulties = async () => {
    try {
        const response = await fetch(`${AI_SERVICE_URL}/api/difficulties`);
        if (!response.ok) throw new Error('Failed to fetch difficulties');
        return await response.json();
    } catch (error) {
        console.error('getDifficulties error:', error);
        return { difficulties: ['easy', 'medium', 'hard'] };
    }
};

// ========================================
// 폴백 함수들 (API 실패 시 사용)
// ========================================

const FALLBACK_WORDS = [
    { id: 1, answer: "알고리즘", category: "기초개념", difficulty: "easy", hints: ["문제 해결의 청사진", "빅오 표기법", "레시피와 같은 것"] },
    { id: 2, answer: "스택", category: "자료구조", difficulty: "medium", hints: ["LIFO(Last In First Out)", "함수 호출 스택", "Back 버튼 구현"] },
    { id: 3, answer: "큐", category: "자료구조", difficulty: "medium", hints: ["FIFO", "BFS 알고리즘", "대기열"] },
    { id: 4, answer: "재귀", category: "프로그래밍기법", difficulty: "medium", hints: ["마트료시카 인형", "스택 오버플로우", "팩토리얼"] },
    { id: 5, answer: "해시테이블", category: "자료구조", difficulty: "hard", hints: ["O(1) 탐색", "충돌 해결", "키-값 쌍"] },
];

const getFallbackProblem = () => {
    const today = new Date();
    const seed = today.getFullYear() * 10000 + (today.getMonth() + 1) * 100 + today.getDate();
    const index = seed % FALLBACK_WORDS.length;
    const word = FALLBACK_WORDS[index];
    currentWordId = word.id;
    currentProblem = word;
    return {
        id: word.id,
        category: word.category,
        difficulty: word.difficulty,
        hints: word.hints,
    };
};

const getYesterdayAnswerFallback = () => {
    const WORDS = [
        { answer: "알고리즘", category: "기초개념" },
        { answer: "스택", category: "자료구조" },
        { answer: "큐", category: "자료구조" },
        { answer: "재귀", category: "프로그래밍기법" },
        { answer: "해시테이블", category: "자료구조" },
        { answer: "트리", category: "자료구조" },
        { answer: "그래프", category: "자료구조" },
    ];

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const seed = yesterday.getFullYear() * 10000 + (yesterday.getMonth() + 1) * 100 + yesterday.getDate();
    const index = seed % WORDS.length;

    return WORDS[index];
};

const checkSimilarityFallback = async (userWord) => {
    await new Promise(resolve => setTimeout(resolve, 300));

    const cleanWord = userWord.trim().toLowerCase();
    const answer = FALLBACK_WORDS.find(w => w.id === currentWordId)?.answer || '스택';

    // 정답 체크
    if (cleanWord === answer.toLowerCase() || cleanWord === 'stack' && answer === '스택') {
        return { userWord, answerWord: answer, similarity: 1.0, score: 100, isCorrect: true };
    }

    // Mock 유사도
    let baseScore = Math.random() * 40 + 10;
    const csKeywords = ["lifo", "fifo", "자료구조", "데이터", "메모리", "포인터", "배열"];
    if (csKeywords.some(kw => cleanWord.includes(kw))) baseScore += 20;

    const finalScore = Math.max(5, Math.min(89, baseScore));

    return {
        userWord,
        answerWord: null,
        similarity: finalScore / 100,
        score: parseFloat(finalScore.toFixed(2)),
        isCorrect: false
    };
};
