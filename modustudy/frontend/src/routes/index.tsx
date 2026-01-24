import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { LoadingFallback } from '../components';

// 즉시 로드: 랜딩 및 핵심 페이지
import { StartPage } from '../features/start/StartPage';
import { Dashboard, CalendarExpandWidget } from '../features/dashboard';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import ReuseTest from '../features/reuseTest';

// Lazy 로드: 나머지 페이지들
const CommentlePage = lazy(() =>
    import('../features/commentle/CommentlePage').then(m => ({ default: m.CommentlePage }))
);
const QuizGameSelection = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.QuizGameSelection }))
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
const StudyCreatePage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyCreatePage }))
);
const StudyDetailPage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyDetailPage }))
);
const StudyManagementPage = lazy(() =>
    import('../features/study').then(m => ({ default: m.StudyManagementPage }))
);
const TestSidebarPage = lazy(() =>
    import('../features/test/TestSidebarPage').then(m => ({ default: m.TestSidebarPage }))
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

export const AppRouter = () => {
    const { login, logout, setInitialized } = useAuthStore();

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
                logout(); // 토큰이 만료되었거나 잘못된 경우 로그아웃 처리
            } finally {
                setInitialized(true);
            }
        };

        initAuth();
    }, [login, logout, setInitialized]);

    return (
        <BrowserRouter>
            <Suspense fallback={<LoadingFallback />}>
                <Routes>
                    {/* 즉시 로드 페이지 */}
                    <Route path="/" element={<StartPage />} />
                    <Route path="/startpage" element={<StartPage />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/calendar-expand" element={<CalendarExpandWidget />} />
                    <Route path="/reuse-test" element={<ReuseTest />} />

                    {/* Lazy 로드 페이지 */}
                    <Route path="/commentle" element={<CommentlePage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/login/callback" element={<LoginCallbackPage />} />
                    <Route path="/password/reset" element={<PasswordResetPage />} />
                    <Route path="/signup" element={<SignupPage />} />
                    <Route path="/quiz" element={<QuizGameSelection />} />
                    <Route path="/quiz-commentle" element={<CommentleQuiz />} />
                    <Route path="/quiz-practice" element={<QuizCourseList />} />
                    <Route path="/quiz-practice/:courseId" element={<CourseDetail />} />
                    <Route path="/quiz-practice/:courseId/section/:sectionId/session" element={<QuizSessionPage />} />
                    <Route path="/study" element={<StudyPage />} />
                    <Route path="/study/create" element={<StudyCreatePage />} />
                    <Route path="/study/:id" element={<StudyDetailPage />} />
                    <Route path="/study/manage/:id" element={<StudyManagementPage />} />
                    <Route path="/study/:studyId/meetings" element={<MeetingHistoryPage />} />
                    <Route path="/study/:studyId/meetings/:meetingId" element={<MeetingDetailPage />} />
                    <Route path="/study/:studyId/meetings/:meetingId/room" element={<MeetingRoomPage />} />
                    <Route path="/recruitment" element={<RecruitmentPage />} />
                    <Route path="/profile" element={<ProfilePage />} />
                </Routes>
            </Suspense>
        </BrowserRouter>
    );
};
