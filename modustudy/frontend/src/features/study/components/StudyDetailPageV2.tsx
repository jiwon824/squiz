import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Heart, Star, Users, Clock, MapPin,
    Target, Award, AlertTriangle, Share2, Shield, Send,
    BookOpen, ChevronRight
} from 'lucide-react';
import { DifficultyBadge } from './DifficultyBadge';
import { useAuthStore } from '@/store/authStore';
import { studyApi } from '@/api/endpoints/studyApi';
import { Study } from '../services/studyService';
import StudyApplyModalV2 from './StudyApplyModalV2';
import { StudyReportModal } from './StudyReportModal';
import LeaderReviewModal from './LeaderReviewModal';
import StudyListContainer from './StudyListContainer';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Button, ArrowButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useUIStore } from '@/store/uiStore';
import { getReviewsByLeaderId, getLeaderAverageRating, LeaderReview } from '../mockData';
import { getProfileImageUrl } from '@/shared/utils/profileImage';

/**
 * StudyDetailPageV2 - Google Material Design 스타일 스터디 상세 페이지
 *
 * 특징:
 * - 깔끔한 카드 기반 레이아웃
 * - 명확한 시각적 계층구조
 * - CSS 변수 활용으로 테마 일관성 유지
 * - 반응형 디자인
 */

