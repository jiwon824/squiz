import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Terminal, Trophy, Dumbbell, Sparkles, ArrowRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
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
 * 보물섬 탐험 컨셉의 3가지 핵심 게임 모드
 */
export const QuizGameSelection = () => {
    const navigate = useNavigate();
    const { isLoggedIn } = useAuthStore();
    const { showToast } = useUIStore();

    // 카드 클릭 핸들러
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
            description: 'S-ROBERTA 임베딩 기술로 CS 용어를 추론하는 미스터리 섬!',
            icon: Terminal,
            route: '/quiz-commentle',
            theme: 'primary',
            gradient: 'from-blue-500 to-cyan-400',
            bgGradient: 'from-blue-50 to-cyan-50',
            borderColor: 'border-blue-200',
            tag: '인기',
            fishImage: '/images/commentle_fish.png',
            glowColor: 'rgba(59, 130, 246, 0.4)'
        },
        {
            id: 'contest',
            title: 'QUIZ CONTEST',
            subtitle: '실시간 온라인 대전',
            description: '전국의 팀과 실시간 지력 대결! 명예를 쟁취하세요!',
            icon: Trophy,
            route: '/quiz-contest',
            theme: 'secondary',
            gradient: 'from-emerald-500 to-green-400',
            bgGradient: 'from-emerald-50 to-green-50',
            borderColor: 'border-emerald-200',
            tag: '준비중',
            fishImage: '/images/quiz_contest_fish.png',
            glowColor: 'rgba(34, 197, 94, 0.4)'
        },
        {
            id: 'practice',
            title: 'PRACTICE MODE',
            subtitle: '개인 훈련소',
            description: 'CS 핵심 개념을 반복 학습하는 수련의 섬!',
            icon: Dumbbell,
            route: '/quiz-practice',
            theme: 'accent',
            gradient: 'from-amber-500 to-orange-400',
            bgGradient: 'from-amber-50 to-orange-50',
            borderColor: 'border-amber-200',
            fishImage: '/images/practice_fish.png',
            glowColor: 'rgba(250, 204, 21, 0.5)'
        },
    ];

    return (
        <div className="min-h-screen bg-white p-6 md:p-8 lg:p-10">
            <div className="max-w-7xl mx-auto">
                {/* 헤더 */}
                <header
                    className="relative mb-12 rounded-2xl p-8 md:p-10 backdrop-blur-md overflow-hidden"
                    style={{
                        backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    }}
                >
                    {/* 물방울 애니메이션 - 크게 & 많이 */}
                    <div className="bubble-container" style={{ transform: 'scale(3)' }}>
                        {[...Array(8)].map((_, i) => (
                            <span key={i} className={`bubble bubble-${i + 1}`} />
                        ))}
                    </div>
                    <div className="bubble-container" style={{ transform: 'scale(2.5)', left: '20%' }}>
                        {[...Array(8)].map((_, i) => (
                            <span key={`extra1-${i}`} className={`bubble bubble-${i + 1}`} />
                        ))}
                    </div>
                    <div className="bubble-container" style={{ transform: 'scale(2)', left: '60%' }}>
                        {[...Array(8)].map((_, i) => (
                            <span key={`extra2-${i}`} className={`bubble bubble-${i + 1}`} />
                        ))}
                    </div>

                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
                        <div>
                            <div className="flex items-center gap-2 text-google-blue font-black text-xs uppercase tracking-widest mb-3">
                                <Sparkles size={14} className="animate-pulse" />
                                Treasure Island Adventure
                            </div>
                            <h1 className="text-4xl md:text-5xl font-black tracking-tight mb-3 text-gray-800">
                                SQUIZ 아일랜드 🏝️
                            </h1>
                            <p className="text-lg font-medium text-gray-600">
                                퀴즈를 세 가지 모드로 즐겨보세요!
                            </p>
                        </div>

                        {/* 마스코트 돌고래 */}
                        <div className="relative hidden md:block">
                            <MascotWithEyes size={130} className="relative z-10 drop-shadow-xl" />
                        </div>
                    </div>
                </header>

                {/* 게임 카드 그리드 - 섬 테마 */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 md:gap-10 lg:gap-12">
                    {games.map((game, index) => {
                        const Icon = game.icon;

                        return (
                            <div
                                key={game.id}
                                onClick={() => handleGameClick(game)}
                                className={cn(
                                    "relative group cursor-pointer rounded-[24px] p-7 md:p-9 lg:p-10 transition-all duration-500 hover:-translate-y-4 overflow-hidden",
                                    "bg-gradient-to-br shadow-lg hover:shadow-2xl",
                                    "flex flex-col h-full min-h-[380px]",
                                    game.bgGradient,
                                    "animate-slideInUp"
                                )}
                                style={{
                                    animationDelay: `${index * 150}ms`,
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.boxShadow = `0 0 50px ${game.glowColor}, 0 25px 60px rgba(0, 0, 0, 0.15)`;
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.boxShadow = '';
                                }}
                            >
                                {/* 반짝임 효과 */}
                                <div className="absolute top-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                                    <Sparkles className="w-5 h-5 text-amber-400 animate-pulse" />
                                </div>

                                {/* 상단 태그 영역 - 고정 높이 */}
                                <div className="h-6 mb-2">
                                    {game.tag && (
                                        <div className={cn(
                                            "inline-block px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-tight",
                                            game.tag === '인기'
                                                ? "bg-red-100 text-red-600 border border-red-200"
                                                : "bg-gray-100 text-gray-500 border border-gray-200"
                                        )}>
                                            {game.tag}
                                        </div>
                                    )}
                                </div>

                                {/* 아이콘 */}
                                <div className="relative mb-5">
                                    <div className={cn(
                                        "inline-flex items-center justify-center w-16 h-16 md:w-18 md:h-18 rounded-2xl shadow-lg transform group-hover:scale-110 group-hover:rotate-6 transition-all duration-500",
                                        "bg-gradient-to-br",
                                        game.gradient
                                    )}>
                                        <Icon size={32} className="text-white drop-shadow" />
                                    </div>
                                </div>

                                {/* 텍스트 영역 - flex-grow로 남은 공간 채우기 */}
                                <div className="flex flex-col flex-grow">
                                    <div className="mb-3">
                                        <h2 className="text-xl md:text-2xl font-black text-gray-800 mb-1 group-hover:text-gray-900 transition-colors">
                                            {game.title}
                                        </h2>
                                        <p className={cn(
                                            "text-sm font-bold uppercase tracking-wide",
                                            game.theme === 'primary' ? "text-blue-600" :
                                            game.theme === 'secondary' ? "text-emerald-600" :
                                            "text-amber-600"
                                        )}>
                                            {game.subtitle}
                                        </p>
                                    </div>

                                    <p className="text-sm text-gray-600 leading-relaxed mb-4">
                                        {game.description}
                                    </p>

                                    {/* 각 모드별 특수 효과 - 고정 높이 */}
                                    <div className="h-[42px] flex items-center">
                                        {game.id === 'commentle' && (
                                            <div className="font-mono text-xs bg-gray-900 text-green-400 px-3 py-2 rounded-lg shadow-inner w-full">
                                                <span className="opacity-60">&gt; </span>
                                                <TypingText text="ALGORITHM" />
                                            </div>
                                        )}

                                        {game.id === 'contest' && (
                                            <div className="flex items-center justify-center gap-4 w-full">
                                                <div className="fighter-left flex items-center gap-1">
                                                    <div className="w-8 h-8 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center text-xs text-white font-black shadow-lg">
                                                        P1
                                                    </div>
                                                </div>
                                                <div className="vs-pulse text-lg font-black text-orange-500">VS</div>
                                                <div className="fighter-right flex items-center gap-1">
                                                    <div className="w-8 h-8 bg-gradient-to-br from-red-400 to-red-600 rounded-full flex items-center justify-center text-xs text-white font-black shadow-lg">
                                                        P2
                                                    </div>
                                                </div>
                                            </div>
                                        )}

                                        {game.id === 'practice' && (
                                            <div className="w-full">
                                                <div className="flex justify-between text-xs text-gray-500 mb-1">
                                                    <span>학습 진행률</span>
                                                    <span className="progress-bar-pulse text-amber-600 font-medium">탐험 중...</span>
                                                </div>
                                                <div className="h-2 bg-white/50 rounded-full overflow-hidden shadow-inner">
                                                    <div className="h-full bg-gradient-to-r from-amber-400 to-orange-500 rounded-full progress-bar-animated" />
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    {/* 빈 공간 채우기 */}
                                    <div className="flex-grow" />
                                </div>

                                {/* 하단 탐험하기 */}
                                <div className="mt-4 pt-4 border-t border-gray-200/50 flex items-center justify-between">
                                    <span className="text-xs font-medium text-gray-400">탐험하기</span>
                                    <ArrowRight size={18} className="text-gray-400 group-hover:text-gray-600 group-hover:translate-x-1 transition-all duration-300" />
                                </div>
                            </div>
                        );
                    })}
                </div>

            </div>
        </div>
    );
};
