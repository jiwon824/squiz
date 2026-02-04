import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Skeleton } from '../shared/components';
import { ErrorBoundary } from '../features/error/ErrorBoundary';

// Dashboard V2 (메인 대시보드)
import { DashboardV2, GuestDashboardV2, UserDashboardV2 } from '../features/dashboard-v2';
import { MyQuizPage } from '../features/dashboard-v2/pages/MyQuizPage';
import { STTReportPage } from '../features/dashboard-v2/pages/STTReportPage';
import { MeetingTestPage } from '../features/dashboard-v2/pages/MeetingTestPage';
import { LearningArchivePage } from '../features/dashboard-v2/pages/LearningArchivePage';
import { MyCreatedStudiesPage } from '../features/dashboard-v2/pages/MyCreatedStudiesPage';
import { MyApplicationsPage } from '../features/dashboard-v2/pages/MyApplicationsPage';
import { GuestLayoutV2 } from '@/layouts/GuestLayoutV2';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { CalendarTestPage } from '../features/calendar/components/CalendarTestPage';
import { CalendarPage } from '../features/calendar/components/CalendarPage';
import { authApi } from '@/api/endpoints/authApi';
import { useAuthStore } from '@/store/authStore';
import { PrivateRoute } from './PrivateRoute';
import { PublicRoute } from './PublicRoute';
import ReuseTest from '../features/reuseTest';

// Lazy 로드: 나머지 페이지들
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
const ContinuousQuizSessionPage = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.ContinuousQuizSession }))
);
const QuizContestComingSoon = lazy(() =>
    import('../features/quiz').then(m => ({ default: m.QuizContestComingSoon }))
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
const LightningStudyEditPage = lazy(() =>
    import('../features/study').then(m => ({ default: m.LightningStudyEditPage }))
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
const NotificationPage = lazy(() =>
    import('../features/notification/pages/NotificationPage').then(m => ({ default: m.NotificationPage }))
);
const ErrorPage = lazy(() =>
    import('../features/error/ErrorPage').then(m => ({ default: m.ErrorPage }))
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
        <ErrorBoundary>
        <Suspense fallback={<div className="p-6"><Skeleton variant="rect" height="100vh" /></div>}>
            <Routes>
                {/* 메인 대시보드 (로그인 여부에 따라 Guest/User 자동 분기) */}
                <Route path="/" element={<DashboardV2 />} />
                <Route path="/dashboard" element={<DashboardV2 />} />
                <Route path="/dashboard/guest" element={<GuestLayoutV2><GuestDashboardV2 /></GuestLayoutV2>} />
                <Route path="/dashboard/user" element={<PrivateRoute><UserLayoutV2><UserDashboardV2 /></UserLayoutV2></PrivateRoute>} />
                <Route path="/calendar" element={<PrivateRoute><UserLayoutV2><CalendarPage /></UserLayoutV2></PrivateRoute>} />
                <Route path="/test-calendar" element={<CalendarTestPage />} />
                <Route path="/reuse-test" element={<ReuseTest />} />
                <Route path="/workspace-test" element={<PrivateRoute><WorkspacePage /></PrivateRoute>} />

                {/* 인증 (로그인 상태면 대시보드로 리다이렉트) */}
                <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
                <Route path="/login/callback" element={<LoginCallbackPage />} />
                <Route path="/signup" element={<PublicRoute><SignupPage /></PublicRoute>} />
                <Route path="/password/reset" element={<PublicRoute><PasswordResetPage /></PublicRoute>} />

                {/* 퀴즈 */}
                <Route path="/quiz" element={<PrivateRoute><UserLayoutV2><QuizGameSelection /></UserLayoutV2></PrivateRoute>} />
                <Route path="/quiz/my-quiz" element={<PrivateRoute><UserLayoutV2><MyQuizPage /></UserLayoutV2></PrivateRoute>} />
                <Route path="/quiz-commentle" element={<CommentleQuiz />} />
                <Route path="/quiz-contest" element={<PrivateRoute><UserLayoutV2><QuizContestComingSoon /></UserLayoutV2></PrivateRoute>} />
                <Route path="/quiz-practice" element={<QuizCourseList />} />
                <Route path="/quiz-practice/:courseId" element={<CourseDetail />} />
                <Route
                    path="/continuous-quiz/:courseId/section/:sectionNumber"
                    element={<ContinuousQuizSessionPage />}
                />

                {/* 개인 페이지 (로그인 필수) */}
                <Route path="/meeting-report" element={<PrivateRoute><UserLayoutV2><STTReportPage /></UserLayoutV2></PrivateRoute>} />
                <Route path="/meeting-test" element={<PrivateRoute><UserLayoutV2><MeetingTestPage /></UserLayoutV2></PrivateRoute>} />
                <Route path="/learning-archive" element={<PrivateRoute><UserLayoutV2><LearningArchivePage /></UserLayoutV2></PrivateRoute>} />
                <Route path="/my-studies/created" element={<PrivateRoute><UserLayoutV2><MyCreatedStudiesPage /></UserLayoutV2></PrivateRoute>} />
                <Route path="/my-studies/applications" element={<PrivateRoute><UserLayoutV2><MyApplicationsPage /></UserLayoutV2></PrivateRoute>} />

                {/* 스터디 */}
                <Route path="/study" element={<StudyPage />} />
                <Route path="/study/create" element={<PrivateRoute><StudyTypeSelectPage /></PrivateRoute>} />
                <Route path="/study/create/planned" element={<PrivateRoute><StudyCreatePage /></PrivateRoute>} />
                <Route path="/study/create/lightning" element={<PrivateRoute><LightningStudyCreatePage /></PrivateRoute>} />
                <Route path="/study/edit/lightning/:studyId" element={<PrivateRoute><LightningStudyEditPage /></PrivateRoute>} />
                <Route path="/study/:id" element={<StudyDetailPageV3 />} />
                <Route path="/study/manage/:id" element={<PrivateRoute><StudyManagementPage /></PrivateRoute>} />
                <Route path="/study/:studyId/workspace" element={<PrivateRoute><WorkspacePage /></PrivateRoute>} />

                {/* 미팅 (로그인 필수) */}
                <Route path="/study/:studyId/meetings" element={<PrivateRoute><MeetingHistoryPage /></PrivateRoute>} />
                <Route path="/study/:studyId/meetings/:meetingId" element={<PrivateRoute><MeetingDetailPage /></PrivateRoute>} />
                <Route path="/study/:studyId/meetings/:meetingId/room" element={<PrivateRoute><MeetingRoomPage /></PrivateRoute>} />

                {/* 기타 */}
                <Route path="/recruitment" element={<RecruitmentPage />} />
                <Route path="/notifications" element={<PrivateRoute><NotificationPage /></PrivateRoute>} />
                <Route path="/setting" element={<PrivateRoute><SettingPage /></PrivateRoute>} />
                <Route
                    path="/profile"
                    element={
                        <PrivateRoute>
                            <Suspense fallback={<ProfileSkeleton />}>
                                <ProfilePage />
                            </Suspense>
                        </PrivateRoute>
                    }
                />
                <Route path="/admin" element={<PrivateRoute><AdminDashboardPage /></PrivateRoute>} />

                {/* 에러 페이지 */}
                <Route path="/error" element={<ErrorPage />} />
                <Route path="/error/:code" element={<ErrorPage />} />
                <Route path="*" element={<ErrorPage />} />
            </Routes>
        </Suspense>
        </ErrorBoundary>
    );
};
