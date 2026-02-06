// 미팅 테스트 위젯: 미팅 후 AI 자동 생성 퀴즈 목록 표시
// 대시보드에서 미팅 기반 퀴즈를 빠르게 확인하고 풀 수 있는 위젯

import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    ClipboardCheck,
    Calendar,
    Play,
    RotateCcw,
    Trophy,
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { WidgetHeader, WidgetContainer } from '@/shared/components/layouts';
import {
    QuizSingleChoice,
    QuizShortAnswer,
    QuizProgress,
} from '@/shared/components';
import type { QuizQuestion } from '@/shared/components/QuizForm';
import { studyApi } from '@/api/endpoints/studyApi';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import {
    studyQuizApi,
    type StudyQuizListItem,
    type StudyQuizDetail,
    type StudyQuizQuestion,
} from '@/api/endpoints/studyQuizApi';
import {
    parseOptions as parseQuizOptions,
    indexToOptionId,
    normalizeCorrectAnswer,
} from '@/shared/utils/quizUtils';

// 위젯용 퀴즈 아이템
interface MeetingQuizItem {
    quiz: StudyQuizListItem;
    studyId: number;
    studyName: string;
}

type ViewMode = 'list' | 'quiz';

/** StudyQuizQuestion → QuizForm용 QuizQuestion으로 변환 */
const transformStudyQuizToQuizQuestion = (question: StudyQuizQuestion): QuizQuestion => {
    const isMultipleChoice = question.questionType === 'MULTIPLE_CHOICE';
    const parsedOptions = parseQuizOptions(question.options);

    // 객관식: "A" 같은 정답을 0-based 인덱스로 변환, 주관식: 원본 문자열 유지
    const correctAnswer = isMultipleChoice
        ? (normalizeCorrectAnswer(question.correctAnswer)[0] ?? question.correctAnswer)
        : question.correctAnswer;

    return {
        id: question.id,
        type: isMultipleChoice ? 'multiple' : 'short',
        question: question.questionText,
        options: isMultipleChoice ? parsedOptions.map(o => o.text) : undefined,
        correctAnswer,
        explanation: question.explanation || '',
    };
};

