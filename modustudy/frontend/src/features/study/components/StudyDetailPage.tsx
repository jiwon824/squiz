import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Heart, Star, Users, Calendar, Clock, MapPin,
    Target, Award, AlertTriangle, Share2, Shield, Send
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { studyService, Study } from '../services/studyService';
import StudyApplyModal from './StudyApplyModal';
import { StudyReportModal } from './StudyReportModal';
import LeaderReviewModal from './LeaderReviewModal';
import StudyListContainer from './StudyListContainer';
import { MainLayout } from '@/layouts/MainLayout';
import { Button, ArrowButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import { useDMStore } from '@/features/dm/store/dmStore';
import { useUIStore } from '@/store/uiStore';
import { getReviewsByLeaderId, getLeaderAverageRating, LeaderReview } from '../mockData';

// ============================================
// 📦 시맨틱 스타일 상수 (Semantic Style Constants)
// Tailwind 클래스를 의미있는 이름으로 분리하여 가독성 향상
// ============================================
const styles = {
    // 페이지 컨테이너
    pageContainer: "max-w-[1200px] mx-auto py-12 animate-fadeIn",
    
    // 상단 컨트롤 바
    controlBar: "flex justify-between items-center mb-8",
    backButtonGroup: "flex items-center gap-4",
    backLabel: "text-sm font-bold text-text-secondary tracking-tight",
    reportButton: "text-text-muted hover:text-error hover:bg-error/5 flex items-center gap-2 px-4 h-10",
    
    // 히어로 섹션
    heroSection: cn(
        "bg-white border border-border-light rounded-[40px] p-12 mb-10",
        "relative overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.04)]"
    ),
    heroBlur: "absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full -mr-40 -mt-40 blur-[80px] pointer-events-none",
    heroContent: "relative z-10",
    
    // 뱃지 영역
    badgeRow: "flex flex-wrap items-center gap-2 mb-6",
    badgeDot: "text-text-tertiary/50",
    badgeText: "text-[12px] font-semibold text-text-secondary",
    badgeTopic: "text-[12px] font-bold text-primary",
    
    // 타이틀 영역
    titleArea: "flex flex-col md:flex-row md:items-start justify-between gap-10",
    titleWrapper: "max-w-2xl",
    title: "text-3xl md:text-4xl lg:text-5xl font-black text-text-primary mb-6 leading-tight tracking-tight",
    description: "text-base md:text-lg text-text-secondary leading-relaxed font-medium opacity-70",
    actionButtons: "flex items-center gap-2",
    iconButton: "w-12 h-12 transition-all duration-300 hover:scale-110",
    
    // 메인 콘텐츠 그리드
    contentGrid: "grid grid-cols-1 lg:grid-cols-12 gap-10 items-start",
    mainColumn: "lg:col-span-8 space-y-10",
    sideColumn: "lg:col-span-4 space-y-8",
    
    // 섹션 카드
    sectionCard: "bg-white border border-border-light rounded-[32px] p-10 shadow-sm",
    sectionTitle: "text-lg font-bold text-text-primary mb-8 flex items-center gap-3",
    sectionIcon: "p-2 bg-primary/10 text-primary rounded-xl",
    infoGrid: "grid grid-cols-1 sm:grid-cols-2 gap-10",
    
    // 커리큘럼 아이템
    curriculumItem: "flex gap-4 p-4 rounded-2xl bg-surface-50 hover:bg-surface-100 transition-colors border border-border-light/50",
    weekBadge: "flex-shrink-0 w-16 flex flex-col items-center justify-center bg-white rounded-xl border border-primary/20 shadow-sm h-16",
    weekLabel: "text-[10px] font-bold text-text-tertiary uppercase tracking-wider",
    weekNumber: "text-xl font-black text-primary",
    curriculumText: "flex-grow flex items-center",
    
    // 리더 카드 (사이드바)
    leaderCard: cn(
        "bg-white border border-border-light rounded-[40px] p-10",
        "shadow-[0_10px_40px_rgba(0,0,0,0.03)]",
        "flex flex-col items-center sticky top-10"
    ),
    
    // 리더 아바타
    avatarWrapper: "relative mb-6",
    avatar: "w-24 h-24 rounded-[32px] overflow-hidden border-4 border-white shadow-xl relative group",
    avatarImage: "w-full h-full object-cover transition-transform duration-500 group-hover:scale-110",
    avatarFallback: "w-full h-full bg-primary text-white flex items-center justify-center text-3xl font-black",
    onlineIndicator: "absolute -bottom-2 -right-2 w-8 h-8 bg-success border-4 border-white rounded-full shadow-lg",
    
    // 리더 정보
    leaderInfo: "text-center mb-10 w-full",
    leaderName: "text-2xl font-black text-text-primary tracking-tight mb-2 truncate",
    ratingWrapper: "flex flex-col items-center gap-2",
    ratingButton: cn(
        "flex items-center gap-2 px-4 py-1.5",
        "bg-background-secondary/50 rounded-full border border-border-light/30",
        "hover:bg-primary/10 hover:border-primary/30",
        "transition-all cursor-pointer group"
    ),
    ratingStar: "text-yellow-400 fill-current group-hover:scale-110 transition-transform",
    ratingValue: "text-sm font-black text-text-primary",
    ratingCount: "text-[10px] font-bold text-text-tertiary opacity-60 group-hover:text-primary transition-colors",
    
    // 버튼 그룹
    buttonGroup: "w-full space-y-3",
    inquiryButton: "h-14 rounded-2xl border-border-light/80 text-text-secondary font-black hover:bg-background-secondary transition-all active:scale-95 shadow-sm",
    primaryButton: "h-14 rounded-2xl font-black text-base shadow-xl shadow-primary/20 active:scale-95 transition-all text-white border-none",
    
    // 인증 배지
    verifiedBadge: "mt-6 text-[11px] font-bold text-text-tertiary flex items-center gap-1.5 opacity-60",
    verifiedIcon: "text-primary/60",
};

// 상태별 뱃지 스타일
const getBadgeStyle = (status: string) => cn(
    "px-2.5 py-0.5 rounded-full text-[11px] font-bold",
    status === 'RECRUITING' ? "bg-success text-white" : "bg-gray-400 text-white"
);

// 난이도별 텍스트 색상
const getDifficultyColor = (difficulty: string) => cn(
    "text-[12px] font-semibold",
    difficulty === 'BEGINNER' || difficulty === 'ELEMENTARY' ? "text-success" :
    difficulty === 'INTERMEDIATE' ? "text-primary" : "text-error"
);

const StudyDetailPage: React.FC = () => {
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
        if (id) {
            const data = studyService.getStudyById(Number(id));
            if (data) {
                setStudy(data);
                setIsBookmarked(data.isBookmarked);
            }
        }
    }, [id]);

    if (!study) return null;

    const handleBookmarkToggle = () => {
        studyService.toggleBookmark(study.id);
        setIsBookmarked(!isBookmarked);
    };

    const handleReportSubmit = (reason: string) => {
        console.log(`[REPORT] Study ID: ${study.id}, Reason: ${reason}`);
    };

    // 스터디장에게 문의하기 (친구 여부와 관계없이 DM 가능)
    const handleInquiry = () => {
        // 대화 목록 새로고침
        fetchConversations();
        // 스터디장과 DM 시작 (친구가 아니어도 스터디 문의로 DM 가능)
        startConversationWith({
            id: study.leader.id,
            nickname: study.leader.nickname,
            profileImage: study.leader.profileImage
        });
        // DM 탭으로 전환
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

    const getDifficultyConfig = (difficulty: string) => {
        switch (difficulty) {
            case 'BEGINNER':
            case 'ELEMENTARY':
                return { text: '입문', color: 'text-success border-success/30' };
            case 'INTERMEDIATE':
                return { text: '중급', color: 'text-primary border-primary/30' };
            case 'ADVANCED':
                return { text: '고급', color: 'text-error border-error/30' };
            default:
                return { text: difficulty, color: 'text-text-tertiary border-border-light' };
        }
    };

    const diffConfig = getDifficultyConfig(study.difficulty);

    return (
        <MainLayout>
            <StudyListContainer className="px-6">
                <div className={styles.pageContainer}>
                    {/* 상단 컨트롤 바 */}
                    <div className={styles.controlBar}>
                        <div className={styles.backButtonGroup}>
                            <ArrowButton
                                direction="left"
                                onClick={() => navigate('/study')}
                                size="md"
                            />
                            <span className={styles.backLabel}>상세 정보</span>
                        </div>
                        <Button
                            variant="google-ghost"
                            size="sm"
                            onClick={() => setIsReportModalOpen(true)}
                            className={styles.reportButton}
                        >
                            <AlertTriangle size={16} />
                            <span className="text-xs font-bold">신고하기</span>
                        </Button>
                    </div>

                    {/* 히어로 섹션 */}
                    <header className={styles.heroSection}>
                        <div className={styles.heroBlur} />

                        <div className={styles.heroContent}>
                            {/* 뱃지 영역 - 컴팩트하게 한 줄로 */}
                            <div className={styles.badgeRow}>
                                <span className={getBadgeStyle(study.status)}>
                                    {study.status === 'RECRUITING' ? '모집중' : '진행중'}
                                </span>
                                <span className={styles.badgeDot}>•</span>
                                <span className={styles.badgeText}>
                                    {study.meetingType === 'ONLINE' ? '온라인' : study.meetingType === 'OFFLINE' ? '오프라인' : '혼합'}
                                </span>
                                <span className={styles.badgeDot}>•</span>
                                <span className={getDifficultyColor(study.difficulty)}>
                                    {diffConfig.text}
                                </span>
                                <span className="text-text-tertiary/50">•</span>
                                <span className={styles.badgeTopic}>
                                    {study.topic}
                                </span>
                            </div>

                            <div className={styles.titleArea}>
                                <div className={styles.titleWrapper}>
                                    <h1 className={styles.title}>
                                        {study.name}
                                    </h1>
                                    <p className={styles.description}>
                                        {study.description}
                                    </p>
                                </div>
                                <div className={styles.actionButtons}>
                                    <Button
                                        variant="google-ghost"
                                        size="lg"
                                        isCircle
                                        onClick={handleBookmarkToggle}
                                        className={cn(
                                            styles.iconButton,
                                            isBookmarked ? "text-error" : "text-text-muted hover:text-error"
                                        )}
                                    >
                                        <Heart size={28} fill={isBookmarked ? 'currentColor' : 'none'} />
                                    </Button>
                                    <Button
                                        variant="google-ghost"
                                        size="lg"
                                        isCircle
                                        className={cn(styles.iconButton, "text-text-muted hover:text-primary")}
                                        onClick={() => {
                                            navigator.clipboard.writeText(window.location.href);
                                            showToast('링크가 클립보드에 복사되었습니다!', 'success');
                                        }}
                                    >
                                        <Share2 size={24} />
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </header>

                    {/* 메인 콘텐츠 그리드 */}
                    <div className={styles.contentGrid}>
                        {/* 상세 정보 (좌측 8열) */}
                        <div className={styles.mainColumn}>
                            {/* 모집 요강 카드 */}
                            <section className={styles.sectionCard}>
                                <h2 className={styles.sectionTitle}>
                                    <div className={styles.sectionIcon}>
                                        <Target size={20} />
                                    </div>
                                    모집 정보
                                </h2>
                                <div className={styles.infoGrid}>
                                    <InfoItem label="활동 지역" value={study.region?.name || '전국 (온라인)'} icon={<MapPin size={18} />} />
                                    <InfoItem label="모집 인원" value={`현재 ${study.currentMembers}명 / 최대 ${study.maxMembers}명`} icon={<Users size={18} />} />
                                    <InfoItem label="활동 요일" value={study.scheduleDays} icon={<Calendar size={18} />} />
                                    <InfoItem label="활동 시간" value={study.scheduleTime ? `${study.scheduleTime.substring(0, 5)} ~` : '협의 후 결정'} icon={<Clock size={18} />} />
                                </div>
                            </section>

                            {/* 커리큘럼 섹션 */}
                            <section className={styles.sectionCard}>
                                <h2 className={styles.sectionTitle}>
                                    <div className={styles.sectionIcon}>
                                        <Award size={20} />
                                    </div>
                                    스터디 운영 계획
                                </h2>
                                
                                {study.curriculum && study.curriculum.length > 0 ? (
                                    <div className="flex flex-col gap-6">
                                        <div className="text-text-secondary leading-relaxed font-medium mb-4">
                                            <p>본 스터디는 <strong className="text-primary font-black">{study.topic}</strong> 분야의 전문성 향상을 목표로 다음과 같이 진행됩니다.</p>
                                        </div>
                                        
                                        <div className="space-y-4">
                                            {study.curriculum.map((item, index) => (
                                                <div key={index} className={styles.curriculumItem}>
                                                    <div className={styles.weekBadge}>
                                                        <span className={styles.weekLabel}>WEEK</span>
                                                        <span className={styles.weekNumber}>{item.week}</span>
                                                    </div>
                                                    <div className={styles.curriculumText}>
                                                        <p className="text-text-primary font-medium leading-relaxed">
                                                            {item.description}
                                                        </p>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="text-text-secondary leading-relaxed font-medium space-y-4">
                                        <p>
                                            본 스터디는 <strong className="text-primary font-black">{study.topic}</strong> 분야의 전문성 향상을 목표로 합니다.
                                        </p>
                                        <p>
                                            단순한 지식 습득을 넘어, 실천적인 프로젝트나 심화 토론을 통해 참여자 모두가 실질적인 성장을 이룰 수 있도록 운영될 예정입니다.
                                            열정적인 팀원들과 함께 지식의 경계를 넓히고 싶으신 분들의 적극적인 참여를 기다립니다.
                                        </p>
                                    </div>
                                )}
                            </section>
                        </div>

                        {/* 사이드바 (우측 4열): 리더 프로필 */}
                        <div className={styles.sideColumn}>
                            <div className={styles.leaderCard}>
                                {/* 아바타 섹션 */}
                                <div className={styles.avatarWrapper}>
                                    <div className={styles.avatar}>
                                        {study.leader.profileImage ? (
                                            <img
                                                src={study.leader.profileImage}
                                                alt={study.leader.nickname}
                                                className={styles.avatarImage}
                                            />
                                        ) : (
                                            <div className={styles.avatarFallback}>
                                                {study.leader.nickname.charAt(0)}
                                            </div>
                                        )}
                                    </div>
                                    <div className={styles.onlineIndicator} />
                                </div>

                                {/* 리더 정보 */}
                                <div className={styles.leaderInfo}>
                                    <h3 className={styles.leaderName}>
                                        {study.leader.nickname}
                                    </h3>
                                    <div className={styles.ratingWrapper}>
                                        <button onClick={handleRatingClick} className={styles.ratingButton}>
                                            <Star size={14} className={styles.ratingStar} />
                                            <span className={styles.ratingValue}>{study.leader.leaderRating.toFixed(1)}</span>
                                            <span className={styles.ratingCount}>리뷰 {study.leader.leaderReviewCount}개</span>
                                        </button>
                                    </div>
                                </div>

                                {/* 버튼 액션 그룹 */}
                                <div className={styles.buttonGroup}>
                                    <Button
                                        variant="google-outline"
                                        fullWidth
                                        onClick={handleInquiry}
                                        leftIcon={<Send size={18} />}
                                        className={styles.inquiryButton}
                                    >
                                        문의하기
                                    </Button>

                                    {String(user?.id) === String(study.leader.id) ? (
                                        <Button
                                            variant="primary"
                                            fullWidth
                                            size="lg"
                                            onClick={() => navigate(`/study/manage/${study.id}`)}
                                            className={styles.primaryButton}
                                        >
                                            스터디 관리 모드
                                        </Button>
                                    ) : (
                                        <Button
                                            variant="primary"
                                            fullWidth
                                            size="lg"
                                            onClick={() => setIsApplyModalOpen(true)}
                                            className={styles.primaryButton}
                                        >
                                            스터디 신청하기
                                        </Button>
                                    )}
                                </div>

                                <p className={styles.verifiedBadge}>
                                    <Shield size={12} className={styles.verifiedIcon} />
                                    인증된 리더가 운영하는 스터디입니다
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </StudyListContainer>

            {/* 모달 관리 */}
            {study && (
                <StudyApplyModal
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

            {/* 리더 리뷰 모달 */}
            <LeaderReviewModal
                isOpen={isReviewModalOpen}
                onClose={() => setIsReviewModalOpen(false)}
                leaderNickname={study.leader.nickname}
                reviews={leaderReviews}
                averageRating={leaderAvgRating}
            />
        </MainLayout>
    );
};

// 범용 정보 아이템 컴포넌트
const InfoItem: React.FC<{ label: string; value: string; icon: React.ReactNode }> = ({ label, value, icon }) => (
    <div className="flex flex-col gap-3">
        <span className="text-[10px] font-black text-text-tertiary uppercase tracking-[0.2em]">{label}</span>
        <div className="flex items-center gap-3">
            <div className="p-2 bg-background-secondary/50 rounded-xl text-primary/60">
                {icon}
            </div>
            <span className="text-[15px] font-bold text-text-primary tracking-tight">{value}</span>
        </div>
    </div>
);

export default StudyDetailPage;
