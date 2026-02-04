/**
 * =============================================================================
 * quizUtils.ts - Centralized Quiz Utility Functions
 * =============================================================================
 *
 * 퀴즈 관련 변환 및 매핑 로직을 중앙화하여 일관된 채점을 보장합니다.
 * 
 * 주요 기능:
 * - 옵션 ID ↔ 인덱스 변환 (A ↔ 0, B ↔ 1, ...)
 * - 다양한 백엔드 정답 포맷 정규화
 * - ReviewItemDto → QuizQuestion 변환
 *
 * =============================================================================
 */

import type { ReviewItemDto } from '@/features/dashboard-v2/api/reviewApi';
import type { QuizQuestion } from '@/shared/components/QuizForm';

// =============================================================================
// Option ID ↔ Index Conversion
// =============================================================================

/**
 * 알파벳 옵션 ID를 숫자 인덱스로 변환합니다.
 * 
 * @example
 * optionIdToIndex("A") // → 0
 * optionIdToIndex("B") // → 1
 * optionIdToIndex(" c ") // → 2 (대소문자, 공백 무시)
 * optionIdToIndex("0") // → 0 (숫자 문자열도 지원)
 * optionIdToIndex("invalid") // → -1
 */
export const optionIdToIndex = (id: string): number => {
    const normalized = id.trim().toUpperCase();

    // 알파벳 단일 문자인 경우 (A-Z)
    if (normalized.length === 1 && normalized >= 'A' && normalized <= 'Z') {
        return normalized.charCodeAt(0) - 'A'.charCodeAt(0);
    }

    // 숫자인 경우
    const parsed = parseInt(id.trim(), 10);
    return isNaN(parsed) ? -1 : parsed;
};

/**
 * 숫자 인덱스를 알파벳 옵션 ID로 변환합니다.
 * 
 * @example
 * indexToOptionId(0) // → "A"
 * indexToOptionId(1) // → "B"
 * indexToOptionId(25) // → "Z"
 */
export const indexToOptionId = (index: number): string => {
    if (index < 0 || index > 25) {
        console.warn('[quizUtils] indexToOptionId: 범위 초과:', index);
        return String(index);
    }
    return String.fromCharCode(65 + index); // 65 = 'A'
};

// =============================================================================
// Correct Answer Normalization
// =============================================================================

/**
 * 다양한 백엔드 정답 포맷을 정규화합니다.
 * 
 * 지원하는 입력 포맷:
 * - 배열: ["A", "B"] → [0, 1]
 * - JSON 문자열: "[\"A\", \"B\"]" → [0, 1]
 * - 쉼표 구분 문자열: "A,B" → [0, 1]
 * - 단일 문자열: "A" → [0]
 * - 이미 숫자인 경우: [숫자]로 반환
 * 
 * @param answer - 백엔드에서 받은 정답 (다양한 형태)
 * @returns 정규화된 정답 인덱스 배열
 */
export const normalizeCorrectAnswer = (answer: unknown): number[] => {
    if (!answer) return [];

    let targetArray: string[] = [];

    // 이미 숫자 배열인 경우
    if (Array.isArray(answer) && answer.length > 0 && typeof answer[0] === 'number') {
        return answer as number[];
    }

    // 문자열 배열인 경우
    if (Array.isArray(answer)) {
        targetArray = answer.map(String);
    } else if (typeof answer === 'number') {
        // 숫자인 경우
        return [answer];
    } else if (typeof answer === 'string') {
        // 핵심 수정: JSON 형태의 대괄호와 따옴표를 모두 제거합니다.
        // "[\"A\", \"B\", \"D\"]" -> "A, B, D"
        const cleanStr = answer.replace(/[\[\]"']/g, '');
        targetArray = cleanStr.split(',').map(s => s.trim());
    }

    // 빈 값 필터링 및 인덱스 변환
    return targetArray
        .filter(s => s !== '')
        .map(s => optionIdToIndex(s))
        .filter(idx => idx !== -1);
};

// =============================================================================
// Quiz Question Transformation
// =============================================================================

/**
 * ReviewItemDto를 QuizQuestion 형식으로 변환합니다.
 * 
 * 주요 변환:
 * - options: OptionItem[] → string[] (텍스트만 추출)
 * - correctAnswer: "A,B" 문자열 → [0, 1] 숫자 배열
 * - difficulty: 숫자 → 'easy' | 'medium' | 'hard'
 */
export const transformToQuizQuestion = (item: ReviewItemDto): QuizQuestion => {
    const q = item.question;
    const isMultiple = q.questionType === 'MULTIPLE_CHOICE';
    const isMultipleAnswer = q.questionType === 'MULTIPLE_CHOICE_MULTIPLE';

    // options 파싱: [{ id: "A", text: "..." }] → ["..."]
    const options: string[] = q.options ? q.options.map(o => o.text) : [];

    // 정답 처리
    let correctAnswer: number | number[] | string = q.correctAnswer;

    if ((isMultiple || isMultipleAnswer) && q.options && q.options.length > 0) {
        // 디버깅 로그
        console.log('[transformToQuizQuestion] 변환 시작:', {
            questionType: q.questionType,
            rawCorrectAnswer: q.correctAnswer,
            optionIds: q.options.map(o => o.id)
        });

        // normalizeCorrectAnswer를 사용하여 통일된 방식으로 변환 (항상 number[] 반환)
        const normalized = normalizeCorrectAnswer(q.correctAnswer);
        console.log('[transformToQuizQuestion] normalizeCorrectAnswer 결과:', normalized);

        if (isMultipleAnswer) {
            // 복수 정답: 배열 그대로 사용
            correctAnswer = normalized;
        } else {
            // 단일 정답: 첫 번째 값 사용
            correctAnswer = normalized.length > 0 ? normalized[0] : q.correctAnswer;
        }

        console.log('[transformToQuizQuestion] 최종 correctAnswer:', correctAnswer);
    }

    return {
        id: item.reviewItemId,
        type: 'multiple',
        question: q.questionText,
        options: (isMultiple || isMultipleAnswer) ? options : undefined,
        correctAnswer,
        explanation: q.explanation || '',
        difficulty: item.difficulty < 3 ? 'easy' : item.difficulty < 7 ? 'medium' : 'hard',
        category: q.category || '',
    } as QuizQuestion;
};