export const MeetingTestWidget: React.FC = () => {
    // 목록 상태
    const [quizItems, setQuizItems] = useState<MeetingQuizItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [hasEndedMeetings, setHasEndedMeetings] = useState(false);

    // 풀이 상태
    const [viewMode, setViewMode] = useState<ViewMode>('list');
    const [selectedItem, setSelectedItem] = useState<MeetingQuizItem | null>(null);
    const [quizDetail, setQuizDetail] = useState<StudyQuizDetail | null>(null);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
    const [shortAnswer, setShortAnswer] = useState('');
    const [showResult, setShowResult] = useState(false);
    const [correctCount, setCorrectCount] = useState(0);
    const [isComplete, setIsComplete] = useState(false);
    const [startTime, setStartTime] = useState(Date.now());

    // 마운트 시 미팅 퀴즈 목록 로드
    useEffect(() => {
        const loadData = async () => {
            try {
                setIsLoading(true);
                const studyResponse = await studyApi.getMyStudies(0, 20);
                const items: MeetingQuizItem[] = [];
                let foundEndedMeeting = false;

                for (const study of studyResponse.content) {
                    try {
                        // 종료된 미팅 존재 여부 확인
                        if (!foundEndedMeeting) {
                            try {
                                const meetingsPage = await meetingApi.listMeetings(study.id, { size: 5 });
                                const meetings = meetingsPage?.content || [];
                                if (meetings.some((m) => m.endedAt !== null)) {
                                    foundEndedMeeting = true;
                                }
                            } catch {
                                // 미팅 조회 실패는 무시
                            }
                        }

                        const quizzes = await studyQuizApi.getStudyQuizzes(study.id);
                        const meetingQuizzes = quizzes.filter(
                            q => q.sourceType === 'MEETING' && q.status === 'ACTIVE'
                        );
                        for (const quiz of meetingQuizzes) {
                            items.push({ quiz, studyId: study.id, studyName: study.name });
                        }
                    } catch (err) {
                        console.warn(`[MeetingTestWidget] 스터디 ${study.id} 퀴즈 조회 실패:`, err);
                    }
                }

                setHasEndedMeetings(foundEndedMeeting);

                // 최신순 정렬
                items.sort((a, b) =>
                    new Date(b.quiz.createdAt).getTime() - new Date(a.quiz.createdAt).getTime()
                );
                setQuizItems(items);
            } catch (err) {
                console.error('[MeetingTestWidget] 데이터 로딩 실패:', err);
            } finally {
                setIsLoading(false);
            }
        };
        loadData();
    }, []);

    // 퀴즈 선택 → 상세 로드 → 풀이 모드 진입
    const handleSelect = useCallback(async (item: MeetingQuizItem) => {
        try {
            setIsLoading(true);
            const detail = await studyQuizApi.getQuizDetail(item.studyId, item.quiz.id);
            setQuizDetail(detail);
            setSelectedItem(item);
            setViewMode('quiz');
            setCurrentIndex(0);
            setSelectedAnswer(null);
            setShortAnswer('');
            setShowResult(false);
            setCorrectCount(0);
            setIsComplete(false);
            setStartTime(Date.now());
        } catch (err) {
            console.error('[MeetingTestWidget] 퀴즈 상세 로딩 실패:', err);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const handleBackToList = () => {
        setViewMode('list');
        setSelectedItem(null);
        setQuizDetail(null);
    };

    // 객관식 답안 제출
    const handleSubmitMultiple = useCallback(async () => {
        if (!quizDetail || !selectedItem || selectedAnswer === null) return;
        const question = quizDetail.questions[currentIndex];
        const options = parseQuizOptions(question.options);
        const responseTimeMs = Date.now() - startTime;

        // 인덱스를 옵션 ID로 변환 (0 → "A", 1 → "B", ...)
        const optionId = options[selectedAnswer]?.id || indexToOptionId(selectedAnswer);

        let isCorrect = false;
        try {
            const res = await studyQuizApi.submitAnswer(
                selectedItem.studyId, quizDetail.id, question.id,
                { userAnswer: optionId, responseTimeMs }
            );
            isCorrect = res.isCorrect;
        } catch {
            // 제출 실패 시 로컬 비교
            const correctIdx = normalizeCorrectAnswer(question.correctAnswer);
            isCorrect = correctIdx.includes(selectedAnswer);
        }

        if (isCorrect) setCorrectCount(prev => prev + 1);
        setShowResult(true);
    }, [quizDetail, selectedItem, currentIndex, selectedAnswer, startTime]);

    // 주관식 답안 제출
    const handleSubmitShort = useCallback(async () => {
        if (!quizDetail || !selectedItem || !shortAnswer.trim()) return;
        const question = quizDetail.questions[currentIndex];
        const responseTimeMs = Date.now() - startTime;

        let isCorrect = false;
        try {
            const res = await studyQuizApi.submitAnswer(
                selectedItem.studyId, quizDetail.id, question.id,
                { userAnswer: shortAnswer, responseTimeMs }
            );
            isCorrect = res.isCorrect;
        } catch {
            isCorrect = shortAnswer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase();
        }

        if (isCorrect) setCorrectCount(prev => prev + 1);
        setShowResult(true);
    }, [quizDetail, selectedItem, currentIndex, shortAnswer, startTime]);

    // 다음 문제로 이동
    const handleNext = useCallback(() => {
        if (!quizDetail) return;
        if (currentIndex + 1 >= quizDetail.questions.length) {
            setIsComplete(true);
        } else {
            setCurrentIndex(prev => prev + 1);
            setSelectedAnswer(null);
            setShortAnswer('');
            setShowResult(false);
            setStartTime(Date.now());
        }
    }, [quizDetail, currentIndex]);

    // 다시 풀기
    const handleRetry = useCallback(() => {
        setCurrentIndex(0);
        setSelectedAnswer(null);
        setShortAnswer('');
        setShowResult(false);
        setCorrectCount(0);
        setIsComplete(false);
        setStartTime(Date.now());
    }, []);

    // 로딩
    if (isLoading && viewMode === 'list') {
        return (
            <WidgetContainer>
                <WidgetHeader
                    icon={ClipboardCheck}
                    iconColor="neutral"
                    title="미팅 퀴즈"
                    subtitle="미팅 후 자동 생성 퀴즈"
                    maximizePath="/meeting-test"
                />
                <div className="flex items-center justify-center h-64">
                    <Spinner variant="center" size="md" label="로딩 중..." />
                </div>
            </WidgetContainer>
        );
    }

    return (
        <WidgetContainer>
            <WidgetHeader
                icon={ClipboardCheck}
                iconColor="neutral"
                title={viewMode === 'list' ? '미팅 퀴즈' : quizDetail?.title || ''}
                subtitle={viewMode === 'list' ? '미팅 후 자동 생성 퀴즈' : selectedItem?.studyName}
                showBackButton={viewMode === 'quiz'}
                onBack={handleBackToList}
                maximizePath="/meeting-test"
                rightActions={
                    viewMode === 'quiz' && correctCount > 0 ? (
                        <div className="text-sm font-bold text-text-primary">
                            {correctCount} / {currentIndex + (showResult ? 1 : 0)}
                        </div>
                    ) : undefined
                }
            />

            <div className="p-6">
                <AnimatePresence mode="wait">
                    {viewMode === 'list' ? (
                        <motion.div
                            key="list"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                        >
                            {quizItems.length === 0 ? (
                                <div className="text-center py-12">
                                    {hasEndedMeetings ? (
                                        <>
                                            <Spinner variant="center" size="lg" />
                                            <p className="text-text-secondary mt-4">퀴즈를 생성하고 있습니다</p>
                                            <p className="text-sm text-text-tertiary mt-1">
                                                AI가 미팅 내용을 분석 중입니다
                                            </p>
                                        </>
                                    ) : (
                                        <>
                                            <ClipboardCheck className="mx-auto text-gray-300 mb-4" size={48} />
                                            <p className="text-text-secondary">아직 미팅 퀴즈가 없습니다</p>
                                            <p className="text-sm text-text-tertiary mt-1">
                                                스터디 미팅 후 AI가 자동으로 퀴즈를 생성합니다
                                            </p>
                                        </>
                                    )}
                                </div>
                            ) : (
                                <div className="max-h-[400px] overflow-y-auto space-y-3 pr-2">
                                    {quizItems.map(item => (
                                        <button
                                            key={item.quiz.id}
                                            onClick={() => handleSelect(item)}
                                            className={cn(
                                                'w-full p-4 rounded-xl bg-white text-left transition-all',
                                                'shadow-[0_4px_15px_rgba(0,0,0,0.05)]',
                                                'hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] hover:bg-gray-50'
                                            )}
                                        >
                                            <div className="flex items-start justify-between gap-3">
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-center gap-2 mb-2">
                                                        <span className="px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-700">
                                                            {item.quiz.questionCount}문제
                                                        </span>
                                                        <span className="text-xs text-text-tertiary bg-gray-100 px-2 py-0.5 rounded flex items-center gap-1">
                                                            <Calendar size={12} />
                                                            {item.quiz.createdAt.split('T')[0]}
                                                        </span>
                                                    </div>
                                                    <h4 className="font-medium text-text-primary line-clamp-2 text-sm leading-relaxed mb-1">
                                                        {item.quiz.title}
                                                    </h4>
                                                    <p className="text-xs text-text-secondary truncate">
                                                        {item.studyName}
                                                    </p>
                                                </div>
                                                <div className="flex-shrink-0 self-center">
                                                    <Play size={18} className="text-primary/70" />
                                                </div>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </motion.div>
                    ) : quizDetail && !isComplete ? (
                        <motion.div
                            key={`quiz-${currentIndex}`}
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -20 }}
                            transition={{ duration: 0.3 }}
                        >
                            {(() => {
                                const question = quizDetail.questions[currentIndex];
                                const currentQuiz = transformStudyQuizToQuizQuestion(question);

                                return (
                                    <>
                                        {/* 진행률 */}
                                        <QuizProgress
                                            current={currentIndex + 1}
                                            total={quizDetail.questions.length}
                                            className="pt-0 border-t-0 mb-4"
                                        />

                                        {/* 문제 풀이 - QuizForm 공유 컴포넌트 사용 (인라인 해설) */}
                                        {question.questionType === 'MULTIPLE_CHOICE' ? (
                                            <QuizSingleChoice
                                                quiz={currentQuiz}
                                                questionNumber={currentIndex + 1}
                                                selectedAnswer={selectedAnswer}
                                                showResult={showResult}
                                                onSelectAnswer={setSelectedAnswer}
                                                onSubmit={handleSubmitMultiple}
                                                onNext={handleNext}
                                                isLastQuestion={currentIndex + 1 >= quizDetail.questions.length}
                                            />
                                        ) : (
                                            <QuizShortAnswer
                                                quiz={currentQuiz}
                                                questionNumber={currentIndex + 1}
                                                userAnswer={shortAnswer}
                                                showResult={showResult}
                                                onChangeAnswer={(val) => setShortAnswer(val || '')}
                                                onSubmit={handleSubmitShort}
                                                onNext={handleNext}
                                                isLastQuestion={currentIndex + 1 >= quizDetail.questions.length}
                                            />
                                        )}
                                    </>
                                );
                            })()}
                        </motion.div>
                    ) : isComplete && quizDetail ? (
                        <motion.div
                            key="complete"
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="text-center py-8"
                        >
                            <div className="w-16 h-16 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-4">
                                <Trophy size={28} className="text-accent" />
                            </div>
                            <h3 className="text-lg font-bold text-text-primary mb-1">퀴즈 완료!</h3>
                            <p className="text-sm text-text-secondary mb-4">{quizDetail.title}</p>

                            <div className="flex justify-center gap-4 mb-6">
                                <div className="text-center px-4 py-3 rounded-xl bg-gray-50 border border-gray-100">
                                    <div className="text-xl font-bold text-text-primary">{quizDetail.questions.length}</div>
                                    <div className="text-xs text-text-tertiary">총 문제</div>
                                </div>
                                <div className="text-center px-4 py-3 rounded-xl bg-gray-50 border border-gray-100">
                                    <div className="text-xl font-bold text-secondary">{correctCount}</div>
                                    <div className="text-xs text-text-tertiary">정답</div>
                                </div>
                                <div className="text-center px-4 py-3 rounded-xl bg-gray-50 border border-gray-100">
                                    <div className="text-xl font-bold text-accent">
                                        {Math.round((correctCount / quizDetail.questions.length) * 100)}%
                                    </div>
                                    <div className="text-xs text-text-tertiary">정답률</div>
                                </div>
                            </div>

                            <div className="flex justify-center gap-3">
                                <button
                                    onClick={handleRetry}
                                    className={cn(
                                        'inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                        'border border-accent/30 text-accent hover:bg-accent/5'
                                    )}
                                >
                                    <RotateCcw size={14} />
                                    다시 풀기
                                </button>
                                <button
                                    onClick={handleBackToList}
                                    className={cn(
                                        'inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                        'bg-accent text-white hover:bg-accent/90'
                                    )}
                                >
                                    목록으로
                                </button>
                            </div>
                        </motion.div>
                    ) : null}
                </AnimatePresence>
            </div>
        </WidgetContainer>
    );
};
