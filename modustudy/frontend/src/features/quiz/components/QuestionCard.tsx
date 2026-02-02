/**
 * =============================================================================
 * QuestionCard.tsx - 문제 표시 카드 컴포넌트
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 세 가지 유형의 문제를 표시하고 답변을 입력받습니다:
 * - 단일선택 (single-choice): 라디오 버튼
 * - 다중선택 (multiple-choice): 체크박스
 * - 단답형 (short-answer): 텍스트 입력
 * 
 * =============================================================================
 */

import { useState, useEffect } from 'react';
import { QuizQuestion } from '../types/QuizQuestion.types';
import { cn } from '@/shared/utils/cn';
import { CheckCircle2, Circle, Square, CheckSquare } from 'lucide-react';

// -----------------------------------------------------------------------------
// Props 인터페이스
// -----------------------------------------------------------------------------
interface QuestionCardProps {
    /** 문제 데이터 */
    question: QuizQuestion;
    /** 문제 번호 (1-based) */
    questionNumber: number;
    /** 현재 선택된 답안 */
    currentAnswer?: string | string[];
    /** 답안 변경 핸들러 */
    onAnswerChange: (answer: string | string[]) => void;
    /** 추가 CSS 클래스 */
    className?: string;
}

// -----------------------------------------------------------------------------
// SQL 키워드 목록 (하이라이팅용)
// -----------------------------------------------------------------------------
const SQL_KEYWORDS = [
    'SELECT', 'FROM', 'WHERE', 'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'DROP',
    'ALTER', 'TABLE', 'INDEX', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER',
    'ON', 'AND', 'OR', 'NOT', 'IN', 'BETWEEN', 'LIKE', 'IS', 'NULL',
    'ORDER', 'BY', 'GROUP', 'HAVING', 'LIMIT', 'OFFSET', 'DISTINCT',
    'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'AS', 'INTO', 'VALUES', 'SET',
    'PRIMARY', 'KEY', 'FOREIGN', 'REFERENCES', 'CONSTRAINT', 'DEFAULT',
    'UNION', 'ALL', 'EXISTS', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
    'ASC', 'DESC', 'TRUNCATE', 'VARCHAR', 'INT', 'INTEGER', 'BOOLEAN',
    'DATE', 'DATETIME', 'TIMESTAMP', 'TEXT', 'FLOAT', 'DOUBLE', 'DECIMAL'
];

// -----------------------------------------------------------------------------
// 문제 텍스트 파싱 및 렌더링 함수
// -----------------------------------------------------------------------------

/**
 * 코드 블록 내 SQL 키워드 하이라이팅
 */
const highlightSqlKeywords = (code: string): React.ReactNode[] => {
    // 키워드 패턴 생성 (대소문자 무시)
    const keywordPattern = new RegExp(
        `\\b(${SQL_KEYWORDS.join('|')})\\b`,
        'gi'
    );

    const parts = code.split(keywordPattern);

    return parts.map((part, index) => {
        const isKeyword = SQL_KEYWORDS.some(
            kw => kw.toLowerCase() === part.toLowerCase()
        );

        if (isKeyword) {
            return (
                <span
                    key={index}
                    style={{
                        color: 'var(--color-primary)',
                        fontWeight: 600,
                    }}
                >
                    {part.toUpperCase()}
                </span>
            );
        }

        // 빈칸(____) 강조
        if (part.includes('______') || part.includes('____')) {
            const blankParts = part.split(/(_{4,})/g);
            return blankParts.map((blankPart, blankIndex) => {
                if (/^_{4,}$/.test(blankPart)) {
                    return (
                        <span
                            key={`${index}-${blankIndex}`}
                            style={{
                                backgroundColor: 'var(--color-warning-light)',
                                color: 'var(--color-warning-dark)',
                                padding: '2px 8px',
                                borderRadius: '4px',
                                fontWeight: 700,
                            }}
                        >
                            ?
                        </span>
                    );
                }
                return blankPart;
            });
        }

        return part;
    });
};

/**
 * 문제 텍스트를 파싱하여 코드 블록을 분리하고 스타일링
 * - 작은따옴표('...')로 감싸진 부분 → 코드 블록
 * - 빈칸(______) → 강조 표시
 */
