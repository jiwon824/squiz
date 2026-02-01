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
            <h3
                className="text-lg sm:text-xl font-semibold mb-6 leading-relaxed"
                style={{ color: 'var(--color-text-primary)' }}
            >
                {question.question}
            </h3>

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
