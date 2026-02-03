import React, { Suspense, lazy, useEffect } from 'react';
import { Box } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { Guess } from '../hooks/useCommentleGame';

// lazy 로드 + preload 함수
const importEmbedding3DViewer = () => import('../Embedding3DViewer.tsx');
const Embedding3DViewer = lazy(importEmbedding3DViewer) as React.ComponentType<{ guesses: Guess[] }>;

interface Commentle3DViewProps {
    guesses: Guess[];
}

export const Commentle3DView: React.FC<Commentle3DViewProps> = ({ guesses }) => {
    // 컴포넌트 마운트 시 미리 로드 시작
    useEffect(() => {
        importEmbedding3DViewer();
    }, []);
    return (
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-1.5 shadow-2xl h-full min-h-[600px] flex flex-col overflow-hidden group">
            <div className="flex items-center gap-3 px-6 py-4 border-b border-slate-800/50">
                <div className="bg-primary/20 p-2 rounded-xl text-primary-light">
                    <Box size={20} />
                </div>
                <h3 className="font-bold text-slate-100 uppercase tracking-wider text-sm">3D Semantic Space</h3>
                <div className="ml-auto flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                    <span className="text-[10px] font-black text-slate-500 uppercase tracking-tighter">Live Visualization</span>
                </div>
            </div>

            <div className="flex-1 relative bg-slate-950/50 rounded-3xl m-1 overflow-hidden">
                <Suspense fallback={
                    <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-900/80 backdrop-blur-sm z-10">
                        <div className="relative">
                            <Spinner size="xl" className="text-primary" />
                            <div className="absolute inset-0 blur-xl bg-primary/20 animate-pulse" />
                        </div>
                        <p className="mt-4 text-slate-400 font-bold text-sm tracking-widest uppercase">Rendering Space...</p>
                    </div>
                }>
                    <Embedding3DViewer guesses={guesses} />
                </Suspense>
            </div>

            {/* Legend/Info Area */}
            <div className="px-6 py-4 bg-slate-900/50 flex items-center justify-between">
                <div className="flex gap-4">
                    <div className="flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-[#22c55e]" />
                        <span className="text-[10px] font-bold text-slate-400 uppercase">Correct</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-primary" />
                        <span className="text-[10px] font-bold text-slate-400 uppercase">Near</span>
                    </div>
                </div>
                <span className="text-[10px] font-medium text-slate-600">DRAG TO ROTATE • SCROLL TO ZOOM</span>
            </div>
        </div>
    );
};
