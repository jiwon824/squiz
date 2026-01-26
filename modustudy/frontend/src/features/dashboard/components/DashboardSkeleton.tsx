import { Skeleton } from '@/shared/components';
import '../styles/Dashboard.css';

export const DashboardSkeleton = () => {
    return (
        <div className="dashboard-grid">
            {/* Activity Section Skeleton */}
            <div className="dashboard-card opacity-50">
                <Skeleton variant="rect" height={250} className="w-full" />
            </div>

            {/* Stats Section Skeleton */}
            <div className="dashboard-card opacity-50">
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <Skeleton variant="rect" height={100} count={4} />
                </div>
            </div>

            {/* Quiz Widgets Skeletons */}
            <div className="quiz-daily">
                <Skeleton variant="rect" height={150} className="w-full" />
            </div>
            <div className="quiz-comp">
                <Skeleton variant="rect" height={150} className="w-full" />
            </div>

            {/* Feeds Section Skeleton */}
            <div className="dashboard-card opacity-50">
                <div className="space-y-4">
                    <Skeleton variant="text" width="60%" height={24} />
                    <Skeleton variant="rect" height={300} className="w-full" />
                </div>
            </div>
        </div>
    );
};
