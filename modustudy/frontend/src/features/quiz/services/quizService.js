// src/features/quiz/services/quizService.js

// 🎯 오늘의 단어 데이터 (백엔드 words.json 기반)
// TODO: 실제 배포 시 백엔드 API로 대체
const WORDS = [
    { id: 1, answer: "알고리즘", category: "기초개념", hints: ["문제 해결의 청사진", "빅오 표기법", "레시피와 같은 것"] },
    { id: 2, answer: "스택", category: "자료구조", hints: ["LIFO(Last In First Out)", "함수 호출 스택", "Back 버튼 구현"] },
    { id: 3, answer: "큐", category: "자료구조", hints: ["FIFO", "BFS 알고리즘", "대기열"] },
    { id: 4, answer: "재귀", category: "프로그래밍기법", hints: ["마트료시카 인형", "스택 오버플로우", "팩토리얼"] },
    { id: 5, answer: "해시테이블", category: "자료구조", hints: ["O(1) 탐색", "충돌 해결", "키-값 쌍"] },
    { id: 6, answer: "트리", category: "자료구조", hints: ["루트와 리프", "DOM 구조", "이진 탐색 트리"] },
    { id: 7, answer: "그래프", category: "자료구조", hints: ["정점과 간선", "DFS/BFS", "네트워크 관계"] },
];

// 정답으로 인정되는 변형들 (대소문자 무시)
const ANSWER_VARIATIONS = {
    "스택": ["스택", "stack", "STACK", "Stack"],
    "큐": ["큐", "queue", "QUEUE", "Queue"],
    "알고리즘": ["알고리즘", "algorithm", "ALGORITHM", "Algorithm"],
    "재귀": ["재귀", "recursion", "RECURSION", "Recursion"],
    "해시테이블": ["해시테이블", "hashtable", "HASHTABLE", "HashTable", "hash table"],
    "트리": ["트리", "tree", "TREE", "Tree"],
    "그래프": ["그래프", "graph", "GRAPH", "Graph"],
};

/**
 * 날짜 기반으로 오늘의 문제를 선택
 * @returns {Object} 오늘의 문제 데이터
 */
const getDailyWord = () => {
    const today = new Date();
    // 날짜를 시드로 사용하여 매일 다른 문제 선택
    const seed = today.getFullYear() * 10000 + (today.getMonth() + 1) * 100 + today.getDate();
    const index = seed % WORDS.length;
    return WORDS[index];
};

// 오늘의 문제 (앱 시작 시 결정)
const DAILY_PROBLEM = getDailyWord();

/**
 * 단어 유사도 계산 API 호출 (Mock)
 * @param {string} userWord 사용자가 입력한 단어
 * @returns {Promise<Object>} 분석 결과
 */
export const checkSimilarity = async (userWord) => {
    // 네트워크 지연 시뮬레이션 (0.3초)
    await new Promise(resolve => setTimeout(resolve, 300));

    const cleanWord = userWord.trim();
    const answerWord = DAILY_PROBLEM.answer;

    // 정답 변형 체크 (대소문자 무시)
    const variations = ANSWER_VARIATIONS[answerWord] || [answerWord];
    const isExactMatch = variations.some(v => v.toLowerCase() === cleanWord.toLowerCase());

    if (isExactMatch) {
        return {
            userWord: cleanWord,
            answerWord: answerWord,
            similarity: 1.0,
            score: 100,
            isCorrect: true
        };
    }

    // Mock 유사도 계산 (실제로는 백엔드 AI가 계산)
    // 관련 단어들에 대해 더 높은 점수를 부여
    let baseScore = Math.random() * 40 + 10; // 10 ~ 50 기본 점수

    // CS 관련 키워드면 가산점
    const csKeywords = ["lifo", "fifo", "후입선출", "선입선출", "자료구조", "데이터", "메모리", "포인터", "배열", "리스트"];
    if (csKeywords.some(kw => cleanWord.toLowerCase().includes(kw))) {
        baseScore += 20;
    }

    // 정답과 첫 글자가 같으면 가산점
    if (cleanWord[0]?.toLowerCase() === answerWord[0]?.toLowerCase()) {
        baseScore += 10;
    }

    // 점수 범위 제한 (5 ~ 89)
    const finalScore = Math.max(5, Math.min(89, baseScore));

    return {
        userWord: cleanWord,
        answerWord: answerWord,
        similarity: finalScore / 100,
        score: parseFloat(finalScore.toFixed(2)),
        isCorrect: false
    };
};

/**
 * 오늘의 문제 정보 반환 (정답은 제외)
 * @returns {Object} 문제 정보
 */
export const getDailyProblem = () => {
    return {
        id: DAILY_PROBLEM.id,
        category: DAILY_PROBLEM.category,
        hints: DAILY_PROBLEM.hints,
    };
};

/**
 * 어제의 정답 반환
 * @returns {Object} 어제의 문제 정보 (정답 포함)
 */
export const getYesterdayAnswer = () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    // 어제 날짜를 시드로 사용
    const seed = yesterday.getFullYear() * 10000 + (yesterday.getMonth() + 1) * 100 + yesterday.getDate();
    const index = seed % WORDS.length;
    const yesterdayWord = WORDS[index];

    return {
        answer: yesterdayWord.answer,
        category: yesterdayWord.category,
    };
};

/**
 * 오늘의 문제 카테고리만 반환 (위젯용)
 * @returns {Object} 카테고리와 힌트 수
 */
export const getTodayQuizInfo = () => {
    return {
        category: DAILY_PROBLEM.category,
        hintCount: DAILY_PROBLEM.hints.length,
    };
};
