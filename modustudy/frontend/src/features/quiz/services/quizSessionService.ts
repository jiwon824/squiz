/**
 * =============================================================================
 * quizSessionService.ts - 퀴즈 세션 관리 서비스
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 퀴즈 세션의 생성, 복원, 저장, 삭제를 관리합니다.
 * localStorage를 사용하여 임시 저장 기능을 제공하고,
 * Mock API를 통해 답안 동기화를 시뮬레이션합니다.
 * 
 * =============================================================================
 */

import { QuizSession, QuizAnswer, getSessionStorageKey } from '../types/QuizQuestion.types';
import { quizQuestions } from '../data/quizQuestionData';

// -----------------------------------------------------------------------------
// Fisher-Yates 셔플 알고리즘 (SHUFFLE ALGORITHM)
// -----------------------------------------------------------------------------
/**
 * shuffleArray - 배열을 무작위로 섞는 Fisher-Yates 알고리즘
 * 
 * 편향 없는 무작위 셔플을 보장합니다.
 * 
 * @param array - 원본 배열
 * @returns 새로운 셔플된 배열 (원본 보존)
 */
const shuffleArray = <T>(array: T[]): T[] => {
    const shuffled = [...array];
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
};

// -----------------------------------------------------------------------------
// 고유 ID 생성 (UNIQUE ID GENERATOR)
// -----------------------------------------------------------------------------
/**
 * generateSessionId - 세션 고유 ID 생성
 * 
 * 타임스탬프와 랜덤 문자열을 조합하여 고유한 세션 ID를 생성합니다.
 * 
 * @returns 고유 세션 ID 문자열
 */
