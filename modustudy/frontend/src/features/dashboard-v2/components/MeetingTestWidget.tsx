// 미팅 테스트 위젯: 미팅 후 AI 자동 생성 퀴즈 목록 표시
// 대시보드에서 미팅 기반 퀴즈를 빠르게 확인하고 풀 수 있는 위젯

import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    ClipboardCheck,
    Calendar,
    Play,
    CheckCircle2,
    Circle,
    ChevronRight,
    XCircle,
    Send,
    RotateCcw,
    Trophy,
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { WidgetHeader, WidgetContainer } from '@/shared/components/layouts';
import { studyApi } from '@/api/endpoints/studyApi';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import {
    studyQuizApi,
    type StudyQuizListItem,
    type StudyQuizDetail,
    type StudyQuizSubmitResponse,
} from '@/api/endpoints/studyQuizApi';

// 위젯용 퀴즈 아이템
interface MeetingQuizItem {
    quiz: StudyQuizListItem;
    studyId: number;
    studyName: string;
}

type ViewMode = 'list' | 'quiz';

/** options JSON 문자열을 파싱 */
const parseOptions = (optionsStr: string | null): { id: string; text: string }[] => {
    if (!optionsStr) return [];
    try {
        const parsed = JSON.parse(optionsStr);
        if (Array.isArray(parsed)) {
            return parsed.map((opt, idx) => {
                if (typeof opt === 'string') {
                    return { id: String.fromCharCode(65 + idx), text: opt };
                }
                if (typeof opt === 'object' && opt !== null) {
                    return {
                        id: opt.id || String.fromCharCode(65 + idx),
                        text: opt.text || opt.label || String(opt.id || ''),
                    };
                }
                return { id: String.fromCharCode(65 + idx), text: String(opt) };
            });
        }
        return [];
    } catch {
        return [];
    }
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
    const [userAnswer, setUserAnswer] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [result, setResult] = useState<StudyQuizSubmitResponse | null>(null);
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
                                if (meetings.some((m: any) => m.status === 'ENDED')) {
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
            setUserAnswer('');
            setSubmitted(false);
            setResult(null);
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

    // 답안 제출
    const handleSubmit = useCallback(async () => {
        if (!quizDetail || !selectedItem || !userAnswer.trim()) return;
        const question = quizDetail.questions[currentIndex];
        const responseTimeMs = Date.now() - startTime;

        setSubmitting(true);
        try {
            const res = await studyQuizApi.submitAnswer(
                selectedItem.studyId, quizDetail.id, question.id, { userAnswer, responseTimeMs }
            );
            setResult(res);
            setSubmitted(true);
            if (res.isCorrect) setCorrectCount(prev => prev + 1);
        } catch {
            // 제출 실패 시 로컬 비교
            const isCorrect = userAnswer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase();
            setResult({ isCorrect, correctAnswer: question.correctAnswer, explanation: question.explanation } as StudyQuizSubmitResponse);
            setSubmitted(true);
            if (isCorrect) setCorrectCount(prev => prev + 1);
        } finally {
            setSubmitting(false);
        }
    }, [quizDetail, selectedItem, currentIndex, userAnswer, startTime]);

    // 다음 문제
    const handleNext = useCallback(() => {
        if (!quizDetail) return;
        if (currentIndex + 1 >= quizDetail.questions.length) {
            setIsComplete(true);
        } else {
            setCurrentIndex(prev => prev + 1);
            setUserAnswer('');
            setSubmitted(false);
            setResult(null);
            setStartTime(Date.now());
        }
    }, [quizDetail, currentIndex]);

    // 다시 풀기
    const handleRetry = useCallback(() => {
        setCurrentIndex(0);
        setUserAnswer('');
        setSubmitted(false);
        setResult(null);
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
                    title="미팅 테스트"
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
                title={viewMode === 'list' ? '미팅 테스트' : quizDetail?.title || ''}
                subtitle={viewMode === 'list' ? '미팅 후 자동 생성 퀴즈' : selectedItem?.studyName}
                showBackButton={viewMode === 'quiz'}
                onBack={handleBackToList}
                maximizePath="/meeting-test"
                rightActions={
                    viewMode === 'quiz' && correctCount > 0 ? (
                        <div className="text-sm font-bold text-text-primary">
                            {correctCount} / {currentIndex + (submitted ? 1 : 0)}
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
                                            <p className="text-text-secondary">아직 미팅 테스트가 없습니다</p>
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
                                                'w-full p-4 rounded-xl border text-left transition-colors',
                                                'hover:border-accent/30 hover:bg-gray-50/50',
                                                'border-gray-100'
                                            )}
                                        >
                                            <div className="flex items-start justify-between gap-3">
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-center gap-2 mb-1">
                                                        <Circle className="text-gray-300 flex-shrink-0" size={18} />
                                                        <h4 className="font-bold text-text-primary truncate mb-0">
                                                            {item.quiz.title}
                                                        </h4>
                                                    </div>
                                                    <p className="text-sm text-text-secondary ml-6 truncate">
                                                        {item.studyName}
                                                    </p>
                                                    <div className="flex items-center gap-3 mt-2 ml-6">
                                                        <span className="text-xs text-text-tertiary flex items-center gap-1">
                                                            <Calendar size={12} />
                                                            {item.quiz.createdAt.split('T')[0]}
                                                        </span>
                                                        <span className="text-xs text-text-tertiary">
                                                            {item.quiz.questionCount}문제
                                                        </span>
                                                    </div>
                                                </div>
                                                <div className="flex-shrink-0 p-2 rounded-lg bg-accent/10">
                                                    <Play size={16} className="text-accent" />
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
                                const options = parseOptions(question.options);
                                const progress = ((currentIndex + 1) / quizDetail.questions.length) * 100;

                                return (
                                    <>
                                        {/* 진행률 */}
                                        <div className="w-full bg-gray-100 rounded-full h-1.5 mb-5">
                                            <motion.div
                                                className="h-1.5 bg-accent rounded-full"
                                                initial={{ width: 0 }}
                                                animate={{ width: `${progress}%` }}
                                                transition={{ duration: 0.3 }}
                                            />
                                        </div>

                                        {/* 문제 번호 */}
                                        <div className="flex items-center gap-2 mb-3">
                                            <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-accent text-white">
                                                Q{currentIndex + 1}
                                            </span>
                                            <span className="text-xs text-text-tertiary">
                                                {question.questionType === 'MULTIPLE_CHOICE' ? '객관식' : '단답형'}
                                            </span>
                                            <span className="text-xs text-text-tertiary ml-auto">
                                                {currentIndex + 1} / {quizDetail.questions.length}
                                            </span>
                                        </div>

                                        {/* 문제 텍스트 */}
                                        <p className="text-sm font-medium text-text-primary mb-4 leading-relaxed">
                                            {question.questionText}
                                        </p>

                                        {/* 선택지 / 입력 */}
                                        {question.questionType === 'MULTIPLE_CHOICE' && options.length > 0 ? (
                                            <div className="space-y-2">
                                                {options.map(opt => {
                                                    const isSelected = userAnswer === opt.id;
                                                    const showCorrect = submitted && result && opt.id === result.correctAnswer;
                                                    const showWrong = submitted && result && isSelected && !result.isCorrect;

                                                    return (
                                                        <button
                                                            key={opt.id}
                                                            onClick={() => !submitted && setUserAnswer(opt.id)}
                                                            disabled={submitted}
                                                            className={cn(
                                                                'w-full flex items-center gap-2.5 px-3.5 py-3 rounded-xl text-left transition-all',
                                                                'border-2 text-sm',
                                                                !submitted && conditionalClasses.state(
                                                                    isSelected,
                                                                    'border-accent bg-accent/5',
                                                                    'border-gray-100 hover:border-accent/40'
                                                                ),
                                                                submitted && showCorrect && 'border-secondary bg-secondary/5',
                                                                submitted && showWrong && 'border-error bg-error/5',
                                                                submitted && !showCorrect && !showWrong && 'border-gray-100 opacity-50',
                                                                submitted && 'cursor-default'
                                                            )}
                                                        >
                                                            {submitted && showCorrect ? (
                                                                <CheckCircle2 size={16} className="text-secondary flex-shrink-0" />
                                                            ) : submitted && showWrong ? (
                                                                <XCircle size={16} className="text-error flex-shrink-0" />
                                                            ) : (
                                                                <Circle size={16} className={cn('flex-shrink-0', isSelected ? 'text-accent' : 'text-gray-300')} />
                                                            )}
                                                            <span className={cn(
                                                                'font-medium',
                                                                submitted && showCorrect && 'text-secondary-dark',
                                                                submitted && showWrong && 'text-error',
                                                                !submitted && isSelected && 'text-accent',
                                                                !submitted && !isSelected && 'text-text-secondary'
                                                            )}>
                                                                {opt.id}. {opt.text}
                                                            </span>
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        ) : (
                                            <input
                                                type="text"
                                                value={userAnswer}
                                                onChange={e => !submitted && setUserAnswer(e.target.value)}
                                                onKeyDown={e => e.key === 'Enter' && !submitted && userAnswer.trim() && handleSubmit()}
                                                disabled={submitted}
                                                placeholder="답을 입력하세요..."
                                                className={cn(
                                                    'w-full px-3.5 py-2.5 text-sm rounded-xl transition-all',
                                                    'border-2 border-gray-200 bg-gray-50',
                                                    'focus:border-accent focus:outline-none',
                                                    submitted && 'cursor-default opacity-80'
                                                )}
                                            />
                                        )}

                                        {/* 피드백 */}
                                        <AnimatePresence>
                                            {submitted && result && (
                                                <motion.div
                                                    initial={{ opacity: 0, height: 0 }}
                                                    animate={{ opacity: 1, height: 'auto' }}
                                                    exit={{ opacity: 0, height: 0 }}
                                                    className="mt-4"
                                                >
                                                    <div className={cn(
                                                        'rounded-xl p-3 border text-sm',
                                                        result.isCorrect
                                                            ? 'bg-secondary/5 border-secondary/30'
                                                            : 'bg-error/5 border-error/30'
                                                    )}>
                                                        <div className="flex items-center gap-2 mb-1">
                                                            {result.isCorrect ? (
                                                                <CheckCircle2 size={16} className="text-secondary" />
                                                            ) : (
                                                                <XCircle size={16} className="text-error" />
                                                            )}
                                                            <span className={cn(
                                                                'font-semibold',
                                                                result.isCorrect ? 'text-secondary-dark' : 'text-error'
                                                            )}>
                                                                {result.isCorrect ? '정답!' : '오답'}
                                                            </span>
                                                        </div>
                                                        {!result.isCorrect && (
                                                            <p className="text-text-secondary ml-6">
                                                                정답: <span className="font-medium text-secondary">{result.correctAnswer}</span>
                                                            </p>
                                                        )}
                                                        {result.explanation && (
                                                            <p className="text-text-secondary ml-6 mt-1">{result.explanation}</p>
                                                        )}
                                                    </div>
                                                </motion.div>
                                            )}
                                        </AnimatePresence>

                                        {/* 버튼 */}
                                        <div className="mt-5 flex justify-end">
                                            {!submitted ? (
                                                <button
                                                    onClick={handleSubmit}
                                                    disabled={!userAnswer.trim() || submitting}
                                                    className={cn(
                                                        'inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                                        'bg-accent text-white hover:bg-accent/90',
                                                        'disabled:opacity-50 disabled:cursor-not-allowed'
                                                    )}
                                                >
                                                    {submitting ? <Spinner size="sm" /> : <Send size={14} />}
                                                    제출
                                                </button>
                                            ) : (
                                                <button
                                                    onClick={handleNext}
                                                    className={cn(
                                                        'inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                                        'bg-accent text-white hover:bg-accent/90'
                                                    )}
                                                >
                                                    {currentIndex + 1 >= quizDetail.questions.length ? '결과 보기' : '다음'}
                                                    <ChevronRight size={14} />
                                                </button>
                                            )}
                                        </div>
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
