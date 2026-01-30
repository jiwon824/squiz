import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Heart, Users, Clock, MapPin,
    Target, Award, AlertTriangle, Share2,
    BookOpen, Monitor, Handshake, Layers, MoreVertical,
    Calendar, CalendarDays, Bookmark, FileText, GraduationCap, Info, Loader2, Pencil
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { studyService, Study } from '../services/studyService';
import { studyApi, getStudySessions, StudySessionItem } from '@/api/endpoints/studyApi';
import StudyApplyModalV2 from './StudyApplyModalV2';
import { StudyReportModal } from './StudyReportModal';
import LeaderReviewModal from './LeaderReviewModal';
import StudyListContainer from './StudyListContainer';
import StudyLeaderCard from './StudyLeaderCard';
import StudyCommentSection from './StudyCommentSection';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button, ArrowButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useUIStore } from '@/store/uiStore';
import { getReviewsByLeaderId, getLeaderAverageRating, getRegionById, LeaderReview } from '../mockData';

// API 응답 타입 (StudyResponse 구조)
interface StudyDetail {
    id: number;
    name: string;
    intro?: string;
    description?: string;
    topic?: { id: number; name: string; icon?: string; parent?: { id: number; name: string } };
    format?: { id: number; name: string; description?: string };
    studyType: string;
    meetingType: string;
    regionId?: number;
    locationDetail?: string;
    scheduleSummary?: string;
    scheduleDays?: string;
    scheduleTime?: string;
    maxMembers: number;
    isPublic?: boolean;
    status: string;
    penaltyPolicy?: string;
    startDate?: string;
    endDate?: string;
    totalSessions?: number;
    recruitStartDate?: string;
    recruitEndDate?: string;
    extensionCount?: number;
    textbook?: string;
    goal?: string;
    difficulty?: string;
    prerequisites?: string;
    processDetail?: string;
    targetOrgType?: string;
    createdAt: string;
    updatedAt?: string;
    leader?: { id: number; nickname: string; profileImage?: string };
}

/**
 * StudyDetailPageV3
 *
 * 특징:
 * - 깔끔한 카드 기반 레이아웃
 * - 명확한 시각적 계층구조
 * - CSS 변수 활용으로 테마 일관성 유지
 * - 반응형 디자인
 */