const generateSessionId = (): string => {
    return `session_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
};

// -----------------------------------------------------------------------------
// 세션 조회 (GET STORED SESSION)
// -----------------------------------------------------------------------------
/**
 * getStoredSession - 저장된 세션 조회
 * 
 * localStorage에서 임시 저장된 세션을 조회합니다.
 * 
 * @param sectionId - 섹션 ID
 * @returns 저장된 세션 또는 null
 */
export const getStoredSession = (sectionId: string): QuizSession | null => {
    try {
        const key = getSessionStorageKey(sectionId);
        const stored = localStorage.getItem(key);
        if (stored) {
            return JSON.parse(stored) as QuizSession;
        }
        return null;
    } catch (error) {
        console.error('[QuizSession] 세션 조회 실패:', error);
        return null;
    }
};

// -----------------------------------------------------------------------------
// 새 세션 생성 (CREATE NEW SESSION)
// -----------------------------------------------------------------------------
/**
 * createNewSession - 새 퀴즈 세션 생성
 * 
 * 30개의 문제를 셔플하여 새로운 세션을 생성합니다.
 * 생성된 세션은 자동으로 localStorage에 저장됩니다.
 * 
 * @param sectionId - 섹션 ID
 * @returns 새로 생성된 퀴즈 세션
 */
export const createNewSession = (sectionId: string): QuizSession => {
    // 문제 ID 목록을 셔플
    const questionIds = quizQuestions.map(q => q.id);
    const shuffledIds = shuffleArray(questionIds);

    const now = Date.now();
    const session: QuizSession = {
        sessionId: generateSessionId(),
        sectionId,
        questionOrder: shuffledIds,
        answers: {},
        currentIndex: 0,
        startedAt: now,
        lastSavedAt: now,
    };

    // 세션 저장
    saveSession(session);

    return session;
};

// -----------------------------------------------------------------------------
// 세션 복원 또는 생성 (RESUME OR CREATE SESSION)
// -----------------------------------------------------------------------------
/**
 * resumeOrCreateSession - 세션 복원 또는 새 세션 생성
 * 
 * 저장된 세션이 있으면 복원하고, 없으면 새 세션을 생성합니다.
 * 
 * @param sectionId - 섹션 ID
 * @returns 복원 또는 새로 생성된 세션과 복원 여부 플래그
 */
export const resumeOrCreateSession = (sectionId: string): { session: QuizSession; isResumed: boolean } => {
    const storedSession = getStoredSession(sectionId);

    if (storedSession) {
        return { session: storedSession, isResumed: true };
    }

    const newSession = createNewSession(sectionId);
    return { session: newSession, isResumed: false };
};

// -----------------------------------------------------------------------------
// 세션 저장 (SAVE SESSION)
// -----------------------------------------------------------------------------
/**
 * saveSession - 세션을 localStorage에 저장
 * 
 * @param session - 저장할 세션 객체
 */
export const saveSession = (session: QuizSession): void => {
    try {
        const key = getSessionStorageKey(session.sectionId);
        const updatedSession: QuizSession = {
            ...session,
            lastSavedAt: Date.now(),
        };
        localStorage.setItem(key, JSON.stringify(updatedSession));
    } catch (error) {
        console.error('[QuizSession] 세션 저장 실패:', error);
    }
};

// -----------------------------------------------------------------------------
// 세션 삭제 (CLEAR SESSION)
// -----------------------------------------------------------------------------
/**
 * clearSession - 저장된 세션 삭제
 * 
 * @param sectionId - 섹션 ID
 */
export const clearSession = (sectionId: string): void => {
    try {
        const key = getSessionStorageKey(sectionId);
        localStorage.removeItem(key);
    } catch (error) {
        console.error('[QuizSession] 세션 삭제 실패:', error);
    }
};

// -----------------------------------------------------------------------------
// 답안 업데이트 (UPDATE ANSWER)
// -----------------------------------------------------------------------------
/**
 * updateAnswer - 세션의 답안 업데이트
 * 
 * @param session - 현재 세션
 * @param questionId - 문제 ID
 * @param answer - 사용자 답안
 * @returns 업데이트된 세션
 */
export const updateAnswer = (
    session: QuizSession,
    questionId: string,
    answer: string | string[]
): QuizSession => {
    const quizAnswer: QuizAnswer = {
        questionId,
        answer,
        timestamp: Date.now(),
    };

    const updatedSession: QuizSession = {
        ...session,
        answers: {
            ...session.answers,
            [questionId]: quizAnswer,
        },
    };

    // 자동 저장
    saveSession(updatedSession);

    return updatedSession;
};

// -----------------------------------------------------------------------------
// 현재 인덱스 업데이트 (UPDATE CURRENT INDEX)
// -----------------------------------------------------------------------------
/**
 * updateCurrentIndex - 현재 문제 인덱스 업데이트
 * 
 * @param session - 현재 세션
 * @param newIndex - 새 인덱스
 * @returns 업데이트된 세션
 */
export const updateCurrentIndex = (session: QuizSession, newIndex: number): QuizSession => {
    const updatedSession: QuizSession = {
        ...session,
        currentIndex: newIndex,
    };

    // 자동 저장
    saveSession(updatedSession);

    return updatedSession;
};

// -----------------------------------------------------------------------------
// Mock API: 답안 동기화 (SYNC ANSWERS - MOCK API)
// -----------------------------------------------------------------------------
/**
 * syncAnswerToServer - 답안을 서버에 동기화 (Mock API)
 * 
 * 실제 API 호출을 시뮬레이션하여 200ms 딜레이 후 성공을 반환합니다.
 * 
 * @param sessionId - 세션 ID
 * @param answer - 동기화할 답안
 * @returns Promise<boolean> - 성공 여부
 */
export const syncAnswerToServer = async (
    _sessionId: string,
    _answer: QuizAnswer
): Promise<boolean> => {
    // Mock API: 200ms 딜레이
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve(true);
        }, 200);
    });
};

// -----------------------------------------------------------------------------
// Mock API: 세션 완료 (COMPLETE SESSION - MOCK API)
// -----------------------------------------------------------------------------
/**
 * completeSession - 세션 완료 처리 (Mock API)
 * 
 * 퀴즈 완료 시 서버에 전체 결과를 전송합니다.
 * 
 * @param session - 완료할 세션
 * @returns Promise<{ success: boolean; score: number }> - 결과
 */
export const completeSession = async (
    session: QuizSession
): Promise<{ success: boolean; score: number }> => {
    // Mock API: 500ms 딜레이
    return new Promise((resolve) => {
        setTimeout(() => {
            const totalQuestions = session.questionOrder.length;
            const answeredQuestions = Object.keys(session.answers).length;
            const score = Math.round((answeredQuestions / totalQuestions) * 100);

            // 세션 삭제 (완료했으므로)
            clearSession(session.sectionId);

            resolve({ success: true, score });
        }, 500);
    });
};
