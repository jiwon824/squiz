import React from 'react';
import { MainLayout } from '@/layouts/MainLayout';
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
        return <MainLayout><CommentleSkeleton /></MainLayout>;
    }

    return (
        <MainLayout>
            <div className="w-full max-w-[1400px] mx-auto p-6 animate-fade-in">
                <CommentleHeader />

                {/* 2-Column Grid Layout for Large Screens */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                    {/* Left Column: Core Game Interaction */}
                    <div className="flex flex-col">
                        <section className="sticky top-6 space-y-8">
                            <div>
                                <h2 className="text-sm font-black text-text-tertiary uppercase tracking-widest mb-4">Problem Context</h2>
                                <CommentleProblemCard
                                    problem={problem}
                                    attemptCount={guesses.length}
                                />
                            </div>

                            <div>
                                <CommentleInputSection
                                    onGuess={handleGuess}
                                    loading={loading}
                                />
                            </div>

                            <div className="lg:hidden">
                                <Commentle3DView guesses={guesses} />
                            </div>

                            <div>
                                <CommentleHistory guesses={guesses} />
                            </div>
                        </section>
                    </div>

                    {/* Right Column: Visualization & Stats */}
                    <div className="hidden lg:flex flex-col gap-8">
                        <div className="sticky top-6 space-y-8">
                            <div>
                                <h2 className="text-sm font-black text-text-tertiary uppercase tracking-widest mb-4">Semantic Analytics</h2>
                                <Commentle3DView guesses={guesses} />
                            </div>

                            <div>
                                <h2 className="text-sm font-black text-text-tertiary uppercase tracking-widest mb-4">Community Stats</h2>
                                <CommentleStatsBoard leaderboard={leaderboard} />
                            </div>
                        </div>
                    </div>

                    {/* Mobile Stats Board (Shown at the bottom on mobile) */}
                    <div className="lg:hidden mt-8">
                        <h2 className="text-sm font-black text-text-tertiary uppercase tracking-widest mb-4">Community Stats</h2>
                        <CommentleStatsBoard leaderboard={leaderboard} />
                    </div>
                </div>

                {/* Success Modal */}
                <Modal
                    isOpen={showSuccess}
                    onClose={() => setShowSuccess(false)}
                    title="축하합니다! 🎉"
                >
                    <div className="flex flex-col items-center justify-center p-8 text-center">
                        <div className="relative mb-6">
                            <Trophy size={80} className="text-yellow-400 animate-bounce" />
                            <div className="absolute inset-0 blur-2xl bg-yellow-400/20 -z-10 animate-pulse" />
                        </div>
                        <h2 className="text-3xl font-black text-text-primary mb-2">정답입니다!</h2>
                        <p className="text-lg text-text-secondary">
                            단 <strong className="text-primary text-2xl mx-1">{guesses.length}</strong>번 만에 맞추셨어요!
                        </p>
                        <button
                            onClick={() => setShowSuccess(false)}
                            className="mt-10 w-full py-4 bg-primary text-white rounded-2xl font-bold text-lg hover:bg-primary-dark hover:scale-[1.02] active:scale-[0.98] transition-all shadow-lg shadow-primary/20"
                        >
                            멋져요! 계속하기 😎
                        </button>
                    </div>
                </Modal>
            </div>
        </MainLayout>
    );
};
