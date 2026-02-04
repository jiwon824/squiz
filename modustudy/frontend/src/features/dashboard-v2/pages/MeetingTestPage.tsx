// 미팅 테스트 전체화면 페이지
// STTReportPage와 동일한 레이아웃 스타일 (사이드바 + 콘텐츠)

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    ClipboardCheck,
    Calendar,
    Search,
    BookOpen,
    ChevronDown,
    Circle,
    CheckCircle2,
    XCircle,
    ChevronRight,
    Send,
    RotateCcw,
    Trophy,
    Sparkles,
} from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { PageNavHeader } from '@/shared/components/layouts';
import { studyApi } from '@/api/endpoints/studyApi';
import {
    studyQuizApi,
    type StudyQuizListItem,
    type StudyQuizDetail,
    type StudyQuizSubmitResponse,
} from '@/api/endpoints/studyQuizApi';
interface StudyOption {
    id: number;
    name: string;
}

interface MeetingQuizItem {
    quiz: StudyQuizListItem;
    studyId: number;
    studyName: string;
}

/** options JSON 문자열을 파싱 */
const parseOptions = (optionsStr: string | null): { id: string; text: string }[] => {
    if (!optionsStr) return [];
    try {
        const parsed = JSON.parse(optionsStr);
        if (Array.isArray(parsed)) {
            return parsed.map((opt, idx) => {
                const defaultId = String.fromCharCode(65 + idx);
                if (typeof opt === 'string') {
                    // 이미 "A. " 또는 "A." 형태의 prefix가 있는지 확인
                    const prefixMatch = opt.match(/^([A-Z])\.[\s]*(.*)/);
                    if (prefixMatch) {
                        // prefix가 있으면 ID는 추출, text는 prefix 제거된 내용만
                        return { id: prefixMatch[1], text: prefixMatch[2] };
                    }
                    return { id: defaultId, text: opt };
                }
                if (typeof opt === 'object' && opt !== null) {
                    const id = opt.id || defaultId;
                    let text = opt.text || opt.label || String(opt.id || '');
                    // 객체의 text도 prefix 중복 확인
                    const textMatch = text.match(/^([A-Z])\.[\s]*(.*)/);
                    if (textMatch) {
                        text = textMatch[2];
                    }
                    return { id, text };
                }
                return { id: defaultId, text: String(opt) };
            });
        }
        return [];
    } catch {
        return [];
    }
};

/** 정답 텍스트 찾기 헬퍼 함수 */
const getCorrectAnswerText = (
    correctAnswer: string | null | undefined,
    options: { id: string; text: string }[]
): string => {
    if (!correctAnswer) return correctAnswer ?? '';
    // correctAnswer가 "A", "B" 같은 ID인 경우 해당 옵션의 text 반환
    const option = options.find(opt => opt.id === correctAnswer);
    if (option) return `${option.id}. ${option.text}`;
    // ID로 못 찾으면 원본 값 반환
    return correctAnswer;
};