const StudyDetailPageV3: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [studyDetail, setStudyDetail] = useState<StudyDetail | null>(null);
    const [sessions, setSessions] = useState<StudySessionItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isBookmarked, setIsBookmarked] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    // DM 관련 스토어
    const { startConversationWith, fetchConversations } = useDMStore();
    const { setActiveRightTab, showToast } = useUIStore();
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
    const [leaderReviews, setLeaderReviews] = useState<LeaderReview[]>([]);
    const [leaderAvgRating, setLeaderAvgRating] = useState(0);
    const { user } = useAuthStore();

    // 실제 API에서 스터디 상세 및 세션(커리큘럼) 조회
    useEffect(() => {
        const fetchStudyData = async () => {
            if (!id) return;
            setIsLoading(true);
            try {
                // 스터디 상세 조회
                const data = await studyApi.getStudyDetail(Number(id));
                console.log('[스터디 상세 API 응답]', data);
                setStudyDetail(data as StudyDetail);

                // 세션(커리큘럼) 목록 조회
                try {
                    console.log('[세션 조회 시작] studyId:', id);
                    const sessionData = await getStudySessions(Number(id));
                    console.log('[세션 목록 API 응답]', sessionData);
                    console.log('[세션 개수]', Array.isArray(sessionData) ? sessionData.length : 'not array');
                    setSessions(Array.isArray(sessionData) ? sessionData : []);
                } catch (sessionError: any) {
                    console.error('세션 목록 조회 실패:', sessionError);
                    console.error('세션 에러 상세:', sessionError?.response?.data || sessionError?.message);
                    setSessions([]);
                }
            } catch (error) {
                console.error('스터디 상세 조회 실패:', error);
                showToast('스터디 정보를 불러올 수 없습니다.', 'error');
            } finally {
                setIsLoading(false);
            }
        };
        fetchStudyData();
    }, [id]);

    // 메뉴 외부 클릭 감지
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setIsMenuOpen(false);
            }
        };

        if (isMenuOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isMenuOpen]);

    // 로딩 상태
    if (isLoading) {
        return (
            <UserLayoutV2>
                <div className="flex items-center justify-center min-h-[60vh]">
                    <Loader2 size={40} className="animate-spin text-[var(--color-primary)]" />
                </div>
            </UserLayoutV2>
        );
    }

    if (!studyDetail) return null;

    // studyDetail을 기존 Study 타입에 맞게 변환 (하위 호환성)
    // 날짜 기반 상태 보정 (백엔드에서 자동 업데이트 안 될 경우 대비)
    const computeStatus = (originalStatus: string, recruitStart?: string, recruitEnd?: string): string => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (originalStatus === 'PENDING' && recruitStart) {
            const startDate = new Date(recruitStart);
            startDate.setHours(0, 0, 0, 0);
            const endDate = recruitEnd ? new Date(recruitEnd) : null;
            if (endDate) endDate.setHours(0, 0, 0, 0);

            // 모집 시작일이 오늘이거나 지났고, 종료일이 안 지났으면 → 모집중
            if (startDate <= today && (!endDate || endDate >= today)) {
                return 'RECRUITING';
            }
        }
        return originalStatus;
    };

    const computedStatus = computeStatus(studyDetail.status, studyDetail.recruitStartDate, studyDetail.recruitEndDate);

    const study: Study = {
        id: studyDetail.id,
        leaderId: studyDetail.leader?.id || 0,
        name: studyDetail.name,
        description: studyDetail.description || '',
        topic: studyDetail.topic?.name || '',
        format: studyDetail.format?.name || '',
        studyType: studyDetail.studyType,
        meetingType: studyDetail.meetingType,
        status: computedStatus,
        isPublic: studyDetail.isPublic ?? true,
        maxMembers: studyDetail.maxMembers,
        currentMembers: 1,
        difficulty: studyDetail.difficulty || 'INTERMEDIATE',
        scheduleDays: studyDetail.scheduleDays || '',
        scheduleTime: studyDetail.scheduleTime,
        regionId: studyDetail.regionId,
        recruitStartDate: studyDetail.recruitStartDate,
        recruitEndDate: studyDetail.recruitEndDate,
        leader: {
            id: studyDetail.leader?.id || 0,
            nickname: studyDetail.leader?.nickname || '스터디장',
            profileImage: studyDetail.leader?.profileImage || null,
            leaderRating: 4.5,
            leaderReviewCount: 0,
        },
        isBookmarked: false,
        createdAt: studyDetail.createdAt,
    };

    const handleBookmarkToggle = () => {
        setIsBookmarked(!isBookmarked);
        showToast(isBookmarked ? '찜 목록에서 제거되었습니다.' : '찜 목록에 추가되었습니다.', 'success');
    };

    const handleReportSubmit = (reason: string) => {
        console.log(`[REPORT] Study ID: ${studyDetail.id}, Reason: ${reason}`);
        showToast('신고가 접수되었습니다.', 'success');
    };

    // 스터디장에게 문의하기
    const handleInquiry = () => {
        if (!studyDetail.leader) return;
        fetchConversations();
        startConversationWith({
            id: studyDetail.leader.id,
            nickname: studyDetail.leader.nickname,
            profileImage: studyDetail.leader.profileImage ?? null  // undefined -> null 변환
        });
        setActiveRightTab('dm');
    };

    // 평점 클릭 시 리뷰 목록 모달 열기
    const handleRatingClick = () => {
        if (!studyDetail.leader) return;
        const reviews = getReviewsByLeaderId(studyDetail.leader.id);
        const avgRating = getLeaderAverageRating(studyDetail.leader.id);
        setLeaderReviews(reviews);
        setLeaderAvgRating(avgRating > 0 ? avgRating : 4.5);
        setIsReviewModalOpen(true);
    };

    // 날짜 포맷팅 헬퍼
    const formatDate = (dateStr?: string) => {
        if (!dateStr) return '미정';
        return new Date(dateStr).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
    };

    // 링크 복사
    const handleShareLink = () => {
        navigator.clipboard.writeText(window.location.href);
        showToast('링크가 클립보드에 복사되었습니다!', 'success');
    };

    // 상태 및 난이도 설정
    const getStatusConfig = (status: string) => {
        switch (status) {
            case 'RECRUITING':
                return { text: '모집중', color: 'bg-[var(--color-success)] text-white' };
            case 'PENDING':
                return { text: '모집예정', color: 'bg-[var(--color-warning)] text-white' };
            case 'RECRUIT_CLOSED':
                return { text: '모집마감', color: 'bg-gray-500 text-white' };
            case 'IN_PROGRESS':
                return { text: '진행중', color: 'bg-[var(--color-primary)] text-white' };
            case 'COMPLETED':
                return { text: '완료', color: 'bg-[var(--color-text-tertiary)] text-white' };
            case 'CANCELLED':
                return { text: '취소됨', color: 'bg-[var(--color-error)] text-white' };
            case 'DRAFT':
                return { text: '임시저장', color: 'bg-gray-400 text-white' };
            case 'SCHEDULED':
                return { text: '예정됨', color: 'bg-blue-400 text-white' };
            default:
                return { text: status, color: 'bg-gray-400 text-white' };
        }
    };

    const getMeetingTypeText = (meetingType: string) => {
        switch (meetingType) {
            case 'ONLINE': return '온라인';
            case 'OFFLINE': return '오프라인';
            case 'HYBRID': return '온/오프라인 혼합';
            default: return meetingType;
        }
    };

    // 요일 포맷팅 (MON,WED,FRI -> 월, 수, 금)
    const formatScheduleDays = (days: string) => {
        if (!days) return '협의 후 결정';
        const dayMap: Record<string, string> = {
            'MON': '월', 'TUE': '화', 'WED': '수',
            'THU': '목', 'FRI': '금', 'SAT': '토', 'SUN': '일'
        };
        return days.split(',').map(d => dayMap[d.trim()] || d).join(', ');
    };

    // 날짜 범위 포맷팅
    const formatDateRange = (start?: string, end?: string) => {
        if (!start && !end) return '미정';
        const formatDate = (d: string) => {
            const date = new Date(d);
            return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
        };
        if (start && end) return `${formatDate(start)} ~ ${formatDate(end)}`;
        if (start) return `${formatDate(start)} ~`;
        return `~ ${formatDate(end!)}`;
    };

    const statusConfig = getStatusConfig(study.status);
    const isOwner = user?.id != null && study.leader?.id != null && Number(user.id) === Number(study.leader.id);

    // 디버그용: 콘솔에서 확인
    console.log('[isOwner 확인]', { userId: user?.id, leaderId: study.leader?.id, isOwner });

    return (
        <UserLayoutV2>
            <StudyListContainer className="px-4 md:px-6">
                <div className="max-w-8xl mx-auto py-8 animate-fadeIn">
                    {/* 상단 네비게이션 */}
                    <div className="flex justify-between items-center mb-6">
                        <div className="flex items-center gap-3">
                            <ArrowButton
                                direction="left"
                                onClick={() => navigate('/study')}
                                size="md"
                            />
                            <span className="text-sm font-semibold text-[var(--color-text-secondary)]">
                                스터디 상세
                            </span>
                        </div>
                    </div>

                    {/* 메인 콘텐츠 */}
                    <div className="grid grid-cols-1 2xl:grid-cols-4 gap-6">
                        {/* 좌측: 스터디 정보 (3열) */}
                        <div className="2xl:col-span-3">
                            {/* 통합 카드 */}
                            <div className="bg-white rounded-2xl border border-[var(--color-border)] shadow-sm overflow-hidden">
                                {/* 헤더 섹션 */}
                                <div className="p-6 md:p-8">
                                    {/* 상단: 뱃지 + 액션 버튼 */}
                                    <div className="flex justify-between items-start gap-4 mb-4">
                                        {/* 뱃지 영역 - 해시태그 스타일 */}
                                        <div className="flex flex-wrap items-center gap-2">
                                        <span className={cn(
                                            "px-3 py-1 rounded-full text-xs font-bold",
                                            statusConfig.color
                                        )}>
                                            {statusConfig.text}
                                        </span>
                                        <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[var(--color-primary-alpha-10)] text-[var(--color-primary)]">
                                            # {getMeetingTypeText(study.meetingType)}
                                        </span>
                                        <span className={cn(
                                            "px-3 py-1 rounded-full text-xs font-semibold",
                                            study.difficulty === 'ADVANCED' && "bg-[#FCE8E6] text-[#EA4335]",
                                            study.difficulty === 'INTERMEDIATE' && "bg-[#E8F0FE] text-[#4285F4]",
                                            (study.difficulty === 'BEGINNER' || study.difficulty === 'ELEMENTARY') && "bg-[#E6F4EA] text-[#34A853]"
                                        )}>
                                            # {study.difficulty === 'ADVANCED' ? '고급' : study.difficulty === 'INTERMEDIATE' ? '중급' : '입문'}
                                        </span>
                                        <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[var(--color-background-secondary)] text-[var(--color-text-secondary)]">
                                            # {study.topic}
                                        </span>
                                        {isOwner && (
                                            <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[#FEF7E0] text-[#f9ab00] border border-[#FBBC04]">
                                                # 내가 작성한 글
                                            </span>
                                        )}
                                        </div>

                                        {/* 액션 버튼 */}
                                        <div className="flex items-center gap-2 flex-shrink-0">
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={handleBookmarkToggle}
                                                className={cn(
                                                    "rounded-full",
                                                    isBookmarked
                                                        ? "text-[var(--color-error)]"
                                                        : "text-[var(--color-text-tertiary)] hover:text-[var(--color-error)]"
                                                )}
                                            >
                                                <Heart size={20} fill={isBookmarked ? 'currentColor' : 'none'} />
                                            </Button>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                onClick={handleShareLink}
                                                className="text-[var(--color-text-tertiary)] hover:text-[var(--color-primary)] rounded-full"
                                            >
                                                <Share2 size={20} />
                                            </Button>

                                            {/* 케밥 메뉴 */}
                                            <div className="relative" ref={menuRef}>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => setIsMenuOpen(!isMenuOpen)}
                                                    className="text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)] rounded-full"
                                                >
                                                    <MoreVertical size={20} />
                                                </Button>

                                                {/* 드롭다운 메뉴 */}
                                                {isMenuOpen && (
                                                    <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl border border-[var(--color-border)] shadow-lg z-50 overflow-hidden">
                                                        {isOwner ? (
                                                            <button
                                                                onClick={() => {
                                                                    navigate(`/study/create/planned?studyId=${study.id}`);
                                                                    setIsMenuOpen(false);
                                                                }}
                                                                className="w-full px-4 py-3 text-left text-sm font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-primary-alpha-10)] hover:text-[var(--color-primary)] transition-colors flex items-center gap-2"
                                                            >
                                                                <Pencil size={16} />
                                                                <span>수정하기</span>
                                                            </button>
                                                        ) : (
                                                            <button
                                                                onClick={() => {
                                                                    setIsReportModalOpen(true);
                                                                    setIsMenuOpen(false);
                                                                }}
                                                                className="w-full px-4 py-3 text-left text-sm font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-error-light)] hover:text-[var(--color-error)] transition-colors flex items-center gap-2"
                                                            >
                                                                <AlertTriangle size={16} />
                                                                <span>신고하기</span>
                                                            </button>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>

                                    {/* 타이틀 */}
                                    <h1 className="text-2xl md:text-3xl font-bold text-[var(--color-text-primary)] mb-4 leading-tight">
                                        {study.name}
                                    </h1>

                                    {/* 설명 */}
                                    <p className="text-[var(--color-text-secondary)] leading-relaxed">
                                        {study.description}
                                    </p>
                                </div>

                                {/* 구분선 */}
                                <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                                {/* 모집 정보 섹션 */}
                                <div className="p-6 md:p-8">
                                    <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-8">
                                        <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                            <Target size={18} className="text-[var(--color-primary)]" />
                                        </div>
                                        모집 정보
                                    </h2>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                                        <InfoRow
                                            icon={<Users size={18} />}
                                            label="모집 인원"
                                            value={`${study.currentMembers} / ${study.maxMembers}명`}
                                            highlight={study.currentMembers >= study.maxMembers ? '마감' : undefined}
                                        />
                                        <InfoRow
                                            icon={
                                                study.meetingType === 'ONLINE' ? <Monitor size={18} /> :
                                                study.meetingType === 'OFFLINE' ? <Handshake size={18} /> :
                                                <Layers size={18} />
                                            }
                                            label="진행 방식"
                                            value={getMeetingTypeText(study.meetingType)}
                                        />
                                        <InfoRow
                                            icon={<MapPin size={18} />}
                                            label="활동 지역"
                                            value={
                                                study.meetingType === 'ONLINE'
                                                    ? '전국'
                                                    : study.region?.name || getRegionById(study.regionId!)?.name || '미지정'
                                            }
                                        />
                                        <InfoRow
                                            icon={<Clock size={18} />}
                                            label="모임 시간"
                                            value={study.scheduleTime ? study.scheduleTime.substring(0, 5) : '협의 후 결정'}
                                        />
                                        <InfoRow
                                            icon={<CalendarDays size={18} />}
                                            label="모임 요일"
                                            value={formatScheduleDays(study.scheduleDays)}
                                        />
                                        <InfoRow
                                            icon={<Bookmark size={18} />}
                                            label="모집 기간"
                                            value={formatDateRange(study.recruitStartDate, study.recruitEndDate)}
                                        />
                                    </div>
                                </div>

                                {/* 구분선 */}
                                <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                                {/* 스터디 상세 정보 섹션 */}
                                <div className="p-6 md:p-8">
                                    <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-8">
                                        <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                            <Info size={18} className="text-[var(--color-primary)]" />
                                        </div>
                                        스터디 상세
                                    </h2>

                                    <div className="space-y-6">
                                        {/* 한줄 소개 */}
                                        {studyDetail.intro && (
                                            <div className="flex items-start gap-3 p-4 bg-[var(--color-primary-alpha-5)] rounded-xl">
                                                <span className="text-xl">💬</span>
                                                <div>
                                                    <p className="text-xs text-[var(--color-text-secondary)] mb-1">한줄 소개</p>
                                                    <p className="text-[var(--color-text-primary)] font-medium">
                                                        {studyDetail.intro}
                                                    </p>
                                                </div>
                                            </div>
                                        )}

                                        {/* 일정 정보 */}
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                                            <InfoRow
                                                icon={<Calendar size={18} />}
                                                label="스터디 기간"
                                                value={`${formatDate(studyDetail.startDate)} ~ ${formatDate(studyDetail.endDate)}`}
                                            />
                                            <InfoRow
                                                icon={<Calendar size={18} />}
                                                label="모집 기간"
                                                value={`${formatDate(studyDetail.recruitStartDate)} ~ ${formatDate(studyDetail.recruitEndDate)}`}
                                            />
                                            {studyDetail.scheduleDays && (
                                                <InfoRow
                                                    icon={<Clock size={18} />}
                                                    label="모임 요일"
                                                    value={studyDetail.scheduleDays}
                                                />
                                            )}
                                            {studyDetail.totalSessions && (
                                                <InfoRow
                                                    icon={<FileText size={18} />}
                                                    label="총 회차"
                                                    value={`${studyDetail.totalSessions}회`}
                                                />
                                            )}
                                        </div>

                                        {/* 목표 */}
                                        {studyDetail.goal && (
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <GraduationCap size={16} className="text-[var(--color-primary)]" />
                                                    스터디 목표
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed bg-[var(--color-background)] p-4 rounded-xl">
                                                    {studyDetail.goal}
                                                </p>
                                            </div>
                                        )}

                                        {/* 교재/자료 */}
                                        {studyDetail.textbook && (
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <BookOpen size={16} className="text-[var(--color-primary)]" />
                                                    교재 및 자료
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed bg-[var(--color-background)] p-4 rounded-xl">
                                                    {studyDetail.textbook}
                                                </p>
                                            </div>
                                        )}

                                        {/* 사전 지식 */}
                                        {studyDetail.prerequisites && (
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <Award size={16} className="text-[var(--color-primary)]" />
                                                    필요한 사전 지식
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed bg-[var(--color-background)] p-4 rounded-xl">
                                                    {studyDetail.prerequisites}
                                                </p>
                                            </div>
                                        )}

                                        {/* 진행 방식 상세 */}
                                        {studyDetail.processDetail && (
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <FileText size={16} className="text-[var(--color-primary)]" />
                                                    진행 방식
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed bg-[var(--color-background)] p-4 rounded-xl whitespace-pre-wrap">
                                                    {studyDetail.processDetail}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* 구분선 */}
                                <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

                                {/* 커리큘럼(세션) 섹션 */}
                                <div className="p-6 md:p-8">
                                    <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-8">
                                        <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                            <BookOpen size={18} className="text-[var(--color-primary)]" />
                                        </div>
                                        스터디 커리큘럼
                                        {sessions.length > 0 && (
                                            <span className="text-sm font-normal text-[var(--color-text-tertiary)]">
                                                ({sessions.length}회차)
                                            </span>
                                        )}
                                    </h2>

                                    {sessions.length > 0 ? (
                                        <div className="space-y-3">
                                            {sessions
                                                .sort((a, b) => a.sessionNumber - b.sessionNumber)
                                                .map((session) => (
                                                <div
                                                    key={session.id}
                                                    className="flex items-start gap-4 p-4 bg-[var(--color-background)] rounded-xl border border-[var(--color-border-lighter)] hover:border-[var(--color-primary-alpha-20)] transition-colors"
                                                >
                                                    <div className="flex-shrink-0 w-12 h-12 bg-[var(--color-primary-alpha-10)] rounded-xl flex flex-col items-center justify-center">
                                                        <span className="text-[10px] font-bold text-[var(--color-text-tertiary)] uppercase">
                                                            {session.sessionNumber}회차
                                                        </span>
                                                    </div>
                                                    <div className="flex-1 pt-1">
                                                        {session.title && (
                                                            <p className="text-[var(--color-text-primary)] font-bold mb-1">
                                                                {session.title}
                                                            </p>
                                                        )}
                                                        <p className="text-[var(--color-text-secondary)] text-sm leading-relaxed">
                                                            {session.description || '내용 없음'}
                                                        </p>
                                                        {session.scheduledAt && (
                                                            <p className="text-xs text-[var(--color-text-tertiary)] mt-2">
                                                                {new Date(session.scheduledAt).toLocaleDateString('ko-KR', {
                                                                    month: 'long', day: 'numeric', weekday: 'short'
                                                                })}
                                                            </p>
                                                        )}
                                                    </div>
                                                    <div className="flex items-center gap-2">
                                                        {session.status === 'COMPLETED' && (
                                                            <span className="px-2 py-1 bg-[var(--color-success-light)] text-[var(--color-success)] text-xs font-bold rounded">
                                                                완료
                                                            </span>
                                                        )}
                                                        {session.status === 'IN_PROGRESS' && (
                                                            <span className="px-2 py-1 bg-[var(--color-primary-alpha-10)] text-[var(--color-primary)] text-xs font-bold rounded">
                                                                진행중
                                                            </span>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    ) : (
                                        <div className="text-center py-8 text-[var(--color-text-secondary)]">
                                            <Award size={40} className="mx-auto mb-3 text-[var(--color-text-muted)]" />
                                            <p className="font-medium mb-1">아직 커리큘럼이 등록되지 않았습니다.</p>
                                            <p className="text-sm text-[var(--color-text-tertiary)]">
                                                스터디 진행 후 커리큘럼이 공개됩니다.
                                            </p>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* 댓글 섹션 */}
                            <StudyCommentSection
                                studyId={study.id}
                                studyLeaderId={study.leader.id}
                            />
                        </div>

                        {/* 우측: 리더 정보 (1열) */}
                        <StudyLeaderCard
                            leader={study.leader}
                            studyId={study.id}
                            studyStatus={study.status}
                            currentMembers={study.currentMembers}
                            maxMembers={study.maxMembers}
                            recruitEndDate={study.recruitEndDate}
                            isOwner={isOwner}
                            onInquiry={handleInquiry}
                            onRatingClick={handleRatingClick}
                            onApply={() => setIsApplyModalOpen(true)}
                        />
                    </div>
                </div>
            </StudyListContainer>

            {/* 모달들 */}
            {study && (
                <StudyApplyModalV2
                    study={study}
                    isOpen={isApplyModalOpen}
                    onClose={() => setIsApplyModalOpen(false)}
                />
            )}

            <StudyReportModal
                isOpen={isReportModalOpen}
                onClose={() => setIsReportModalOpen(false)}
                onSubmit={handleReportSubmit}
                targetTitle={study.name}
            />

            <LeaderReviewModal
                isOpen={isReviewModalOpen}
                onClose={() => setIsReviewModalOpen(false)}
                leaderNickname={study.leader.nickname}
                reviews={leaderReviews}
                averageRating={leaderAvgRating}
            />
        </UserLayoutV2>
    );
};

// 정보 행 컴포넌트
interface InfoRowProps {
    icon: React.ReactNode;
    label: string;
    value: string;
    highlight?: string;
}

const InfoRow: React.FC<InfoRowProps> = ({ icon, label, value, highlight }) => (
    <div className="flex items-start gap-3">
        <div className="p-2 bg-[var(--color-background-secondary)] rounded-lg text-[var(--color-primary)]">
            {icon}
        </div>
        <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wide mb-1">
                {label}
            </p>
            <p className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-2">
                <span className="truncate">{value}</span>
                {highlight && (
                    <span className="px-2 py-0.5 bg-[var(--color-error-light)] text-[var(--color-error)] text-xs font-bold rounded">
                        {highlight}
                    </span>
                )}
            </p>
        </div>
    </div>
);

export default StudyDetailPageV3;
