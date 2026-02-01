import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Heart, Users, Clock, MapPin,
    Target, Award, AlertTriangle, Share2,
    BookOpen, Monitor, Handshake, Layers, MoreVertical,
    Calendar, CalendarDays, Bookmark, FileText, GraduationCap, Info, Loader2, Pencil, Quote, Trash2
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { studyService, Study } from '../services/studyService';
import { studyApi, getStudySessions, StudySessionItem, deleteStudy, getLeaderReviews, getLeaderInfo, LeaderReviewResponse, LeaderInfoResponse, getProvinces, getDistricts } from '@/api/endpoints/studyApi';
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
    currentMembers?: number;  // 현재 참여 인원
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
    const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [leaderReviews, setLeaderReviews] = useState<LeaderReviewResponse[]>([]);
    const [leaderAvgRating, setLeaderAvgRating] = useState<number | null>(null);
    const [leaderReviewCount, setLeaderReviewCount] = useState(0);
    const [leaderInfo, setLeaderInfo] = useState<LeaderInfoResponse | null>(null);
    const [regionName, setRegionName] = useState<string | null>(null);
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

                // 스터디장 정보 조회 (평점, 리뷰 수 포함)
                try {
                    const leaderData = await getLeaderInfo(Number(id));
                    console.log('[스터디장 정보 API 응답]', leaderData);
                    setLeaderInfo(leaderData);
                    setLeaderAvgRating(leaderData.leaderRating);
                    setLeaderReviewCount(leaderData.leaderReviewCount || 0);
                } catch (leaderError) {
                    console.error('스터디장 정보 조회 실패:', leaderError);
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

    // regionId가 있으면 지역 이름 조회
    useEffect(() => {
        const fetchRegionName = async () => {
            if (!studyDetail?.regionId) {
                setRegionName(null);
                return;
            }

            try {
                // 모든 시/도를 가져와서 각 시/도의 구/군에서 regionId 찾기
                const provinces = await getProvinces();
                for (const province of provinces) {
                    const districts = await getDistricts(province.id);
                    const foundDistrict = districts.find(d => d.id === studyDetail.regionId);
                    if (foundDistrict) {
                        // 시/도 + 구/군 형태로 표시
                        setRegionName(`${province.name} ${foundDistrict.name}`);
                        return;
                    }
                }
                // 못 찾으면 미지정
                setRegionName(null);
            } catch (error) {
                console.error('지역 정보 조회 실패:', error);
                setRegionName(null);
            }
        };

        fetchRegionName();
    }, [studyDetail?.regionId]);

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
        currentMembers: studyDetail.currentMembers || 1,  // API에서 받아온 값 사용 (0이면 최소 1명)
        difficulty: studyDetail.difficulty || 'INTERMEDIATE',
        scheduleDays: studyDetail.scheduleDays || '',
        scheduleTime: studyDetail.scheduleTime,
        regionId: studyDetail.regionId,
        recruitStartDate: studyDetail.recruitStartDate,
        recruitEndDate: studyDetail.recruitEndDate,
        leader: {
            id: studyDetail.leader?.id || 0,
            nickname: leaderInfo?.nickname || studyDetail.leader?.nickname || '스터디장',
            profileImage: studyDetail.leader?.profileImage || null,
            leaderRating: leaderInfo?.leaderRating ?? null,
            leaderReviewCount: leaderInfo?.leaderReviewCount || 0,
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
    const handleRatingClick = async () => {
        if (!studyDetail.id) return;

        try {
            // 리뷰 목록 조회
            const reviewData = await getLeaderReviews(studyDetail.id);
            setLeaderReviews(reviewData.content || []);

            // 평균 평점 계산
            if (reviewData.content && reviewData.content.length > 0) {
                const sum = reviewData.content.reduce((acc, review) => acc + review.rating, 0);
                setLeaderAvgRating(sum / reviewData.content.length);
            } else {
                setLeaderAvgRating(0);
            }
            setLeaderReviewCount(reviewData.totalElements || 0);
            setIsReviewModalOpen(true);
        } catch (error) {
            console.error('리뷰 조회 실패:', error);
            showToast('리뷰를 불러오는데 실패했습니다.', 'error');
        }
    };

    // 스터디 삭제 핸들러
    const handleDeleteStudy = async () => {
        if (!studyDetail.id) return;

        setIsDeleting(true);
        try {
            await deleteStudy(studyDetail.id);
            showToast('스터디가 삭제되었습니다.', 'success');
            navigate('/study');
        } catch (error) {
            console.error('스터디 삭제 실패:', error);
            showToast('스터디 삭제에 실패했습니다.', 'error');
        } finally {
            setIsDeleting(false);
            setIsDeleteConfirmOpen(false);
        }
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

    // 요일 포맷팅 (MON,WED,FRI -> 월, 수, 금) - 요일 순서대로 정렬
    const formatScheduleDays = (days: string) => {
        // 번개 스터디이고 scheduleDays가 없으면 startDate의 요일 표시
        if (!days && studyDetail?.studyType === 'LIGHTNING' && studyDetail?.startDate) {
            const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
            const startDate = new Date(studyDetail.startDate);
            return dayNames[startDate.getDay()];
        }
        if (!days) return '협의 후 결정';
        // 대소문자/한글 모두 지원
        const dayOrder: Record<string, number> = {
            'MON': 0, 'mon': 0, '월': 0,
            'TUE': 1, 'tue': 1, '화': 1,
            'WED': 2, 'wed': 2, '수': 2,
            'THU': 3, 'thu': 3, '목': 3,
            'FRI': 4, 'fri': 4, '금': 4,
            'SAT': 5, 'sat': 5, '토': 5,
            'SUN': 6, 'sun': 6, '일': 6,
        };
        const dayMap: Record<string, string> = {
            'MON': '월', 'mon': '월', '월': '월',
            'TUE': '화', 'tue': '화', '화': '화',
            'WED': '수', 'wed': '수', '수': '수',
            'THU': '목', 'thu': '목', '목': '목',
            'FRI': '금', 'fri': '금', '금': '금',
            'SAT': '토', 'sat': '토', '토': '토',
            'SUN': '일', 'sun': '일', '일': '일',
        };
        const sortedDays = days.split(',')
            .map(d => d.trim())
            .sort((a, b) => (dayOrder[a] ?? 99) - (dayOrder[b] ?? 99));
        return sortedDays.map(d => dayMap[d] || d).join(', ');
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
                                                            <>
                                                                <button
                                                                    onClick={() => {
                                                                        // 스터디 타입에 따라 다른 수정 페이지로 이동 (from=detail로 돌아올 페이지 지정)
                                                                        if (study.studyType === 'LIGHTNING') {
                                                                            navigate(`/study/edit/lightning/${study.id}?from=detail`);
                                                                        } else {
                                                                            navigate(`/study/create/planned?studyId=${study.id}&from=detail`);
                                                                        }
                                                                        setIsMenuOpen(false);
                                                                    }}
                                                                    className="w-full px-4 py-3 text-left text-sm font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-primary-alpha-10)] hover:text-[var(--color-primary)] transition-colors flex items-center gap-2"
                                                                >
                                                                    <Pencil size={16} />
                                                                    <span>수정하기</span>
                                                                </button>
                                                                <button
                                                                    onClick={() => {
                                                                        setIsDeleteConfirmOpen(true);
                                                                        setIsMenuOpen(false);
                                                                    }}
                                                                    disabled={study.currentMembers > 1}
                                                                    className={cn(
                                                                        "w-full px-4 py-3 text-left text-sm font-medium transition-colors flex items-center gap-2",
                                                                        study.currentMembers > 1
                                                                            ? "text-[var(--color-text-muted)] cursor-not-allowed"
                                                                            : "text-[var(--color-error)] hover:bg-[var(--color-error-light)]"
                                                                    )}
                                                                >
                                                                    <Trash2 size={16} />
                                                                    <span>삭제하기</span>
                                                                    {study.currentMembers > 1 && (
                                                                        <span className="text-[10px] ml-auto">(멤버 있음)</span>
                                                                    )}
                                                                </button>
                                                            </>
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

                                    <div className="space-y-6 pl-2.5">
                                        {/* 모집 인원 & 진행 방식 */}
                                        <div className="grid grid-cols-2 gap-6">
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <Users size={16} className="text-[var(--color-primary)]" />
                                                    모집 인원
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {study.currentMembers} / {study.maxMembers}명
                                                    {study.currentMembers >= study.maxMembers && (
                                                        <span className="ml-2 text-xs font-bold text-[var(--color-error)]">마감</span>
                                                    )}
                                                </p>
                                            </div>
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    {study.meetingType === 'ONLINE' ? <Monitor size={16} className="text-[var(--color-primary)]" /> :
                                                     study.meetingType === 'OFFLINE' ? <Handshake size={16} className="text-[var(--color-primary)]" /> :
                                                     <Layers size={16} className="text-[var(--color-primary)]" />}
                                                    진행 방식
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {getMeetingTypeText(study.meetingType)}
                                                </p>
                                            </div>
                                        </div>

                                        {/* 활동 지역 & 모임 시간 */}
                                        <div className="grid grid-cols-2 gap-6">
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <MapPin size={16} className="text-[var(--color-primary)]" />
                                                    활동 지역
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {study.meetingType === 'ONLINE'
                                                        ? '전국'
                                                        : regionName || '미지정'}
                                                </p>
                                            </div>
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <Clock size={16} className="text-[var(--color-primary)]" />
                                                    모임 시간
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {study.scheduleTime ? study.scheduleTime.substring(0, 5) : '협의 후 결정'}
                                                </p>
                                            </div>
                                        </div>

                                        {/* 모임 요일 & 모집 기간 */}
                                        <div className="grid grid-cols-2 gap-6">
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <CalendarDays size={16} className="text-[var(--color-primary)]" />
                                                    모임 요일
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {formatScheduleDays(study.scheduleDays)}
                                                </p>
                                            </div>
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <Bookmark size={16} className="text-[var(--color-primary)]" />
                                                    모집 기간
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {formatDateRange(study.recruitStartDate, study.recruitEndDate)}
                                                </p>
                                            </div>
                                        </div>
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

                                    <div className="space-y-6 pl-2.5">
                                        {/* 한줄 소개 */}
                                        {studyDetail.intro && (
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <Quote size={16} className="text-[var(--color-primary)]" />
                                                    한줄 소개
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {studyDetail.intro}
                                                </p>
                                            </div>
                                        )}

                                        {/* 일정 정보 */}
                                        <div className="grid grid-cols-2 gap-6">
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <Calendar size={16} className="text-[var(--color-primary)]" />
                                                    스터디 기간
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                    {formatDate(studyDetail.startDate)} ~ {formatDate(studyDetail.endDate)}
                                                </p>
                                            </div>
                                            {studyDetail.totalSessions && (
                                                <div>
                                                    <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                        <FileText size={16} className="text-[var(--color-primary)]" />
                                                        총 회차
                                                    </h3>
                                                    <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
                                                        {studyDetail.totalSessions}회
                                                    </p>
                                                </div>
                                            )}
                                        </div>

                                        {/* 목표 */}
                                        {studyDetail.goal && (
                                            <div>
                                                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--color-text-secondary)] mb-3">
                                                    <GraduationCap size={16} className="text-[var(--color-primary)]" />
                                                    스터디 목표
                                                </h3>
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
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
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
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
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)]">
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
                                                <p className="text-[var(--color-text-primary)] leading-relaxed pb-4 border-b border-[var(--color-border)] whitespace-pre-wrap">
                                                    {studyDetail.processDetail}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* 커리큘럼(세션) 섹션 - 번개 스터디는 숨김 */}
                                {studyDetail.studyType !== 'LIGHTNING' && (
                                <>
                                {/* 구분선 */}
                                <div className="mx-6 md:mx-8 border-t-2 border-gray-200" />

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
                                        <div className="relative pl-8">
                                            {/* 타임라인 세로선 */}
                                            <div className="absolute left-[11px] top-2 bottom-2 w-0.5 bg-[var(--color-border)]" />

                                            <div className="space-y-6">
                                                {sessions
                                                    .sort((a, b) => a.sessionNumber - b.sessionNumber)
                                                    .map((session, index) => (
                                                    <div
                                                        key={session.id}
                                                        className="relative flex items-start gap-4"
                                                    >
                                                        {/* 타임라인 dot */}
                                                        <div className={cn(
                                                            "absolute -left-8 top-1 w-6 h-6 rounded-full flex items-center justify-center border-2 z-10",
                                                            session.status === 'COMPLETED'
                                                                ? "bg-[var(--color-success)] border-[var(--color-success)]"
                                                                : session.status === 'IN_PROGRESS'
                                                                ? "bg-[var(--color-primary)] border-[var(--color-primary)] animate-pulse"
                                                                : "bg-white border-[var(--color-border)]"
                                                        )}>
                                                            {session.status === 'COMPLETED' ? (
                                                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3">
                                                                    <polyline points="20 6 9 17 4 12" />
                                                                </svg>
                                                            ) : (
                                                                <span className={cn(
                                                                    "text-[10px] font-bold",
                                                                    session.status === 'IN_PROGRESS'
                                                                        ? "text-white"
                                                                        : "text-[#4285F4]"
                                                                )}>
                                                                    {session.sessionNumber}
                                                                </span>
                                                            )}
                                                        </div>

                                                        {/* 내용 */}
                                                        <div className={cn(
                                                            "flex-1 pb-2",
                                                            index !== sessions.length - 1 && "border-b border-[var(--color-border-lighter)]"
                                                        )}>
                                                            <div className="flex items-center justify-between gap-2 mb-1">
                                                                <div className="flex items-center gap-2">
                                                                    {session.scheduledAt && (
                                                                        <span className="text-xs text-[var(--color-text-tertiary)]">
                                                                            {new Date(session.scheduledAt).toLocaleDateString('ko-KR', {
                                                                                month: 'short', day: 'numeric'
                                                                            })}
                                                                        </span>
                                                                    )}
                                                                </div>
                                                                {session.status === 'COMPLETED' && (
                                                                    <span className="text-xs font-bold text-[var(--color-success)]">완료</span>
                                                                )}
                                                                {session.status === 'IN_PROGRESS' && (
                                                                    <span className="text-xs font-bold text-[var(--color-primary)]">진행중</span>
                                                                )}
                                                            </div>
                                                            {session.title && (
                                                                <p className="text-[var(--color-text-primary)] font-semibold mb-1">
                                                                    {session.title}
                                                                </p>
                                                            )}
                                                            <p className="text-[var(--color-text-secondary)] text-sm leading-relaxed">
                                                                {session.description || '내용 없음'}
                                                            </p>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
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
                                </>
                                )}

                            </div>

                            {/* 댓글 섹션 */}
                            <div className="mt-6">
                                <StudyCommentSection
                                    studyId={study.id}
                                    studyLeaderId={study.leader.id}
                                />
                            </div>
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

            {/* 스터디 삭제 확인 모달 */}
            {isDeleteConfirmOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-2xl p-6 w-full max-w-md mx-4 shadow-xl">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-12 h-12 rounded-full bg-[var(--color-error-light)] flex items-center justify-center">
                                <Trash2 size={24} className="text-[var(--color-error)]" />
                            </div>
                            <div>
                                <h3 className="text-lg font-bold text-[var(--color-text-primary)]">스터디 삭제</h3>
                                <p className="text-sm text-[var(--color-text-tertiary)]">이 작업은 되돌릴 수 없습니다</p>
                            </div>
                        </div>
                        <p className="text-[var(--color-text-secondary)] mb-6">
                            <strong>"{study.name}"</strong> 스터디를 정말 삭제하시겠습니까?<br />
                            모든 커리큘럼과 댓글이 함께 삭제됩니다.
                        </p>
                        <div className="flex gap-3">
                            <Button
                                variant="secondary"
                                fullWidth
                                onClick={() => setIsDeleteConfirmOpen(false)}
                                disabled={isDeleting}
                            >
                                취소
                            </Button>
                            <Button
                                variant="primary"
                                fullWidth
                                onClick={handleDeleteStudy}
                                disabled={isDeleting}
                                className="bg-[var(--color-error)] hover:bg-[var(--color-error-dark)]"
                            >
                                {isDeleting ? (
                                    <span className="flex items-center gap-2">
                                        <Loader2 size={16} className="animate-spin" />
                                        삭제 중...
                                    </span>
                                ) : '삭제'}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </UserLayoutV2>
    );
};

export default StudyDetailPageV3;
