import React, { useEffect } from 'react';
import { RefreshCw } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { useAdminStore } from '../store/adminStore';
import SummaryCards from '../components/SummaryCards';
import UserSignupChart from '../components/UserSignupChart';
import StudyStatusChart from '../components/StudyStatusChart';
import LoginMethodChart from '../components/LoginMethodChart';
import QuizStatsChart from '../components/QuizStatsChart';
import RecentUsersTable from '../components/RecentUsersTable';
import PopularStudiesTable from '../components/PopularStudiesTable';
// 새로운 차트 컴포넌트
import ActivityTrendChart from '../components/ActivityTrendChart';
import UserLevelChart from '../components/UserLevelChart';
import StudyTopicChart from '../components/StudyTopicChart';

const AdminDashboardPage: React.FC = () => {
    const {
        summary,
        userSignupStats,
        studyStatusStats,
        loginMethodStats,
        quizStats,
        recentUsers,
        popularStudies,
        // 새로운 시계열 통계
        dailyMeetingStats,
        dailyAttendanceStats,
        dailyActivityStats,
        userLevelStats,
        studyTopicStats,
        isLoading,
        error,
        fetchDashboardData
    } = useAdminStore();

    useEffect(() => {
        fetchDashboardData();
    }, [fetchDashboardData]);

    if (isLoading && !summary) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="flex items-center gap-3 text-gray-600">
                    <Spinner size="md" label="대시보드 로딩중..." />
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="text-center">
                    <p className="text-red-500 mb-4">{error}</p>
                    <button
                        onClick={() => fetchDashboardData()}
                        className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 p-6">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-800">관리자 대시보드</h1>
                        <p className="text-gray-500 text-sm mt-1">시스템 현황 및 통계</p>
                    </div>
                    <button
                        onClick={() => fetchDashboardData()}
                        disabled={isLoading}
                        className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
                    >
                        <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
                        새로고침
                    </button>
                </div>

                {/* Summary Cards */}
                <div className="mb-6">
                    <SummaryCards summary={summary} />
                </div>

                {/* Charts Row 1: 시계열 차트 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <UserSignupChart data={userSignupStats} />
                    <ActivityTrendChart
                        meetingData={dailyMeetingStats}
                        attendanceData={dailyAttendanceStats}
                        activityData={dailyActivityStats}
                    />
                </div>

                {/* Charts Row 2: 분포 차트 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <StudyStatusChart data={studyStatusStats} />
                    <StudyTopicChart data={studyTopicStats} />
                </div>

                {/* Charts Row 3: 사용자/퀴즈 통계 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <LoginMethodChart data={loginMethodStats} />
                    <UserLevelChart data={userLevelStats} />
                </div>

                {/* Charts Row 4: 퀴즈 통계 */}
                <div className="mb-6">
                    <QuizStatsChart data={quizStats} />
                </div>

                {/* Tables Row */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <RecentUsersTable data={recentUsers} />
                    <PopularStudiesTable data={popularStudies} />
                </div>
            </div>
        </div>
    );
};

export default AdminDashboardPage;
