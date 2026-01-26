import { ActivitySection, StatsSection, FeedsSection, QuizWidget, QuizWidget2, CalendarExpandWidget, GuestDashboard } from './components';
import './styles/Dashboard.css';
import { MainLayout } from '@/layouts/MainLayout';
import { useAuthStore } from '@/store/authStore';
import { DashboardSkeleton } from './components/DashboardSkeleton';
export { CalendarExpandWidget, DashboardSkeleton };

const UserDashboard = () => (
    <div className="dashboard-grid">
        <ActivitySection />
        <StatsSection />
        <div className="quiz-daily">
            <QuizWidget />
        </div>
        <div className="quiz-comp">
            <QuizWidget2 />
        </div>
        <FeedsSection />
    </div>
);

export const Dashboard = () => {
    const { isLoggedIn } = useAuthStore();

    return (
        <MainLayout>
            <div className="dashboard-container">
                {isLoggedIn ? <UserDashboard /> : <GuestDashboard />}
            </div>
        </MainLayout>
    );
};
