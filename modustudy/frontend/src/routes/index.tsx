import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Skeleton } from '../shared/components';

// Dashboard V2 (메인 대시보드)
import { DashboardV2, GuestDashboardV2, UserDashboardV2 } from '../features/dashboard-v2';
import { StudyAfterQuiz } from '../features/dashboard-v2/pages/StudyAfterQuiz';
import { STTReportPage } from '../features/dashboard-v2/pages/STTReportPage';
import { LearningArchivePage } from '../features/dashboard-v2/pages/LearningArchivePage';
import { GuestLayoutV2 } from '@/layouts/GuestLayoutV2';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { CalendarTestPage } from '../features/calendar/components/CalendarTestPage';
import { CalendarPage } from '../features/calendar/components/CalendarPage';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import ReuseTest from '../features/reuseTest';

// Lazy 로드: 나머지 페이지들
const QuizGameSelection = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.QuizGameSelection }))
);
const ReviewQuizPage = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.ReviewQuizPage }))
);
const CommentleQuiz = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.CommentleQuiz }))
);
const QuizCourseList = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.QuizCourseList }))
);
const CourseDetail = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.CourseDetail }))
);
const QuizSessionPage = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.QuizSession }))
);
const LoginPage = lazy(() =>
    import('../features/auth/index').then(m => ({ default: m.LoginPage }))
);
const SignupPage = lazy(() =>
    import('../features/auth/index').then(m => ({ default: m.SignupPage }))
);
const LoginCallbackPage = lazy(() =>
    import('../features/auth/index').then(m => ({ default: m.LoginCallbackPage }))
);
const PasswordResetPage = lazy(() =>
    import('../features/auth/components/PasswordResetPage').then(m => ({ default: m.PasswordResetPage }))
);
const RecruitmentPage = lazy(() =>
    import('../features/recruitment/RecruitmentPage').then(m => ({ default: m.RecruitmentPage }))
);
const StudyPage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyPage }))
);
const StudyTypeSelectPage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyTypeSelectPage }))
);
const StudyCreatePage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyCreatePage }))
);
const LightningStudyCreatePage = lazy(() =>
    import('../features/study').then(m => ({ default: m.LightningStudyCreatePage }))
);
const StudyDetailPageV3 = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyDetailPageV3 }))
);
const StudyManagementPage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyManagementPage }))
);
const SettingPage = lazy(() =>
    import('../features/setting/SettingPage').then(m => ({ default: m.SettingPage }))
);
const ProfilePage = lazy(() =>
    import('@/features/profile/components/ProfilePage').then(module => ({ default: module.ProfilePage }))
);
const MeetingHistoryPage = lazy(() =>
    import('../features/meeting').then(m => ({ default: m.MeetingHistoryPage }))
);
const MeetingDetailPage = lazy(() =>
    import('../features/meeting').then(m => ({ default: m.MeetingDetailPage }))
);
const MeetingRoomPage = lazy(() =>
    import('../features/meeting').then(m => ({ default: m.MeetingRoomPage }))
);
const WorkspacePage = lazy(() =>
    import('../features/workspace').then(m => ({ default: m.WorkspacePage }))
);

const ProfileSkeleton = lazy(() =>
    import('@/features/profile/components/ProfileSkeleton').then(module => ({ default: module.ProfileSkeleton }))
);
const AdminDashboardPage = lazy(() =>
    import('../features/admin').then(m => ({ default: m.AdminDashboardPage }))
);