export const MeetingTestPage: React.FC = () => {
    const navigate = useNavigate();

    // 스터디 목록
    const [studies, setStudies] = useState<StudyOption[]>([]);
    const [selectedStudyId, setSelectedStudyId] = useState<number | null>(null);
    const [showStudyDropdown, setShowStudyDropdown] = useState(false);
    const [studiesLoading, setStudiesLoading] = useState(true);
    const studyDropdownRef = useRef<HTMLDivElement>(null);

    // 퀴즈 목록
    const [quizItems, setQuizItems] = useState<MeetingQuizItem[]>([]);
    const [quizLoading, setQuizLoading] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    // 선택된 퀴즈
    const [selectedQuiz, setSelectedQuiz] = useState<MeetingQuizItem | null>(null);
    const [quizDetail, setQuizDetail] = useState<StudyQuizDetail | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);

    // 풀이 상태
    const [currentIndex, setCurrentIndex] = useState(0);
    const [userAnswer, setUserAnswer] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [result, setResult] = useState<StudyQuizSubmitResponse | null>(null);
    const [correctCount, setCorrectCount] = useState(0);
    const [isComplete, setIsComplete] = useState(false);
    const startTimeRef = useRef<number>(Date.now());

    // 스터디 목록 로드
    useEffect(() => {
        const loadStudies = async () => {
            try {
                setStudiesLoading(true);
                const response = await studyApi.getMyStudies(0, 50);
                const opts = response.content.map(s => ({ id: s.id, name: s.name }));
                setStudies(opts);
                if (opts.length > 0 && !selectedStudyId) {
                    setSelectedStudyId(opts[0].id);
                }
            } catch (err) {
                console.error('스터디 목록 조회 실패:', err);
            } finally {
                setStudiesLoading(false);
            }
        };
        loadStudies();
    }, []);

    // 선택된 스터디의 미팅 퀴즈 로드
    useEffect(() => {
        if (!selectedStudyId) return;
        const loadQuizzes = async () => {
            setQuizLoading(true);
            try {
                const quizzes = await studyQuizApi.getStudyQuizzes(selectedStudyId);
                const meetingQuizzes = quizzes
                    .filter(q => q.sourceType === 'MEETING' && q.status === 'ACTIVE')
                    .map(quiz => ({
                        quiz,
                        studyId: selectedStudyId,
                        studyName: studies.find(s => s.id === selectedStudyId)?.name || '',
                    }));
                setQuizItems(meetingQuizzes);
                setSelectedQuiz(null);
                setQuizDetail(null);
            } catch (err) {
                console.error('퀴즈 목록 조회 실패:', err);
            } finally {
                setQuizLoading(false);
            }
        };
        loadQuizzes();
    }, [selectedStudyId, studies]);

    // 스터디 드롭다운 외부 클릭
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (studyDropdownRef.current && !studyDropdownRef.current.contains(e.target as Node)) {
                setShowStudyDropdown(false);
            }
        };
        if (showStudyDropdown) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [showStudyDropdown]);

    // 퀴즈 선택
    const handleSelectQuiz = useCallback(async (item: MeetingQuizItem) => {
        setSelectedQuiz(item);
        setDetailLoading(true);
        setCurrentIndex(0);
        setUserAnswer('');
        setSubmitted(false);
        setResult(null);
        setCorrectCount(0);
        setIsComplete(false);

        try {
            const detail = await studyQuizApi.getQuizDetail(item.studyId, item.quiz.id);
            setQuizDetail(detail);
            startTimeRef.current = Date.now();
        } catch (err) {
            console.error('퀴즈 상세 조회 실패:', err);
        } finally {
            setDetailLoading(false);
        }
    }, []);

    // 문제 전환 시 타이머 리셋
    useEffect(() => {
        startTimeRef.current = Date.now();
    }, [currentIndex]);

    // 답안 제출
    const handleSubmit = useCallback(async () => {
        if (!quizDetail || !selectedQuiz || !userAnswer.trim()) return;
        const question = quizDetail.questions[currentIndex];
        const responseTimeMs = Date.now() - startTimeRef.current;

        setSubmitting(true);
        try {
            const res = await studyQuizApi.submitAnswer(
                selectedQuiz.studyId, quizDetail.id, question.id, { userAnswer, responseTimeMs }
            );
            setResult(res);
            setSubmitted(true);
            if (res.isCorrect) setCorrectCount(prev => prev + 1);
        } catch {
            const isCorrect = userAnswer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase();
            setResult({ isCorrect, correctAnswer: question.correctAnswer, explanation: question.explanation } as StudyQuizSubmitResponse);
            setSubmitted(true);
            if (isCorrect) setCorrectCount(prev => prev + 1);
        } finally {
            setSubmitting(false);
        }
    }, [quizDetail, selectedQuiz, currentIndex, userAnswer]);

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
    }, []);

    // 검색 필터링
    const filteredItems = quizItems.filter(item =>
        searchQuery === '' ||
        item.quiz.title.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="py-8">
            <div className="max-w-[1600px] mx-auto px-8">
                {/* 브레드크럼 헤더 */}
                <PageNavHeader
                    title="미팅 테스트"
                    breadcrumbs={[
                        { label: '대시보드', path: '/dashboard' },
                        { label: '미팅 테스트' },
                    ]}
                    onBack={() => navigate(-1)}
                />

                {/* 좌측 사이드바 + 우측 콘텐츠 */}
                <div className={cn(
                    'bg-surface rounded-2xl shadow-[0_4px_15px_rgba(0,0,0,0.05)]',
                    'overflow-hidden'
                )}>
                    <div className="flex">
                        {/* 좌측: 퀴즈 리스트 */}
                        <div className={cn(
                            'w-80 flex-shrink-0 flex flex-col',
                            'bg-background/70 border-r border-border'
                        )}>
                            {/* 스터디 선택 드롭다운 */}
                            <div className="p-4 border-b border-border" ref={studyDropdownRef}>
                                <div className="relative">
                                    <button
                                        onClick={() => setShowStudyDropdown(prev => !prev)}
                                        disabled={studiesLoading || studies.length === 0}
                                        className={cn(
                                            'w-full flex items-center justify-between px-3 py-2.5 text-sm',
                                            'border border-border rounded-google bg-surface',
                                            'hover:border-primary/50 transition-colors',
                                            'disabled:opacity-50 disabled:cursor-not-allowed'
                                        )}
                                    >
                                        <div className="flex items-center gap-2">
                                            <BookOpen size={16} className="text-accent" />
                                            <span className="font-medium text-text-primary truncate">
                                                {studiesLoading
                                                    ? '로딩 중...'
                                                    : studies.length === 0
                                                        ? '참여 중인 스터디 없음'
                                                        : studies.find(s => s.id === selectedStudyId)?.name || '스터디 선택'}
                                            </span>
                                        </div>
                                        <ChevronDown size={16} className={cn(
                                            'text-text-tertiary transition-transform',
                                            showStudyDropdown && 'rotate-180'
                                        )} />
                                    </button>

                                    <AnimatePresence>
                                        {showStudyDropdown && studies.length > 0 && (
                                            <motion.div
                                                initial={{ opacity: 0, y: -5 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                exit={{ opacity: 0, y: -5 }}
                                                className={cn(
                                                    'absolute left-0 right-0 top-full mt-1 z-50',
                                                    'bg-surface border border-border rounded-google shadow-lg',
                                                    'max-h-48 overflow-y-auto'
                                                )}
                                            >
                                                {studies.map(study => (
                                                    <button
                                                        key={study.id}
                                                        onClick={() => {
                                                            setSelectedStudyId(study.id);
                                                            setShowStudyDropdown(false);
                                                        }}
                                                        className={cn(
                                                            'w-full px-3 py-2 text-left text-sm',
                                                            'hover:bg-surface-hover transition-colors',
                                                            selectedStudyId === study.id && 'bg-accent/10 text-accent font-medium'
                                                        )}
                                                    >
                                                        {study.name}
                                                    </button>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {/* 검색 */}
                            <div className="p-4 border-b border-border">
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary" size={16} />
                                    <input
                                        type="text"
                                        placeholder="퀴즈 검색..."
                                        value={searchQuery}
                                        onChange={e => setSearchQuery(e.target.value)}
                                        className={cn(
                                            'w-full pl-9 pr-4 py-2.5 text-sm',
                                            'border border-border rounded-google',
                                            'focus:border-accent focus:outline-none transition-colors'
                                        )}
                                    />
                                </div>
                            </div>

                            {/* 퀴즈 리스트 */}
                            <div className="flex-1 overflow-y-auto max-h-[600px]">
                                {quizLoading ? (
                                    <div className="flex items-center justify-center h-32">
                                        <Spinner variant="center" size="md" label="퀴즈 로딩 중..." />
                                    </div>
                                ) : filteredItems.length === 0 ? (
                                    <div className="flex flex-col items-center justify-center h-32 text-text-tertiary text-sm">
                                        <ClipboardCheck size={24} className="mb-2 opacity-50" />
                                        <span>미팅 테스트가 없습니다</span>
                                    </div>
                                ) : (
                                    filteredItems.map(item => {
                                        const isSelected = selectedQuiz?.quiz.id === item.quiz.id;
                                        return (
                                            <button
                                                key={item.quiz.id}
                                                onClick={() => handleSelectQuiz(item)}
                                                className={cn(
                                                    'w-full p-4 text-left transition-all border-b border-border',
                                                    conditionalClasses.state(
                                                        isSelected,
                                                        'bg-surface border-l-4 border-l-accent -mr-px z-10',
                                                        'hover:bg-surface-hover'
                                                    )
                                                )}
                                            >
                                                <h4 className="font-bold text-text-primary text-sm mb-0.5">
                                                    {item.quiz.title}
                                                </h4>
                                                <div className="flex items-center gap-2 mt-2 text-xs text-text-tertiary">
                                                    <Calendar size={12} />
                                                    <span>{item.quiz.createdAt.split('T')[0]}</span>
                                                    <span>·</span>
                                                    <span>{item.quiz.questionCount}문제</span>
                                                </div>
                                            </button>
                                        );
                                    })
                                )}
                            </div>

                            {/* 우측 border */}
                            <div className="absolute top-0 right-0 bottom-0 w-px bg-border" />
                        </div>

                        {/* 우측 콘텐츠 영역 */}
                        <div className="flex-1 p-8">
                            {!selectedQuiz ? (
                                <div className="flex items-center justify-center h-64 text-text-tertiary">
                                    퀴즈를 선택해주세요
                                </div>
                            ) : detailLoading ? (
                                <div className="flex items-center justify-center h-64">
                                    <Spinner variant="center" size="md" label="로딩 중..." />
                                </div>
                            ) : quizDetail ? (
                                <>
                                    {/* 퀴즈 헤더 */}
                                    <div className="mb-6 pb-6 border-b border-border">
                                        <div className="flex items-center gap-2 mb-2">
                                            <h2 className="text-xl font-bold text-text-primary">
                                                {quizDetail.title}
                                            </h2>
                                            <span className={cn(
                                                'inline-flex items-center gap-1 px-2 py-0.5 text-[11px] font-semibold rounded-pill',
                                                'bg-accent/10 text-accent'
                                            )}>
                                                <Sparkles size={10} />
                                                AI 생성
                                            </span>
                                        </div>
                                        <div className="flex items-center gap-4 text-sm text-text-secondary">
                                            <span>{quizDetail.questions.length}문제</span>
                                            <span>{selectedQuiz.studyName}</span>
                                            <span>{selectedQuiz.quiz.createdAt.split('T')[0]}</span>
                                        </div>
                                    </div>

                                    {/* 풀이 영역 */}
                                    {!isComplete ? (
                                        <AnimatePresence mode="wait">
                                            <motion.div
                                                key={currentIndex}
                                                initial={{ opacity: 0, y: 10 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                exit={{ opacity: 0, y: -10 }}
                                                transition={{ duration: 0.2 }}
                                            >
                                                {(() => {
                                                    const question = quizDetail.questions[currentIndex];
                                                    const options = parseOptions(question.options);
                                                    const progress = ((currentIndex + 1) / quizDetail.questions.length) * 100;

                                                    return (
                                                        <>
                                                            {/* 진행률 */}
                                                            <div className="flex items-center gap-3 mb-6">
                                                                <div className="flex-1 bg-background rounded-full h-2 overflow-hidden">
                                                                    <motion.div
                                                                        className="h-2 bg-accent rounded-full"
                                                                        animate={{ width: `${progress}%` }}
                                                                        transition={{ duration: 0.3 }}
                                                                    />
                                                                </div>
                                                                <span className="text-sm text-text-tertiary whitespace-nowrap">
                                                                    {currentIndex + 1} / {quizDetail.questions.length}
                                                                </span>
                                                            </div>

                                                            {/* 문제 번호 + 유형 */}
                                                            <div className="flex items-center gap-2 mb-4">
                                                                <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-accent text-white">
                                                                    Q{currentIndex + 1}
                                                                </span>
                                                                <span className="text-xs text-text-tertiary">
                                                                    {question.questionType === 'MULTIPLE_CHOICE' ? '객관식' : '단답형'}
                                                                </span>
                                                            </div>

                                                            {/* 문제 텍스트 */}
                                                            <p className="text-base font-medium text-text-primary mb-6 leading-relaxed">
                                                                {question.questionText}
                                                            </p>

                                                            {/* 선택지 / 입력 */}
                                                            {question.questionType === 'MULTIPLE_CHOICE' && options.length > 0 ? (
                                                                <div className="space-y-2.5">
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
                                                                                    'w-full flex items-center gap-3 px-4 py-3.5 rounded-xl text-left transition-all',
                                                                                    'border-2',
                                                                                    !submitted && conditionalClasses.state(
                                                                                        isSelected,
                                                                                        'border-accent bg-accent/5',
                                                                                        'border-border hover:border-accent/40 hover:bg-surface-hover'
                                                                                    ),
                                                                                    submitted && showCorrect && 'border-secondary bg-secondary/5',
                                                                                    submitted && showWrong && 'border-error bg-error/5',
                                                                                    submitted && !showCorrect && !showWrong && 'border-border opacity-50',
                                                                                    submitted && 'cursor-default'
                                                                                )}
                                                                            >
                                                                                {submitted && showCorrect ? (
                                                                                    <CheckCircle2 size={18} className="text-secondary flex-shrink-0" />
                                                                                ) : submitted && showWrong ? (
                                                                                    <XCircle size={18} className="text-error flex-shrink-0" />
                                                                                ) : (
                                                                                    <Circle size={18} className={cn('flex-shrink-0', isSelected ? 'text-accent' : 'text-text-tertiary')} />
                                                                                )}
                                                                                <span className={cn(
                                                                                    'text-sm font-medium',
                                                                                    submitted && showCorrect && 'text-secondary-dark',
                                                                                    submitted && showWrong && 'text-error',
                                                                                    !submitted && isSelected && 'text-accent',
                                                                                    !submitted && !isSelected && 'text-text-secondary',
                                                                                    submitted && !showCorrect && !showWrong && 'text-text-tertiary'
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
                                                                        'w-full px-4 py-3 text-sm rounded-xl transition-all',
                                                                        'border-2 border-border bg-background',
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
                                                                        className="mt-5"
                                                                    >
                                                                        <div className={cn(
                                                                            'rounded-xl p-4 border',
                                                                            result.isCorrect
                                                                                ? 'bg-secondary/5 border-secondary/30'
                                                                                : 'bg-error/5 border-error/30'
                                                                        )}>
                                                                            <div className="flex items-center gap-2 mb-2">
                                                                                {result.isCorrect ? (
                                                                                    <CheckCircle2 size={18} className="text-secondary" />
                                                                                ) : (
                                                                                    <XCircle size={18} className="text-error" />
                                                                                )}
                                                                                <span className={cn(
                                                                                    'text-sm font-semibold',
                                                                                    result.isCorrect ? 'text-secondary-dark' : 'text-error'
                                                                                )}>
                                                                                    {result.isCorrect ? '정답입니다!' : '오답입니다'}
                                                                                </span>
                                                                            </div>
                                                                            {!result.isCorrect && (
                                                                                <p className="text-sm text-text-secondary mb-1">
                                                                                    정답: <span className="font-medium text-secondary">
                                                                                        {getCorrectAnswerText(result.correctAnswer, options)}
                                                                                    </span>
                                                                                </p>
                                                                            )}
                                                                            {result.explanation && (
                                                                                <p className="text-sm text-text-secondary mt-2">{result.explanation}</p>
                                                                            )}
                                                                        </div>
                                                                    </motion.div>
                                                                )}
                                                            </AnimatePresence>

                                                            {/* 버튼 */}
                                                            <div className="mt-6 flex justify-end">
                                                                {!submitted ? (
                                                                    <button
                                                                        onClick={handleSubmit}
                                                                        disabled={!userAnswer.trim() || submitting}
                                                                        className={cn(
                                                                            'inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium rounded-lg transition-colors',
                                                                            'bg-accent text-white hover:bg-accent/90',
                                                                            'disabled:opacity-50 disabled:cursor-not-allowed'
                                                                        )}
                                                                    >
                                                                        {submitting ? <Spinner size="sm" /> : <Send size={15} />}
                                                                        제출하기
                                                                    </button>
                                                                ) : (
                                                                    <button
                                                                        onClick={handleNext}
                                                                        className={cn(
                                                                            'inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium rounded-lg transition-colors',
                                                                            'bg-accent text-white hover:bg-accent/90'
                                                                        )}
                                                                    >
                                                                        {currentIndex + 1 >= quizDetail.questions.length ? '결과 보기' : '다음 문제'}
                                                                        <ChevronRight size={15} />
                                                                    </button>
                                                                )}
                                                            </div>
                                                        </>
                                                    );
                                                })()}
                                            </motion.div>
                                        </AnimatePresence>
                                    ) : (
                                        // 완료 화면
                                        <div className="flex flex-col items-center justify-center py-12">
                                            <div className="w-20 h-20 rounded-full bg-accent/10 flex items-center justify-center mb-6">
                                                <Trophy size={36} className="text-accent" />
                                            </div>
                                            <h3 className="text-xl font-bold text-text-primary mb-2">퀴즈 완료!</h3>
                                            <p className="text-sm text-text-secondary mb-6">{quizDetail.title}</p>

                                            <div className="grid grid-cols-3 gap-4 w-full max-w-sm mb-8">
                                                <div className="text-center p-4 rounded-xl bg-background border border-border">
                                                    <div className="text-2xl font-bold text-text-primary">{quizDetail.questions.length}</div>
                                                    <div className="text-xs text-text-tertiary mt-1">총 문제</div>
                                                </div>
                                                <div className="text-center p-4 rounded-xl bg-background border border-border">
                                                    <div className="text-2xl font-bold text-secondary">{correctCount}</div>
                                                    <div className="text-xs text-text-tertiary mt-1">정답</div>
                                                </div>
                                                <div className="text-center p-4 rounded-xl bg-background border border-border">
                                                    <div className="text-2xl font-bold text-accent">
                                                        {Math.round((correctCount / quizDetail.questions.length) * 100)}%
                                                    </div>
                                                    <div className="text-xs text-text-tertiary mt-1">정답률</div>
                                                </div>
                                            </div>

                                            <div className="w-full max-w-sm mb-8">
                                                <div className="w-full bg-background rounded-full h-3 overflow-hidden">
                                                    <motion.div
                                                        initial={{ width: 0 }}
                                                        animate={{ width: `${Math.round((correctCount / quizDetail.questions.length) * 100)}%` }}
                                                        transition={{ duration: 0.8, ease: 'easeOut' }}
                                                        className={cn(
                                                            'h-3 rounded-full',
                                                            correctCount / quizDetail.questions.length >= 0.8 ? 'bg-secondary' :
                                                                correctCount / quizDetail.questions.length >= 0.5 ? 'bg-accent' : 'bg-error'
                                                        )}
                                                    />
                                                </div>
                                            </div>

                                            <button
                                                onClick={handleRetry}
                                                className={cn(
                                                    'inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium rounded-lg transition-colors',
                                                    'bg-accent text-white hover:bg-accent/90'
                                                )}
                                            >
                                                <RotateCcw size={15} />
                                                다시 풀기
                                            </button>
                                        </div>
                                    )}
                                </>
                            ) : null}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default MeetingTestPage;
