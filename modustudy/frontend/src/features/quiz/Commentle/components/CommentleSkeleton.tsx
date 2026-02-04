import { Skeleton } from '@/shared/components';

export const CommentleSkeleton = () => {
    return (
        <div className="w-full max-w-[1400px] mx-auto animate-fade-in p-6">
            {/* Header Skeleton */}
            <div className="flex justify-between items-center mb-8">
                <Skeleton variant="rect" width={150} height={48} className="rounded-xl" />
                <Skeleton variant="circle" width={40} height={40} />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Left Panel */}
                <div className="space-y-8">
                    {/* Problem Card Skeleton */}
                    <div className="bg-white border border-border-light rounded-[32px] p-8 shadow-sm">
                        <div className="flex gap-4 mb-8">
                            <Skeleton variant="rect" width={120} height={40} className="rounded-2xl" />
                            <Skeleton variant="rect" width={120} height={40} className="rounded-2xl" />
                        </div>
                        <div className="space-y-4">
                            <Skeleton variant="text" width="100%" height={24} />
                            <div className="flex gap-3">
                                <Skeleton variant="rect" width={80} height={40} className="rounded-xl" />
                                <Skeleton variant="rect" width={80} height={40} className="rounded-xl" />
                                <Skeleton variant="rect" width={80} height={40} className="rounded-xl" />
                            </div>
                        </div>
                    </div>

                    {/* Input Section Skeleton */}
                    <div className="bg-white border border-border-light rounded-[24px] p-6 shadow-sm">
                        <Skeleton variant="text" width={100} className="mb-4" />
                        <Skeleton variant="rect" width="100%" height={64} className="rounded-2xl" />
                    </div>

                    {/* History Skeleton */}
                    <div className="bg-white border border-border-light rounded-[28px] p-6 shadow-sm">
                        <div className="flex justify-between mb-6">
                            <Skeleton variant="rect" width={120} height={32} className="rounded-xl" />
                            <Skeleton variant="rect" width={80} height={24} className="rounded-full" />
                        </div>
                        <div className="space-y-3">
                            {Array.from({ length: 4 }).map((_, i) => (
                                <Skeleton key={i} variant="rect" width="100%" height={72} className="rounded-2xl" />
                            ))}
                        </div>
                    </div>
                </div>

                {/* Right Panel */}
                <div className="space-y-6">
                    {/* 3D View Skeleton */}
                    <div className="bg-slate-900 border border-slate-800 rounded-[32px] p-1.5 h-[600px] flex flex-col">
                        <div className="p-4 border-b border-slate-800/50">
                            <Skeleton variant="rect" width={200} height={24} className="bg-slate-800" />
                        </div>
                        <div className="flex-1 m-1 bg-slate-800/20 rounded-[28px] flex items-center justify-center">
                            <Skeleton variant="circle" width={100} height={100} className="bg-slate-800/40" />
                        </div>
                    </div>

                    {/* Stats Board Skeleton */}
                    <div className="bg-white border border-border-light rounded-[28px] p-6 shadow-sm">
                        <Skeleton variant="rect" width={150} height={24} className="mb-6 rounded-xl" />
                        <div className="space-y-2">
                            {Array.from({ length: 5 }).map((_, i) => (
                                <Skeleton key={i} variant="rect" width="100%" height={56} className="rounded-2xl" />
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};