export const AppRouter = () => {
    const { login, logout, isInitialized, setInitialized } = useAuthStore();

    useEffect(() => {
        const initAuth = async () => {
            const token = localStorage.getItem('accessToken');
            if (!token) {
                setInitialized(true);
                return;
            }

            try {
                const user = await authApi.getMe();
                login({
                    id: String(user.id),
                    name: user.name,
                    nickname: user.nickname || undefined,
                    email: user.email,
                    avatar: user.profileImage || undefined
                });
            } catch (error) {
                console.error('[AUTH] Session restoration failed:', error);
                logout();
            } finally {
                setInitialized(true);
            }
        };

        initAuth();
    }, [login, logout, setInitialized]);

    if (!isInitialized) {
        return <div className="p-6"><Skeleton variant="rect" height="100vh" /></div>;
    }

    return (
        <Suspense fallback={<div className="p-6"><Skeleton variant="rect" height="100vh" /></div>}>
            <Routes>
                {/* 메인 대시보드 (로그인 여부에 따라 Guest/User 자동 분기) */}
                <Route path="/" element={<DashboardV2 />} />
                <Route path="/dashboard" element={<DashboardV2 />} />
                <Route path="/dashboard/guest" element={<GuestLayoutV2><GuestDashboardV2 /></GuestLayoutV2>} />
                <Route path="/dashboard/user" element={<UserLayoutV2><UserDashboardV2 /></UserLayoutV2>} />
                <Route path="/calendar" element={<UserLayoutV2><CalendarPage /></UserLayoutV2>} />
                <Route path="/test-calendar" element={<CalendarTestPage />} />
                <Route path="/reuse-test" element={<ReuseTest />} />
                {/* origin/dev 추가 */}
                <Route path="/workspace-test" element={<WorkspacePage />} />

                {/* 인증 */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/login/callback" element={<LoginCallbackPage />} />
                <Route path="/signup" element={<SignupPage />} />
                <Route path="/password/reset" element={<PasswordResetPage />} />

                {/* 퀴즈 */}
                <Route path="/quiz" element={<QuizGameSelection />} />
                <Route path="/test/quiz" element={<ReviewQuizPage />} />
                {/* 내 브랜치 추가 */}
                <Route path="/quiz/review" element={<StudyAfterQuiz />} />
                <Route path="/stt-report" element={<STTReportPage />} />
                <Route path="/learning-archive" element={<LearningArchivePage />} />
                <Route path="/quiz-commentle" element={<CommentleQuiz />} />
                <Route path="/quiz-practice" element={<QuizCourseList />} />
                <Route path="/quiz-practice/:courseId" element={<CourseDetail />} />
                {/* origin/dev: sectionId -> sectionNumber 변경 */}
                <Route
                    path="/quiz-practice/:courseId/section/:sectionNumber/session"
                    element={<QuizSessionPage />}
                />
                <Route
                    path="/quiz-practice/:courseId/section/:sectionNumber/session/:attemptId"
                    element={<QuizSessionPage />}
                />

                {/* 스터디 */}
                <Route path="/study" element={<StudyPage />} />
                <Route path="/study/create" element={<StudyTypeSelectPage />} />
                <Route path="/study/create/planned" element={<StudyCreatePage />} />
                <Route path="/study/create/lightning" element={<LightningStudyCreatePage />} />
                <Route path="/study/:id" element={<StudyDetailPageV3 />} />
                <Route path="/study/v3/:id" element={<StudyDetailPageV3 />} />
                <Route path="/study/manage/:id" element={<StudyManagementPage />} />
                {/* origin/dev 추가 */}
                <Route path="/study/:studyId/workspace" element={<WorkspacePage />} />

                {/* 미팅 */}
                <Route path="/study/:studyId/meetings" element={<MeetingHistoryPage />} />
                <Route path="/study/:studyId/meetings/:meetingId" element={<MeetingDetailPage />} />
                <Route path="/study/:studyId/meetings/:meetingId/room" element={<MeetingRoomPage />} />

                {/* 기타 */}
                <Route path="/recruitment" element={<RecruitmentPage />} />
                <Route path="/setting" element={<SettingPage />} />
                <Route
                    path="/profile"
                    element={
                        <Suspense fallback={<ProfileSkeleton />}>
                            <ProfilePage />
                        </Suspense>
                    }
                />
                <Route path="/admin" element={<AdminDashboardPage />} />
            </Routes>
        </Suspense>
    );
};
