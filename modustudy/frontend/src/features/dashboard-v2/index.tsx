import { GuestDashboardV2 } from './components/GuestDashboardV2';
import { UserDashboardV2 } from './components/UserDashboardV2';
import { GuestLayoutV2 } from '@/layouts/GuestLayoutV2';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { useAuthStore } from '@/store/authStore';

export const DashboardV2 = () => {
    const { isLoggedIn } = useAuthStore();

    if (isLoggedIn) {
        return (
            <UserLayoutV2>
                <UserDashboardV2 />
            </UserLayoutV2>
        );
    }

    return (
        <GuestLayoutV2>
            <GuestDashboardV2 />
        </GuestLayoutV2>
    );
};

// 개별 컴포넌트 export
export { GuestDashboardV2 } from './components/GuestDashboardV2';
export { UserDashboardV2 } from './components/UserDashboardV2';
export { STTReportWidget } from './components/STTReportWidget';
export { MyQuizWidget as AIQuizWidget } from './components/MyQuizWidget';
export { LearningArchiveWidget } from './components/LearningArchiveWidget';
export { TodayGoalsCard } from './components/TodayGoalsCard';
export { GoalsEditModal } from './components/GoalsEditModal';

// Store export (캘린더 등 다른 페이지에서 사용)
export { useGoalsStore } from './store/goalsStore';
export type { Goal } from './store/goalsStore';
