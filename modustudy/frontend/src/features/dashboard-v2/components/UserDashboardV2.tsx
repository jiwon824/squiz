import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Sparkles, Calendar, Trophy, LucideIcon } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { PostIt, getPostItTheme, PostItColor } from '@/shared/components/layouts/PostIt';
import { STTReportWidget } from './STTReportWidget';
import { MeetingTestWidget } from './MeetingTestWidget';
import { MyQuizWidget } from './MyQuizWidget';

import { TodayGoalsCard } from './TodayGoalsCard';
import { MyCreatedStudiesWidget } from './MyCreatedStudiesWidget';
import { MyApplicationsWidget } from './MyApplicationsWidget';
import { calendarApi, StudySessionDTO } from '@/api/endpoints/calendarApi';
import { studyApi } from '@/api/endpoints/studyApi';
import { formatDate } from '@/features/calendar/utils';
import { gamificationApi, UserStatsResponse } from '@/api/endpoints/gamificationApi';

// 다가오는 미팅 정보
interface UpcomingMeeting {
    studyName: string;
    meetingTitle: string;
    scheduledAt: Date;
}

// 다음 세션 결정 함수
const resolveNextSession = (sessions: StudySessionDTO[], currentTime: Date) => {
    const candidates = sessions.filter((session) => {
        if (session.status === 'CANCELLED') return false;
        const startAt = new Date(session.scheduledAt).getTime();
        const durationMinutes = session.durationMinutes || 60;
        const endAt = startAt + durationMinutes * 60 * 1000;
        return endAt >= currentTime.getTime();
    });

    const inProgress = candidates.filter((session) => {
        const startAt = new Date(session.scheduledAt).getTime();
        const durationMinutes = session.durationMinutes || 60;
        const endAt = startAt + durationMinutes * 60 * 1000;
        return startAt <= currentTime.getTime() && currentTime.getTime() <= endAt;
    });

    if (inProgress.length > 0) {
        return inProgress.sort(
            (a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime()
        )[0];
    }

    const upcoming = candidates
        .filter((session) => new Date(session.scheduledAt).getTime() > currentTime.getTime())
        .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime());

    return upcoming[0] || null;
};

