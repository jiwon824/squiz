import { useNavigate } from 'react-router-dom';
import { Terminal, Trophy, Dumbbell, ArrowLeft } from 'lucide-react';

/**
 * 퀴즈 게임 선택 메인 페이지
 * 3가지 게임 타입을 카드로 표시
 */
export const QuizGameSelection = () => {
    const navigate = useNavigate();

    const games = [
        {
            id: 'commentle',
            title: 'COMMENTLE',
            subtitle: 'CS 단어 맞추기',
            description: 'AI 유사도 기반으로 CS 용어를 맞춰보세요',
            icon: Terminal,
            route: '/quiz-commentle',
            color: 'var(--color-primary)',
            gradient: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%)',
        },
        {
            id: 'contest',
            title: 'QUIZ CONTEST',
            subtitle: '온라인 퀴즈 대회',
            description: '전국 스터디 팀과 실시간 퀴즈 배틀',
            icon: Trophy,
            route: '/quiz-contest',
            color: 'var(--color-secondary)',
            gradient: 'linear-gradient(135deg, var(--color-secondary) 0%, var(--color-accent) 100%)',
        },
        {
            id: 'practice',
            title: 'PRACTICE MODE',
            subtitle: '연습 모드',
            description: '다양한 난이도의 문제로 실력 향상',
            icon: Dumbbell,
            route: '/quiz-practice',
            color: 'var(--color-accent)',
            gradient: 'linear-gradient(135deg, var(--color-accent) 0%, var(--color-primary) 100%)',
        },
    ];

    return (
        <div className="min-h-screen" style={{
            backgroundColor: 'var(--color-background)',
            padding: 'var(--spacing-2xl) var(--spacing-lg)'
        }}>
            <div className="max-w-6xl mx-auto">
                {/* 뒤로가기 버튼 */}
                <div className="mb-4">
                    <button
                        onClick={() => navigate('/dashboard')}
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            padding: '0.5rem 1rem',
                            borderRadius: 'var(--radius-md)',
                            color: 'var(--color-text-secondary)',
                            fontWeight: '600',
                            transition: 'all 0.2s',
                            background: 'transparent',
                            border: 'none',
                            cursor: 'pointer',
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--color-background-secondary)';
                            e.currentTarget.style.color = 'var(--color-text-primary)';
                            e.currentTarget.style.transform = 'translateX(-2px)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'transparent';
                            e.currentTarget.style.color = 'var(--color-text-secondary)';
                            e.currentTarget.style.transform = 'translateX(0)';
                        }}
                    >
                        <ArrowLeft size={20} />
                        <span>대시보드로 돌아가기</span>
                    </button>
                </div>

                {/* 헤더 */}
                <div className="text-center mb-12">
                    <h1 className="text-4xl font-bold mb-4" style={{ color: 'var(--color-text-primary)' }}>
                        🎮 퀴즈 게임
                    </h1>
                    <p className="text-lg" style={{ color: 'var(--color-text-secondary)' }}>
                        원하는 게임을 선택하세요
                    </p>
                </div>

                {/* 게임 카드 그리드 */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {games.map((game) => {
                        const Icon = game.icon;
                        return (
                            <div
                                key={game.id}
                                onClick={() => navigate(game.route)}
                                className="group cursor-pointer"
                                style={{
                                    backgroundColor: 'var(--color-surface)',
                                    borderRadius: 'var(--radius-xl)',
                                    padding: 'var(--spacing-xl)',
                                    border: '2px solid var(--color-border)',
                                    transition: 'all var(--transition-base)',
                                    boxShadow: 'var(--shadow-sm)',
                                }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.transform = 'translateY(-8px)';
                                    e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
                                    e.currentTarget.style.borderColor = game.color;
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.transform = 'translateY(0)';
                                    e.currentTarget.style.boxShadow = 'var(--shadow-sm)';
                                    e.currentTarget.style.borderColor = 'var(--color-border)';
                                }}
                            >
                                {/* 아이콘 */}
                                <div
                                    className="mb-4 inline-flex items-center justify-center"
                                    style={{
                                        width: '64px',
                                        height: '64px',
                                        borderRadius: 'var(--radius-lg)',
                                        background: game.gradient,
                                    }}
                                >
                                    <Icon size={32} color="white" />
                                </div>

                                {/* 제목 */}
                                <h2
                                    className="text-2xl font-bold mb-2"
                                    style={{ color: 'var(--color-text-primary)' }}
                                >
                                    {game.title}
                                </h2>

                                {/* 부제목 */}
                                <p
                                    className="text-sm font-medium mb-3"
                                    style={{ color: game.color }}
                                >
                                    {game.subtitle}
                                </p>

                                {/* 설명 */}
                                <p
                                    className="text-sm leading-relaxed"
                                    style={{ color: 'var(--color-text-secondary)' }}
                                >
                                    {game.description}
                                </p>

                                {/* 호버 시 화살표 */}
                                <div
                                    className="mt-4 flex items-center gap-2 opacity-0 group-hover:opacity-100"
                                    style={{
                                        color: game.color,
                                        transition: 'opacity var(--transition-base)',
                                        fontWeight: 'var(--font-weight-semibold)',
                                    }}
                                >
                                    <span>시작하기</span>
                                    <span>→</span>
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* 하단 안내 */}
                <div
                    className="mt-12 text-center"
                    style={{
                        padding: 'var(--spacing-lg)',
                        backgroundColor: 'var(--color-primary-alpha-10)',
                        borderRadius: 'var(--radius-lg)',
                    }}
                >
                    <p style={{ color: 'var(--color-text-secondary)' }}>
                        💡 <strong>Tip:</strong> 각 게임은 다른 방식으로 여러분의 CS 지식을 테스트합니다
                    </p>
                </div>
            </div>
        </div>
    );
};
