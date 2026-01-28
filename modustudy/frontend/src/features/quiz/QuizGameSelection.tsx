import { useNavigate } from 'react-router-dom';
import { Terminal, Trophy, Dumbbell, ChevronRight, Sparkles } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { ArrowButton } from '@/shared/components';

/**
 * 퀴즈 게임 선택 메인 페이지
 * 3가지 핵심 게임 모드를 현대적인 카드로 제공
 */
export const QuizGameSelection = () => {
    const navigate = useNavigate();

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
        <div className="min-h-screen bg-background p-6 md:p-12 lg:p-16">
            <div className="max-w-7xl mx-auto">
                {/* 상단 네비게이션 및 헤더 */}
                <header className="mb-16 animate-fadeIn">
                    <div className="flex items-center gap-4 mb-10">
                        <ArrowButton
                            direction="left"
                            onClick={() => navigate('/dashboard')}
                            size="md"
                        />
                        <span className="text-sm font-bold text-text-secondary">대시보드로 돌아가기</span>
                    </div>

                    <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
                        <div>
                            <div className="flex items-center gap-2 text-primary font-black text-xs uppercase tracking-widest mb-3">
                                <Sparkles size={14} className="animate-pulse" />
                                Interactive Learning
                            </div>
                            <h1 className="text-4xl md:text-5xl font-black text-text-primary tracking-tight mb-2">
                                🎮 SQUIZ 게임 센터
                            </h1>
                            <p className="text-lg text-text-secondary font-medium">
                                지식을 테스트하고 새로운 개념을 습득할 게임 모드를 선택하세요.
                            </p>
                        </div>
                    </div>
                </header>

                {/* 게임 카드 그리드 */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {games.map((game, index) => {
                        const Icon = game.icon;
                        return (
                            <div
                                key={game.id}
                                onClick={() => navigate(game.route)}
                                className={cn(
                                    "relative group cursor-pointer bg-white rounded-[32px] p-8 border border-border-light shadow-sm transition-all duration-500 hover:shadow-2xl hover:shadow-primary/10 hover:-translate-y-3 overflow-hidden",
                                    "animate-slideInUp"
                                )}
                                style={{ animationDelay: `${index * 150}ms` }}
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
                                <div className={cn(
                                    "mb-8 inline-flex items-center justify-center w-16 h-16 rounded-2xl shadow-lg transform group-hover:scale-110 group-hover:rotate-6 transition-all duration-500",
                                    "bg-gradient-to-br",
                                    game.gradient
                                )}>
                                    <Icon size={32} className="text-white" />
                                </div>

                                {/* 텍스트 영역 */}
                                <div className="space-y-4">
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
                                </div>

                                {/* 시작하기 버튼부 */}
                                <div className="mt-10 pt-6 border-t border-border-light/50 flex items-center justify-end">
                                    <div className={cn(
                                        "w-10 h-10 rounded-full flex items-center justify-center transform group-hover:translate-x-1 transition-all duration-500",
                                        "bg-background-secondary group-hover:bg-primary group-hover:text-white"
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
                    <div className="bg-white/40 backdrop-blur-md p-8 rounded-[32px] border border-white/60 flex flex-col md:flex-row items-center justify-between gap-6 shadow-sm">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                                <Sparkles size={24} />
                            </div>
                            <div>
                                <h4 className="font-bold text-text-primary">오늘의 목표를 달성해보세요!</h4>
                                <p className="text-sm text-text-secondary">매일 꾸준한 퀴즈 학습은 장기 기억 형성에 큰 도움이 됩니다.</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <span className="text-xs font-black text-text-tertiary uppercase tracking-widest block mb-1">User Progress</span>
                            <div className="text-2xl font-black text-primary">Level 12</div>
                        </div>
                    </div>
                </footer>
            </div>
        </div>
    );
};