const parseQuestionText = (text: string): React.ReactNode => {
    // 작은따옴표로 감싸진 코드 블록 찾기
    const codeBlockPattern = /'([^']+)'/g;
    const parts: React.ReactNode[] = [];
    let lastIndex = 0;
    let match;
    let keyIndex = 0;

    while ((match = codeBlockPattern.exec(text)) !== null) {
        // 코드 블록 이전 텍스트
        if (match.index > lastIndex) {
            const beforeText = text.slice(lastIndex, match.index);
            parts.push(
                <span key={`text-${keyIndex++}`}>
                    {beforeText}
                </span>
            );
        }

        // 코드 블록 (SQL 하이라이팅 적용)
        const codeContent = match[1];
        parts.push(
            <code
                key={`code-${keyIndex++}`}
                style={{
                    display: 'block',
                    backgroundColor: 'var(--color-gray-900)',
                    color: 'var(--color-gray-100)',
                    padding: '12px 16px',
                    borderRadius: '8px',
                    fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
                    fontSize: '0.9em',
                    lineHeight: 1.6,
                    marginTop: '12px',
                    marginBottom: '8px',
                    overflowX: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                }}
            >
                {highlightSqlKeywords(codeContent)}
            </code>
        );

        lastIndex = match.index + match[0].length;
    }

    // 남은 텍스트
    if (lastIndex < text.length) {
        const remainingText = text.slice(lastIndex);
        // 남은 텍스트에서도 빈칸 처리
        if (remainingText.includes('______') || remainingText.includes('____')) {
            const blankParts = remainingText.split(/(_{4,})/g);
            parts.push(
                <span key={`text-${keyIndex++}`}>
                    {blankParts.map((part, idx) => {
                        if (/^_{4,}$/.test(part)) {
                            return (
                                <span
                                    key={`blank-${idx}`}
                                    style={{
                                        backgroundColor: 'var(--color-warning-light)',
                                        color: 'var(--color-warning-dark)',
                                        padding: '2px 12px',
                                        borderRadius: '4px',
                                        fontWeight: 700,
                                        margin: '0 4px',
                                    }}
                                >
                                    ?
                                </span>
                            );
                        }
                        return part;
                    })}
                </span>
            );
        } else {
            parts.push(
                <span key={`text-${keyIndex++}`}>
                    {remainingText}
                </span>
            );
        }
    }

    // 코드 블록이 없으면 빈칸만 처리
    if (parts.length === 0) {
        if (text.includes('______') || text.includes('____')) {
            const blankParts = text.split(/(_{4,})/g);
            return (
                <>
                    {blankParts.map((part, idx) => {
                        if (/^_{4,}$/.test(part)) {
                            return (
                                <span
                                    key={`blank-${idx}`}
                                    style={{
                                        backgroundColor: 'var(--color-warning-light)',
                                        color: 'var(--color-warning-dark)',
                                        padding: '2px 12px',
                                        borderRadius: '4px',
                                        fontWeight: 700,
                                        margin: '0 4px',
                                    }}
                                >
                                    ?
                                </span>
                            );
                        }
                        return part;
                    })}
                </>
            );
        }
        return text;
    }

    return <>{parts}</>;
};

// -----------------------------------------------------------------------------
// 난이도 배지 색상 매핑
// -----------------------------------------------------------------------------
const difficultyColors: Record<string, { bg: string; text: string }> = {
    Easy: {
        bg: 'var(--color-google-green-light)',
        text: 'var(--color-google-green)',
    },
    Medium: {
        bg: 'var(--color-google-yellow-light)',
        text: 'var(--color-google-yellow)',
    },
    Hard: {
        bg: 'var(--color-google-red-light)',
        text: 'var(--color-google-red)',
    },
};

// -----------------------------------------------------------------------------
// 문제 유형 한글 라벨
// -----------------------------------------------------------------------------
const typeLabels: Record<string, string> = {
    'single-choice': '단일선택',
    'multiple-choice': '다중선택',
    'short-answer': '단답형',
};