export const UserDashboardV2: React.FC = () => {
    const { user } = useAuthStore();
    const [upcomingMeetings, setUpcomingMeetings] = useState<UpcomingMeeting[]>([]);
    const [gamificationStats, setGamificationStats] = useState<UserStatsResponse | null>(null);

    // 게이미피케이션 통계 로드
    useEffect(() => {
        const loadGamificationStats = async () => {
            try {
                const stats = await gamificationApi.getStats();
                setGamificationStats(stats);
            } catch (error) {
            }
        };

        loadGamificationStats();
    }, []);

    // 다가오는 일정 로드
    useEffect(() => {
        const loadUpcomingSchedule = async () => {
            try {
                const today = new Date();
                const endDate = new Date(today);
                endDate.setDate(endDate.getDate() + 30); // 30일 후까지

                const startDate = formatDate(today);
                const endDateString = formatDate(endDate);

                let sessions = await calendarApi.getMyStudySessions(startDate, endDateString);

                const studiesResponse = await studyApi.getMyStudies(0, 50);
                const studyMap = new Map<number, string>();
                studiesResponse.content.forEach((study) => {
                    studyMap.set(study.id, study.name);
                });

                // 내 세션이 없으면 각 스터디의 세션 가져오기
                if (sessions.length === 0 && studiesResponse.content.length > 0) {
                    const sessionLists = await Promise.all(
                        studiesResponse.content.map(async (study) => {
                            try {
                                return await calendarApi.getStudySessions(study.id, startDate, endDateString);
                            } catch (error) {
                                return [];
                            }
                        })
                    );
                    sessions = sessionLists.flat();
                }

                // 스터디별로 세션 그룹화
                const sessionsByStudy = new Map<number, StudySessionDTO[]>();
                sessions.forEach((session: StudySessionDTO) => {
                    const list = sessionsByStudy.get(session.studyId) || [];
                    list.push(session);
                    sessionsByStudy.set(session.studyId, list);
                });

                // 각 스터디의 가장 임박한 일정 하나씩
                const studyIds = Array.from(sessionsByStudy.keys());
                const meetings: UpcomingMeeting[] = studyIds
                    .map((studyId) => {
                        const nextSession = resolveNextSession(sessionsByStudy.get(studyId) || [], today);
                        if (!nextSession) return null;

                        return {
                            studyName: studyMap.get(studyId) || `스터디 ${studyId}`,
                            meetingTitle: nextSession.title || '스터디 세션',
                            scheduledAt: new Date(nextSession.scheduledAt),
                        } as UpcomingMeeting;
                    })
                    .filter((item): item is UpcomingMeeting => item !== null)
                    .sort((a, b) => a.scheduledAt.getTime() - b.scheduledAt.getTime())
                    .slice(0, 3); // 최대 3개만

                setUpcomingMeetings(meetings);
            } catch (error) {
            }
        };

        loadUpcomingSchedule();
    }, []);

    // 일정 포맷팅 함수
    const formatScheduleItem = (meeting: UpcomingMeeting): string => {
        const now = new Date();
        const scheduledDate = new Date(meeting.scheduledAt);

        // 날짜만 비교하기 위해 시간을 00:00:00으로 설정
        const nowDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const scheduleDate = new Date(scheduledDate.getFullYear(), scheduledDate.getMonth(), scheduledDate.getDate());

        const diffMs = scheduleDate.getTime() - nowDate.getTime();
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

        const diff = meeting.scheduledAt.getTime() - now.getTime();
        const diffHours = Math.floor(diff / (1000 * 60 * 60));

        let timeText = '';
        if (diffDays === 0) {
            // 오늘
            if (diffHours === 0) {
                timeText = '지금';
            } else if (diff > 0) {
                timeText = `오늘 ${meeting.scheduledAt.getHours()}시`;
            } else {
                timeText = '진행 중';
            }
        } else if (diffDays === 1) {
            // 내일
            timeText = `내일 ${meeting.scheduledAt.getHours()}시`;
        } else if (diffDays > 1 && diffDays <= 7) {
            // 2~7일 후
            timeText = `${diffDays}일 후`;
        } else {
            // 7일 이후
            timeText = `${meeting.scheduledAt.getMonth() + 1}/${meeting.scheduledAt.getDate()}`;
        }

        return `${meeting.studyName} (${timeText})`;
    };

    const scheduleItems = upcomingMeetings.length > 0
        ? upcomingMeetings.map(formatScheduleItem)
        : ['예정된 일정이 없습니다'];

    // 레벨업 진행 상황 계산
    const levelUpItems = (() => {
        if (!gamificationStats) {
            return ['통계를 불러오는 중...'];
        }

        const { level, levelProgress, nextLevel } = gamificationStats;

        // 최대 레벨 체크
        if (level >= 6) {
            return ['최대 레벨 달성! 🎉', '축하합니다!'];
        }

        const remainingXp = levelProgress.required - levelProgress.current;

        // 경험치 상수 (ExperienceConfig.java 참고)
        const STUDY_ATTENDANCE_XP = 10;  // 스터디 출석
        const QUIZ_CORRECT_XP = 5;       // 퀴즈 정답

        // 필요한 스터디 횟수 (올림)
        const studiesNeeded = Math.ceil(remainingXp / STUDY_ATTENDANCE_XP);
        // 필요한 퀴즈 횟수 (올림)
        const quizzesNeeded = Math.ceil(remainingXp / QUIZ_CORRECT_XP);

        return [
            `다음 레벨: ${nextLevel.name} (Lv.${nextLevel.level})`,
            <span>스터디 <strong className="text-lg font-black text-pink-600">{studiesNeeded}회</strong> 남음</span>,
            <span>퀴즈 <strong className="text-lg font-black text-pink-600">{quizzesNeeded}문제</strong> 남음</span>,
        ];
    })();

    return (
        <div
            className="flex-1 overflow-y-auto"
            style={{
                backgroundImage:
                    'linear-gradient(to right, rgba(0,0,0,0.03) 1px, transparent 1px), linear-gradient(to bottom, rgba(0,0,0,0.03) 3px, transparent 2px)',
                backgroundSize: '40px 40px',
            }}
        >
            <div className="max-w-7xl mx-auto px-4 py-6 space-y-6 sm:px-6 sm:py-8 sm:space-y-8 lg:px-8">
                {/* 환영 메시지 + 포스트잇 */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex flex-col lg:flex-row gap-4 sm:gap-6"
                >
                    {/* 좌측: 인사말 */}
                    <div className="bg-gradient-to-r from-primary/10 to-secondary/10 rounded-2xl pt-6 px-4 pb-3 sm:pt-8 sm:px-8 sm:pb-4 lg:flex-shrink-0 flex flex-col justify-center">
                        <div className="flex items-center gap-3 mb-3">
                            <Sparkles className="text-primary flex-shrink-0" size={28} />
                            <h1 className="text-xl sm:text-3xl font-black text-text-primary mb-0 whitespace-nowrap">
                                안녕하세요 {user?.nickname || user?.name || ''}님! 👋
                            </h1>
                        </div>
                        <p className="text-text-secondary text-sm sm:text-lg">오늘도 즐거운 학습 되세요!</p>
                    </div>

                    {/* 우측: 포스트잇 3개 - 엇갈림 배치 */}
                    <div className="flex-1 min-w-0 grid grid-cols-3 gap-5 items-start py-2 px-4">
                        <div className="mt-2">
                            <TodayGoalsCard />
                        </div>
                        <div className="-mt-1">
                            <InfoCard
                                icon={Calendar}
                                title="다가오는 일정"
                                items={scheduleItems}
                                color="green"
                                rotate={-1.5}
                                tapeRotate={2}
                            />
                        </div>
                        <div className="mt-3">
                            <InfoCard
                                icon={Trophy}
                                title="레벨업 진행"
                                items={levelUpItems}
                                color="pink"
                                rotate={-0.8}
                                tapeRotate={-1}
                            />
                        </div>
                    </div>
                </motion.div>

                {/* 내 스터디 위젯 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6 items-stretch">
                    <MyCreatedStudiesWidget />
                    <MyApplicationsWidget />
                </div>

                {/* 미팅 리포트 (전체 행) */}
                <STTReportWidget />

                {/* 테스트 + 복습 퀴즈 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
                    <MeetingTestWidget />
                    <MyQuizWidget />
                </div>

            </div>
        </div>
    );
};

// 포스트잇 스타일 정보 카드 컴포넌트
interface InfoCardProps {
    icon: LucideIcon;
    title: string;
    items: (string | React.ReactNode)[];
    color: PostItColor;
    rotate?: number;
    tapeRotate?: number;
}

const InfoCard: React.FC<InfoCardProps> = ({ icon: Icon, title, items, color, rotate = 0, tapeRotate }) => {
    const theme = getPostItTheme(color);

    return (
        <PostIt color={color} rotate={rotate} tapeRotate={tapeRotate}>
            {/* 헤더 */}
            <div className="flex items-center gap-2.5 mb-4 mt-1">
                <Icon size={20} style={{ color: theme.text }} />
                <h3
                    className="font-bold text-lg leading-6 mb-0"
                    style={{ color: theme.text }}
                >
                    {title}
                </h3>
            </div>

            {/* 리스트 */}
            <ul className="space-y-2.5">
                {items.map((item, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-sm" style={{ color: theme.sub }}>
                        <span className="mt-1 text-xs leading-none">•</span>
                        <span>{item}</span>
                    </li>
                ))}
            </ul>
        </PostIt>
    );
};