const StudyDetailPageV2: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [study, setStudy] = useState<Study | null>(null);
    const [isBookmarked, setIsBookmarked] = useState(false);

    // DM 관련 스토어
    const { startConversationWith, fetchConversations } = useDMStore();
    const { setActiveRightTab, showToast } = useUIStore();
    const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
    const [leaderReviews, setLeaderReviews] = useState<LeaderReview[]>([]);
    const [leaderAvgRating, setLeaderAvgRating] = useState(0);
    const { user } = useAuthStore();

    useEffect(() => {
        const fetchStudyDetail = async () => {
            if (!id) return;

            try {
                const data = await studyApi.getStudyDetail(Number(id));
                if (data) {
                    setStudy(data as Study);
                    setIsBookmarked(data.isBookmarked || false);
                }
            } catch (error) {
                console.error('스터디 상세 조회 실패:', error);
                showToast('스터디 정보를 불러오는데 실패했습니다.', 'error');
                navigate('/study');
            }
        };

        fetchStudyDetail();
    }, [id, navigate, showToast]);

    if (!study) return null;

    const handleBookmarkToggle = async () => {
        try {
            const response = await studyApi.toggleBookmark(study.id);
            const newBookmarkState = response?.isBookmarked !== undefined ? response.isBookmarked : !isBookmarked;
            setIsBookmarked(newBookmarkState);
            showToast(
                newBookmarkState ? '찜 목록에 추가되었습니다.' : '찜 목록에서 제거되었습니다.',
                'success'
            );
        } catch (error) {
            console.error('북마크 토글 실패:', error);
            showToast('북마크 처리에 실패했습니다.', 'error');
        }
    };

    const handleReportSubmit = (reason: string) => {
        showToast('신고가 접수되었습니다.', 'success');
    };

    // 스터디장에게 문의하기
    const handleInquiry = () => {
        fetchConversations();
        startConversationWith({
            id: study.leader.id,
            nickname: study.leader.nickname,
            profileImage: study.leader.profileImage
        });
        setActiveRightTab('dm');
    };

    // 평점 클릭 시 리뷰 목록 모달 열기
    const handleRatingClick = () => {
        const reviews = getReviewsByLeaderId(study.leader.id);
        const avgRating = getLeaderAverageRating(study.leader.id);
        setLeaderReviews(reviews);
        setLeaderAvgRating(avgRating > 0 ? avgRating : study.leader.leaderRating);
        setIsReviewModalOpen(true);
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
            case 'IN_PROGRESS':
                return { text: '진행중', color: 'bg-[var(--color-primary)] text-white' };
            case 'COMPLETED':
                return { text: '완료됨', color: 'bg-[var(--color-text-tertiary)] text-white' };
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

    const statusConfig = getStatusConfig(study.status);
    const isOwner = String(user?.id) === String(study.leader.id);

    return (
        <UserLayoutV2>
            <StudyListContainer className="px-4 md:px-6">
                <div className="max-w-5xl mx-auto py-8 animate-fadeIn">
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
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setIsReportModalOpen(true)}
                            className="text-[var(--color-text-tertiary)] hover:text-[var(--color-error)] hover:bg-[var(--color-error-light)]"
                        >
                            <AlertTriangle size={16} className="mr-1.5" />
                            <span className="text-xs font-semibold">신고</span>
                        </Button>
                    </div>

                    {/* 메인 콘텐츠 */}
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* 좌측: 스터디 정보 (2열) */}
                        <div className="lg:col-span-2 space-y-6">
                            {/* 헤더 카드 */}
                            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 md:p-8 shadow-sm">
                                {/* 뱃지 영역 */}
                                <div className="flex flex-wrap items-center gap-2 mb-4">
                                    <span className={cn(
                                        "px-3 py-1 rounded-full text-xs font-bold",
                                        statusConfig.color
                                    )}>
                                        {statusConfig.text}
                                    </span>
                                    <DifficultyBadge difficulty={study.difficulty} size="md" />
                                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-[var(--color-background-secondary)] text-[var(--color-text-secondary)]">
                                        {study.topic}
                                    </span>
                                </div>

                                {/* 타이틀 */}
                                <h1 className="text-2xl md:text-3xl font-bold text-[var(--color-text-primary)] mb-3 leading-tight">
                                    {study.name}
                                </h1>

                                {/* 설명 */}
                                <p className="text-[var(--color-text-secondary)] leading-relaxed mb-6">
                                    {study.description}
                                </p>

                                {/* 액션 버튼 */}
                                <div className="flex items-center gap-2">
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
                                </div>
                            </div>

                            {/* 모집 정보 카드 */}
                            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 md:p-8 shadow-sm">
                                <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-6">
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
                                        icon={<MapPin size={18} />}
                                        label="진행 방식"
                                        value={`${getMeetingTypeText(study.meetingType)}${study.region ? ` · ${study.region.name}` : ''}`}
                                    />
                                    <InfoRow
                                        icon={<Clock size={18} />}
                                        label="모임 시간"
                                        value={study.scheduleTime ? study.scheduleTime.substring(0, 5) : '협의 후 결정'}
                                    />
                                </div>
                            </div>

                            {/* 커리큘럼 카드 */}
                            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 md:p-8 shadow-sm">
                                <h2 className="flex items-center gap-2 text-lg font-bold text-[var(--color-text-primary)] mb-6">
                                    <div className="p-2 bg-[var(--color-primary-alpha-10)] rounded-xl">
                                        <BookOpen size={18} className="text-[var(--color-primary)]" />
                                    </div>
                                    스터디 커리큘럼
                                </h2>

                                {study.curriculum && study.curriculum.length > 0 ? (
                                    <div className="space-y-3">
                                        {study.curriculum.map((item, index) => (
                                            <div
                                                key={index}
                                                className="flex items-start gap-4 p-4 bg-[var(--color-background)] rounded-xl border border-[var(--color-border-lighter)] hover:border-[var(--color-primary-alpha-20)] transition-colors"
                                            >
                                                <div className="flex-shrink-0 w-12 h-12 bg-[var(--color-primary-alpha-10)] rounded-xl flex flex-col items-center justify-center">
                                                    <span className="text-[10px] font-bold text-[var(--color-text-tertiary)] uppercase">Week</span>
                                                    <span className="text-lg font-bold text-[var(--color-primary)]">{item.week}</span>
                                                </div>
                                                <div className="flex-1 pt-1">
                                                    <p className="text-[var(--color-text-primary)] font-medium leading-relaxed">
                                                        {item.description}
                                                    </p>
                                                </div>
                                                <ChevronRight size={18} className="text-[var(--color-text-muted)] flex-shrink-0 mt-3" />
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

                        {/* 우측: 리더 정보 (1열) */}
                        <div className="lg:col-span-1">
                            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 shadow-sm sticky top-6">
                                {/* 리더 프로필 */}
                                <div className="text-center mb-6">
                                    <div className="relative inline-block mb-4">
                                        <img
                                            src={getProfileImageUrl(study.leader.profileImage)}
                                            alt={study.leader.nickname}
                                            className="w-20 h-20 rounded-2xl object-cover border-2 border-white shadow-lg"
                                        />
                                        {study.status === 'RECRUITING' && (
                                            <span className="absolute -bottom-1 -right-1 w-5 h-5 bg-[var(--color-success)] border-2 border-white rounded-full" />
                                        )}
                                    </div>

                                    <h3 className="text-xl font-bold text-[var(--color-text-primary)] mb-2">
                                        {study.leader.nickname}
                                    </h3>

                                    {/* 평점 */}
                                    <button
                                        onClick={handleRatingClick}
                                        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-[var(--color-background-secondary)] rounded-full hover:bg-[var(--color-primary-alpha-10)] transition-colors group"
                                    >
                                        <Star size={14} className="text-yellow-400 fill-current" />
                                        <span className="text-sm font-bold text-[var(--color-text-primary)]">
                                            {study.leader.leaderRating.toFixed(1)}
                                        </span>
                                        <span className="text-xs text-[var(--color-text-tertiary)] group-hover:text-[var(--color-primary)]">
                                            ({study.leader.leaderReviewCount}개 리뷰)
                                        </span>
                                    </button>
                                </div>

                                {/* 인증 뱃지 */}
                                <div className="flex items-center justify-center gap-1.5 mb-6 text-xs text-[var(--color-text-tertiary)]">
                                    <Shield size={12} className="text-[var(--color-primary)]" />
                                    <span>인증된 스터디 리더</span>
                                </div>

                                {/* 액션 버튼 */}
                                <div className="space-y-3">
                                    <Button
                                        variant="google-outline"
                                        fullWidth
                                        onClick={handleInquiry}
                                        leftIcon={<Send size={16} />}
                                        className="h-12 rounded-xl font-semibold"
                                    >
                                        스터디장에게 문의
                                    </Button>

                                    {isOwner ? (
                                        <Button
                                            variant="primary"
                                            fullWidth
                                            size="lg"
                                            onClick={() => navigate(`/study/manage/${study.id}`)}
                                            className="h-12 rounded-xl font-bold shadow-lg shadow-[var(--color-primary-alpha-30)]"
                                        >
                                            스터디 관리하기
                                        </Button>
                                    ) : (
                                        <Button
                                            variant="primary"
                                            fullWidth
                                            size="lg"
                                            onClick={() => setIsApplyModalOpen(true)}
                                            disabled={study.status !== 'RECRUITING' || study.currentMembers >= study.maxMembers}
                                            className="h-12 rounded-xl font-bold shadow-lg shadow-[var(--color-primary-alpha-30)]"
                                        >
                                            {study.status !== 'RECRUITING'
                                                ? '모집 마감'
                                                : study.currentMembers >= study.maxMembers
                                                    ? '정원 마감'
                                                    : '스터디 신청하기'
                                            }
                                        </Button>
                                    )}
                                </div>

                                {/* 모집 마감일 안내 */}
                                {study.recruitEndDate && study.status === 'RECRUITING' && (
                                    <div className="mt-4 p-3 bg-[var(--color-warning-light)] rounded-xl text-center">
                                        <p className="text-xs font-medium text-[var(--color-text-secondary)]">
                                            모집 마감일: <span className="font-bold text-[var(--color-warning)]">{study.recruitEndDate}</span>
                                        </p>
                                    </div>
                                )}
                            </div>
                        </div>
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

export default StudyDetailPageV2;