// -----------------------------------------------------------------------------
// 컴포넌트
// -----------------------------------------------------------------------------
export const QuestionCard: React.FC<QuestionCardProps> = ({
    question,
    questionNumber,
    currentAnswer,
    onAnswerChange,
    className,
}) => {
    // 단답형 입력 상태
    const [textInput, setTextInput] = useState('');

    // 답안이 변경되면 입력 상태 동기화
    useEffect(() => {
        if (question.type === 'short-answer') {
            if (typeof currentAnswer === 'string') {
                setTextInput(currentAnswer);
            } else if (currentAnswer === undefined || currentAnswer === null) {
                setTextInput('');
            }
        }
    }, [currentAnswer, question.type]);

    // 단일선택 핸들러
    const handleSingleChoice = (optionId: string) => {
        onAnswerChange(optionId);
    };

    // 다중선택 핸들러
    const handleMultipleChoice = (optionId: string) => {
        const currentAnswers = Array.isArray(currentAnswer) ? currentAnswer : [];
        const isSelected = currentAnswers.includes(optionId);

        if (isSelected) {
            // 선택 해제
            onAnswerChange(currentAnswers.filter(id => id !== optionId));
        } else {
            // 선택 추가
            onAnswerChange([...currentAnswers, optionId]);
        }
    };

    // 단답형 입력 핸들러
    const handleTextInput = (value: string) => {
        setTextInput(value);
        onAnswerChange(value);
    };

    // 선택지가 선택되었는지 확인
    const isOptionSelected = (optionId: string): boolean => {
        if (question.type === 'single-choice') {
            return currentAnswer === optionId;
        }
        if (question.type === 'multiple-choice') {
            return Array.isArray(currentAnswer) && currentAnswer.includes(optionId);
        }
        return false;
    };

    const difficultyColor = difficultyColors[question.difficulty];

    return (
        <div
            className={cn(
                'rounded-2xl p-6 sm:p-8',
                className
            )}
            style={{
                backgroundColor: 'var(--color-surface)',
                boxShadow: 'var(--shadow-md)',
            }}
        >
            {/* 헤더: 문제 번호, 유형, 난이도 */}
            <div className="flex flex-wrap items-center gap-2 mb-4">
                {/* 문제 번호 */}
                <span
                    className="px-3 py-1 rounded-full text-sm font-bold"
                    style={{
                        backgroundColor: 'var(--color-primary)',
                        color: 'white',
                    }}
                >
                    Q{questionNumber}
                </span>

                {/* 문제 유형 */}
                <span
                    className="px-3 py-1 rounded-full text-xs font-medium"
                    style={{
                        backgroundColor: 'var(--color-gray-100)',
                        color: 'var(--color-text-secondary)',
                    }}
                >
                    {typeLabels[question.type]}
                </span>

                {/* 난이도 */}
                <span
                    className="px-3 py-1 rounded-full text-xs font-medium"
                    style={{
                        backgroundColor: difficultyColor.bg,
                        color: difficultyColor.text,
                    }}
                >
                    {question.difficulty === 'Easy' ? '초급' : question.difficulty === 'Medium' ? '중급' : '고급'}
                </span>

                {/* 카테고리 */}
                <span
                    className="px-3 py-1 rounded-full text-xs font-medium ml-auto"
                    style={{
                        backgroundColor: 'var(--color-secondary-alpha-20)',
                        color: 'var(--color-secondary-dark)',
                    }}
                >
                    {question.category}
                </span>
            </div>

            {/* 문제 내용 */}
            <div
                className="text-lg sm:text-xl font-semibold mb-6 leading-relaxed"
                style={{ color: 'var(--color-text-primary)' }}
            >
                {parseQuestionText(question.question)}
            </div>

            {/* 답변 영역 */}
            <div className="space-y-3">
                {/* 단일선택 / 다중선택 */}
                {(question.type === 'single-choice' || question.type === 'multiple-choice') &&
                    question.options?.map((option) => {
                        const isSelected = isOptionSelected(option.id);
                        const Icon = question.type === 'single-choice'
                            ? (isSelected ? CheckCircle2 : Circle)
                            : (isSelected ? CheckSquare : Square);

                        return (
                            <button
                                key={option.id}
                                onClick={() =>
                                    question.type === 'single-choice'
                                        ? handleSingleChoice(option.id)
                                        : handleMultipleChoice(option.id)
                                }
                                className={cn(
                                    'w-full flex items-start gap-3 p-4 rounded-xl text-left transition-all duration-200',
                                    'border-2',
                                    'hover:scale-[1.01]'
                                )}
                                style={{
                                    backgroundColor: isSelected
                                        ? 'var(--color-primary-alpha-10)'
                                        : 'var(--color-surface)',
                                    borderColor: isSelected
                                        ? 'var(--color-primary)'
                                        : 'var(--color-border)',
                                }}
                            >
                                <Icon
                                    size={22}
                                    className="flex-shrink-0 mt-0.5"
                                    style={{
                                        color: isSelected
                                            ? 'var(--color-primary)'
                                            : 'var(--color-text-tertiary)',
                                    }}
                                />
                                <span
                                    className="font-medium"
                                    style={{
                                        color: isSelected
                                            ? 'var(--color-primary-dark)'
                                            : 'var(--color-text-primary)',
                                    }}
                                >
                                    {option.text}
                                </span>
                            </button>
                        );
                    })}

                {/* 단답형 */}
                {question.type === 'short-answer' && (
                    <div>
                        <input
                            type="text"
                            value={textInput}
                            onChange={(e) => handleTextInput(e.target.value)}
                            placeholder="답을 입력하세요..."
                            className="w-full px-4 py-3 rounded-xl text-base font-medium transition-all duration-200 focus:outline-none focus:ring-2"
                            style={{
                                backgroundColor: 'var(--color-background-secondary)',
                                color: 'var(--color-text-primary)',
                                border: '2px solid var(--color-border)',
                            }}
                            onFocus={(e) => {
                                e.target.style.borderColor = 'var(--color-primary)';
                                e.target.style.backgroundColor = 'var(--color-surface)';
                            }}
                            onBlur={(e) => {
                                e.target.style.borderColor = 'var(--color-border)';
                                e.target.style.backgroundColor = 'var(--color-background-secondary)';
                            }}
                        />
                        <p
                            className="mt-2 text-sm"
                            style={{ color: 'var(--color-text-tertiary)' }}
                        >
                            대소문자 구분 없이 정답이 채점됩니다.
                        </p>
                    </div>
                )}
            </div>

            {/* 다중선택 안내 */}
            {question.type === 'multiple-choice' && (
                <p
                    className="mt-4 text-sm"
                    style={{ color: 'var(--color-text-tertiary)' }}
                >
                    💡 해당하는 항목을 모두 선택하세요.
                </p>
            )}
        </div>
    );
};
