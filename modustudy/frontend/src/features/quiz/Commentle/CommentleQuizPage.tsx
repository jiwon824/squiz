import React from 'react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { useCommentleGame } from './hooks/useCommentleGame';
import { CommentleHeader } from './components/CommentleHeader';
import { CommentleProblemCard } from './components/CommentleProblemCard';
import { CommentleInputSection } from './components/CommentleInputSection';
import { CommentleHistory } from './components/CommentleHistory';
import { CommentleStatsBoard } from './components/CommentleStatsBoard';
import { Commentle3DView } from './components/Commentle3DView';
import { CommentleSkeleton } from './components/CommentleSkeleton';
import { Modal } from '@/shared/components/Modal';
import { Trophy } from 'lucide-react';
import './Commentle.css';

export const CommentleQuizPage: React.FC = () => {
    const {
        guesses,
        loading,
        showSuccess,
        setShowSuccess,
        leaderboard,
        problem,
        problemLoading,
        handleGuess,
    } = useCommentleGame();

    if (problemLoading) {
        return <UserLayoutV2><CommentleSkeleton /></UserLayoutV2>;
    }

    return (
        <UserLayoutV2>
            <div className="w-full max-w-[1600px] mx-auto h-full flex flex-col overflow-hidden">
                <div className="shrink-0 px-4 pt-3 pb-1">
                    <CommentleHeader />
                </div>

                {/* 메인 게임 영역 - 남은 높이를 꽉 채움 */}
                <div className="flex-1 flex flex-col lg:flex-row gap-5 px-4 pb-3 min-h-0">

                    {/* 왼쪽: 게임 핵심 영역 - 독립 스크롤 (스크롤바 숨김) */}
                    <div
                        className="w-full lg:w-[45%] lg:min-w-[400px] lg:max-w-[600px] overflow-y-auto scrollbar-hide"
                        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                    >
                        {/* 문제 정보 */}
                        <div className="mb-3">
                            <CommentleProblemCard
                                problem={problem}
                                attemptCount={guesses.length}
                            />
                        </div>

                        {/* 입력 영역 */}
                        <div className="mb-3">
                            <CommentleInputSection
                                onGuess={handleGuess}
                                loading={loading}
                            />
                        </div>

                        {/* 시도 기록 */}
                        <CommentleHistory guesses={guesses} />

                        {/* 모바일: 3D 뷰 */}
                        <div className="lg:hidden mt-4">
                            <Commentle3DView guesses={guesses} />
                        </div>

                        {/* 모바일: 리더보드 */}
                        <div className="lg:hidden mt-4">
                            <CommentleStatsBoard leaderboard={leaderboard} />
                        </div>
                    </div>

                    {/* 오른쪽: 시각화 영역 (데스크탑만) - 독립 스크롤 (스크롤바 숨김) */}
                    <div
                        className="hidden lg:flex flex-1 flex-col gap-4 overflow-y-auto scrollbar-hide"
                        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
                    >
                        <div>
                            <Commentle3DView guesses={guesses} />
                        </div>
                        <div>
                            <CommentleStatsBoard leaderboard={leaderboard} />
                        </div>
                    </div>
                </div>

                {/* Success Modal */}
                <Modal
                    isOpen={showSuccess}
                    onClose={() => setShowSuccess(false)}
                    title="축하합니다! 🎉"
                >
                    <div className="flex flex-col items-center justify-center p-6 text-center">
                        <div className="relative mb-4">
                            <Trophy size={64} className="text-yellow-400 animate-bounce" />
                            <div className="absolute inset-0 blur-2xl bg-yellow-400/20 -z-10 animate-pulse" />
                        </div>
                        <h2 className="text-2xl font-black text-text-primary mb-2">정답입니다!</h2>
                        <p className="text-base text-text-secondary">
                            단 <strong className="text-primary text-xl mx-1">{guesses.length}</strong>번 만에 맞추셨어요!
                        </p>
                        <button
                            onClick={() => setShowSuccess(false)}
                            className="mt-8 w-full py-3 bg-primary text-white rounded-2xl font-bold text-base hover:bg-primary-dark hover:scale-[1.02] active:scale-[0.98] transition-all shadow-lg shadow-primary/20"
                        >
                            멋져요! 계속하기 😎
                        </button>
                    </div>
                </Modal>
            </div>
        </UserLayoutV2>
    );
};
