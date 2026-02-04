import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Terminal, Trophy, Dumbbell, ChevronRight, Sparkles } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { gamificationApi } from '@/api/endpoints/gamificationApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import { MascotWithEyes } from './components/MascotWithEyes';
import '@/features/dashboard-v2/styles/DashboardV2.css';

/**
 * 타이핑 애니메이션 컴포넌트
 */
const TypingText = ({ text }: { text: string }) => {
    const [displayText, setDisplayText] = useState('');
    const [index, setIndex] = useState(0);

    useEffect(() => {
        if (index < text.length) {
            const timer = setTimeout(() => {
                setDisplayText(prev => prev + text[index]);
                setIndex(prev => prev + 1);
            }, 150);
            return () => clearTimeout(timer);
        } else {
            // 타이핑 완료 후 잠시 대기 후 리셋
            const resetTimer = setTimeout(() => {
                setDisplayText('');
                setIndex(0);
            }, 2000);
            return () => clearTimeout(resetTimer);
        }
    }, [index, text]);

    return (
        <span>
            {displayText}
            <span className="typing-cursor" />
        </span>
    );
};

/**
 * 퀴즈 게임 선택 메인 페이지
 * 3가지 핵심 게임 모드를 현대적인 카드로 제공
 */
export const QuizGameSelection = () => {
    const navigate = useNavigate();
    const { isLoggedIn } = useAuthStore();
    const { showToast } = useUIStore();

    // 사용자 퀴즈 통계
    const [quizCount, setQuizCount] = useState<number>(0);

    // 통계 조회 (로그인 시에만)
    useEffect(() => {
        if (!isLoggedIn) return;
        gamificationApi.getStats()
            .then((stats) => {
                setQuizCount(stats.totalQuizCount || 0);
            })
            .catch((err) => {
                console.error('퀴즈 통계 조회 실패:', err);
            });
    }, [isLoggedIn]);

    // 카드 클릭 핸들러 — 꼬멘틀만 비로그인 허용
    const handleGameClick = (game: typeof games[number]) => {
        if (game.id === 'commentle') {
            navigate(game.route);
            return;
        }
        if (!isLoggedIn) {
            showToast?.('로그인이 필요한 기능입니다.', 'warning');
            sessionStorage.setItem('redirectAfterLogin', '/quiz');
            setTimeout(() => navigate('/login'), 300);
            return;
        }
        navigate(game.route);
    };

    const games = [
        {
            id: 'commentle',
            title: 'COMMENTLE',
            subtitle: 'AI 시맨틱 단어 퀴즈',
            description: 'S-ROBERTA 임베딩 기술을 활용하여 고차원 의미 유사도를 기반으로 CS 용어를 추론해보세요.',
            icon: Terminal,
            route: '/quiz-commentle',
            theme: 'primary',
            gradient: 'from-primary to-primary-dark',
            tag: '인기'
        },
        {
            id: 'contest',
            title: 'QUIZ CONTEST',
            subtitle: '실시간 온라인 대전',
            description: '전국의 스터디 팀과 실시간으로 지력을 겨루고, 상위 랭킹에 도전하여 명예를 쟁취하세요.',
            icon: Trophy,
            route: '/quiz-contest',
            theme: 'secondary',
            gradient: 'from-secondary to-secondary-dark',
            tag: '준비중'
        },
        {
            id: 'practice',
            title: 'PRACTICE MODE',
            subtitle: '개인 훈련소',
            description: '분야별, 난이도별 CS 핵심 개념들을 반복 학습하여 기초를 탄탄하게 다질 수 있는 모드입니다.',
            icon: Dumbbell,
            route: '/quiz-practice',
            theme: 'accent',
            gradient: 'from-accent-dark to-accent',
        },
    ];

    return (
        <div className="p-6 md:p-8 lg:p-10">
            <div className="max-w-7xl mx-auto">
                {/* 헤더 */}
                <header className="relative mb-12 animate-fadeIn overflow-hidden">
                    {/* 물방울 애니메이션 배경 */}
                    <div className="bubble-container">
                        <span className="bubble bubble-1" />
                        <span className="bubble bubble-2" />
                        <span className="bubble bubble-3" />
                        <span className="bubble bubble-4" />
                        <span className="bubble bubble-5" />
                        <span className="bubble bubble-6" />
                        <span className="bubble bubble-7" />
                        <span className="bubble bubble-8" />
                    </div>

                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
                        <div>
                            <div className="flex items-center gap-2 text-primary font-black text-xs uppercase tracking-widest mb-3">
                                <Sparkles size={14} className="animate-pulse" />
                                Interactive Learning
                            </div>
                            <h1 className="text-4xl md:text-5xl font-black text-text-primary tracking-tight mb-2">
                                SQUIZ 아일랜드🏝️
                            </h1>
                            <p className="text-lg text-text-secondary font-medium">
                                다양한 모드를 정복하며 지식의 보물을 모아보세요!
                            </p>
                        </div>

                        {/* 마스코트 (눈이 커서를 따라감) */}
                        <MascotWithEyes size={120} className="hidden md:block flex-shrink-0" />
                    </div>
                </header>

                {/* 게임 카드 그리드 */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {games.map((game, index) => {
                        const Icon = game.icon;
                        // 테마별 글로우 색상 (파랑, 초록, 노랑)
                        const glowColors: Record<string, string> = {
                            primary: 'rgba(59, 130, 246, 0.4)',    // 파란색
                            secondary: 'rgba(34, 197, 94, 0.4)',   // 초록색
                            accent: 'rgba(250, 204, 21, 0.5)',     // 노란색
                        };
                        const glowColor = glowColors[game.theme] || glowColors.primary;

                        return (
                            <div
                                key={game.id}
                                onClick={() => handleGameClick(game)}
                                className={cn(
                                    "relative group cursor-pointer bg-white rounded-[24px] p-8 shadow-[0_4px_15px_rgba(0,0,0,0.05)] hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] transition-all duration-500 hover:-translate-y-3 overflow-hidden",
                                    "animate-slideInUp"
                                )}
                                style={{
                                    animationDelay: `${index * 150}ms`,
                                    // @ts-ignore
                                    '--glow-color': glowColor,
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.boxShadow = `0 0 40px ${glowColor}, 0 20px 50px rgba(0, 0, 0, 0.1)`;
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.boxShadow = '';
                                }}
                            >
                                {/* 카드 배경 장식 */}
                                <div className={cn(
                                    "absolute -bottom-10 -right-10 w-40 h-40 rounded-full blur-3xl opacity-0 group-hover:opacity-10 transition-opacity duration-500",
                                    `bg-${game.theme}`
                                )} />

                                {/* 상단 태그 */}
                                {game.tag && (
                                    <div className={cn(
                                        "absolute top-6 right-6 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-tighter border",
                                        game.tag === '인기' ? "bg-error/5 text-error border-error/10" : "bg-text-tertiary/10 text-text-tertiary border-text-tertiary/10"
                                    )}>
                                        {game.tag}
                                    </div>
                                )}

                                {/* 아이콘 유닛 */}
                                <div className="relative mb-8">
                                    <div className={cn(
                                        "inline-flex items-center justify-center w-16 h-16 rounded-2xl shadow-lg transform group-hover:scale-110 group-hover:rotate-6 transition-all duration-500",
                                        "bg-gradient-to-br",
                                        game.gradient
                                    )}>
                                        <Icon size={32} className="text-white" />
                                    </div>

                                </div>

                                {/* 텍스트 영역 */}
                                <div className="space-y-6">
                                    <div>
                                        <h2 className="text-2xl font-black text-text-primary mb-1 group-hover:text-primary transition-colors">
                                            {game.title}
                                        </h2>
                                        <p className={cn(
                                            "text-sm font-bold uppercase tracking-wide",
                                            `text-${game.theme}`
                                        )}>
                                            {game.subtitle}
                                        </p>
                                    </div>

                                    <p className="text-sm text-text-secondary leading-relaxed line-clamp-2 min-h-[44px]">
                                        {game.description}
                                    </p>

                                    {/* COMMENTLE: 타이핑 애니메이션 */}
                                    {game.id === 'commentle' && (
                                        <div className="mt-3 font-mono text-xs bg-gray-900 text-green-400 px-3 py-2 rounded-lg">
                                            <span className="opacity-60">&gt; </span>
                                            <TypingText text="ALGORITHM" />
                                        </div>
                                    )}

                                    {/* QUIZ CONTEST: 배틀 애니메이션 */}
                                    {game.id === 'contest' && (
                                        <div className="mt-3 flex items-center justify-center gap-5">
                                            {/* 왼쪽 파이터 */}
                                            <div className="fighter-left flex items-center gap-1">
                                                <div className="w-8 h-8 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center text-xs text-white font-black shadow-lg">
                                                    P1
                                                </div>
                                                <span className="text-xs font-bold text-blue-600">TEAM</span>
                                            </div>
                                            {/* VS */}
                                            <div className="vs-pulse text-lg font-black text-orange-500">
                                                VS
                                            </div>
                                            {/* 오른쪽 파이터 */}
                                            <div className="fighter-right flex items-center gap-1">
                                                <span className="text-xs font-bold text-red-600">TEAM</span>
                                                <div className="w-8 h-8 bg-gradient-to-br from-red-400 to-red-600 rounded-full flex items-center justify-center text-xs text-white font-black shadow-lg">
                                                    P2
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    {/* PRACTICE MODE: 진행바 */}
                                    {game.id === 'practice' && (
                                        <div className="mt-3">
                                            <div className="flex justify-between text-xs text-text-tertiary mb-1">
                                                <span>학습 진행률</span>
                                                <span className="progress-bar-pulse">진행중...</span>
                                            </div>
                                            <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                                                <div className="h-full bg-gradient-to-r from-accent to-accent-dark rounded-full progress-bar-animated" />
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* 시작하기 버튼부 */}
                                <div className="mt-10 pt-6 border-t border-border-light/50 flex items-center justify-end">
                                    <div className={cn(
                                        "w-10 h-10 rounded-full flex items-center justify-center transform group-hover:translate-x-1 transition-all duration-500",
                                        "text-text-tertiary group-hover:text-primary"
                                    )}>
                                        <ChevronRight size={20} />
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* 하단 정보 섹션 */}
                <footer className="mt-16 animate-fadeIn" style={{ animationDelay: '600ms' }}>
                    <div className="bg-white p-8 rounded-[24px] flex flex-col md:flex-row items-center justify-between gap-6 shadow-[0_4px_15px_rgba(0,0,0,0.05)] hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] transition-shadow duration-300">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                                <Sparkles size={24} />
                            </div>
                            <div className="flex flex-col justify-center">
                                <h4 className="font-bold text-text-primary leading-snug">오늘의 목표를 달성해보세요!</h4>
                                <p className="text-sm text-text-secondary leading-snug">매일 꾸준한 퀴즈 학습은 장기 기억 형성에 큰 도움이 됩니다.</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <span className="text-xs font-black text-text-tertiary uppercase tracking-widest block mb-1">맞춘 문제</span>
                            <div className="text-2xl font-black text-primary">{quizCount}문제</div>
                        </div>
                    </div>
                </footer>
            </div>
        </div>
    );
};
