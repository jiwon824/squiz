import React from 'react';
import { Skeleton } from '@/shared/components';
import '../styles/ProfilePage.css';

export const ProfileSkeleton = () => {
    return (
        <div className="profile-page">
            <div className="profile-container">
                {/* Profile Header Skeleton */}
                <div className="profile-header-card bg-white p-8 rounded-[32px] shadow-sm mb-8 flex flex-col md:flex-row items-center gap-8">
                    <Skeleton variant="circle" width={120} height={120} />
                    <div className="flex-1 space-y-4">
                        <Skeleton variant="text" width={200} height={32} />
                        <Skeleton variant="text" width={150} />
                        <Skeleton variant="text" width="100%" height={60} />
                    </div>
                </div>

                {/* Stats Section Skeleton */}
                <div className="stats-section">
                    <Skeleton variant="text" width={120} height={28} className="mb-6" />
                    <div className="stats-grid-dashboard">
                        {Array.from({ length: 8 }).map((_, i) => (
                            <Skeleton key={i} variant="rect" height={110} className="rounded-2xl" />
                        ))}
                    </div>
                </div>

                {/* Activity Graph Skeleton */}
                <div className="mt-12">
                    <Skeleton variant="text" width={150} height={28} className="mb-6" />
                    <Skeleton variant="rect" height={200} className="w-full rounded-2xl" />
                </div>
            </div>
        </div>
    );
};
