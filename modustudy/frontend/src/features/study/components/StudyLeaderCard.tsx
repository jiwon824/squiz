import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Star, Shield, Send, MessageCircle } from 'lucide-react';
import { Button } from '@/shared/components';
import { useUIStore } from '@/store/uiStore';
import { cn } from '@/shared/utils/cn';

// 기본 프로필 이미지 경로
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

/**
 * 스터디 리더 정보 카드 컴포넌트
 * 리더 프로필, 평점, 문의/신청 버튼을 표시
 */

// 리더 정보 타입
export interface LeaderInfo {
    id: number;
    nickname: string;
    profileImage?: string | null;
    leaderRating: number;
    leaderReviewCount: number;
    loginProvider?: 'KAKAO' | 'GOOGLE' | 'NAVER'; // 스터디장의 로그인 방식
}

// 컴포넌트 Props
interface StudyLeaderCardProps {
    leader: LeaderInfo;
    studyId: number;
    studyStatus: string;
    currentMembers: number;
    maxMembers: number;
    recruitEndDate?: string;
    isOwner: boolean;
    onInquiry: () => void;
    onKakaoInquiry?: () => void; // 카카오톡 문의 핸들러 (백엔드 연동 시 사용)
    onRatingClick: () => void;
    onApply: () => void;
}

const StudyLeaderCard: React.FC<StudyLeaderCardProps> = ({
    leader,
    studyId,
    studyStatus,
    currentMembers,
    maxMembers,
    recruitEndDate,
    isOwner,
    onInquiry,
    onKakaoInquiry,
    onRatingClick,
    onApply,
}) => {
    const navigate = useNavigate();
    const { showToast } = useUIStore();

    // 스터디장이 카카오 로그인 사용자인지 확인
    const isLeaderKakaoUser = leader.loginProvider === 'KAKAO';

    // 카카오톡 문의 핸들러
    const handleKakaoInquiry = () => {
        if (!isLeaderKakaoUser) {
            // 스터디장이 카카오 로그인 사용자가 아닌 경우 토스트 경고 표시
            showToast('스터디장이 카카오 로그인 사용자가 아닙니다.', 'error');
            return;
        }

        if (onKakaoInquiry) {
            onKakaoInquiry();
        } else {
            // TODO: 백엔드 연동 후 실제 카카오톡 메시지 API 호출
            showToast('카카오톡 메시지 기능은 준비 중입니다.', 'info');
        }
    };

    // 신청 버튼 텍스트 결정 (상태별 분기)
    const getApplyButtonText = () => {
        switch (studyStatus) {
            case 'RECRUITING':
                if (currentMembers >= maxMembers) return '정원 마감';
                return '스터디 신청하기';
            case 'SCHEDULED':
            case 'PENDING':
                return '모집 예정';
            case 'RECRUIT_CLOSED':
                return '모집 마감';
            case 'IN_PROGRESS':
                return '진행중';
            case 'COMPLETED':
                return '완료됨';
            case 'CANCELLED':
                return '취소됨';
            case 'DRAFT':
                return '준비중';
            default:
                return '모집 마감';
        }
    };

    // D-day 계산
    const getDday = (dateString: string) => {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const targetDate = new Date(dateString);
        targetDate.setHours(0, 0, 0, 0);
        const diffTime = targetDate.getTime() - today.getTime();
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 0) return 'D-Day';
        if (diffDays > 0) return `D-${diffDays}`;
        return `D+${Math.abs(diffDays)}`;
    };

    // 날짜 포맷팅 (YYYY.MM.DD)
    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}.${month}.${day}`;
    };

    // 신청 버튼 비활성화 여부
    const isApplyDisabled = studyStatus !== 'RECRUITING' || currentMembers >= maxMembers;

    return (
        <div className="2xl:col-span-1">
            <div className="2xl:fixed 2xl:top-[168px] 2xl:z-40">
                <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 shadow-sm">
                {/* 리더 프로필 */}
                <div className="text-center mb-6">
                    <div className="relative inline-block mb-4">
                        <img
                            src={leader.profileImage || DEFAULT_PROFILE_IMAGE}
                            alt={leader.nickname}
                            className={cn(
                                'w-20 h-20 rounded-2xl object-cover',
                                'border-2 border-white shadow-lg'
                            )}
                        />
                        {studyStatus === 'RECRUITING' && (
                            <span className="absolute -bottom-1 -right-1 w-5 h-5 bg-[var(--color-success)] border-2 border-white rounded-full" />
                        )}
                    </div>

                    <h3 className="text-xl font-bold text-[var(--color-text-primary)] mb-2">
                        {leader.nickname}
                    </h3>

                    {/* 평점 */}
                    <button
                        onClick={onRatingClick}
                        className={cn(
                            'inline-flex items-center gap-1.5 px-3 py-1.5',
                            'bg-[var(--color-background-secondary)] rounded-full',
                            'hover:bg-[var(--color-primary-alpha-10)] transition-colors group'
                        )}
                    >
                        <Star size={14} className="text-yellow-400 fill-current" />
                        <span className="text-sm font-bold text-[var(--color-text-primary)]">
                            {leader.leaderRating.toFixed(1)}
                        </span>
                        <span className="text-xs text-[var(--color-text-tertiary)] group-hover:text-[var(--color-primary)]">
                            ({leader.leaderReviewCount}개 리뷰)
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
                    {/* 스터디 신청/관리 버튼 (첫 번째 줄) */}
                    {isOwner ? (
                        <Button
                            variant="primary"
                            fullWidth
                            size="lg"
                            onClick={() => navigate(`/study/manage/${studyId}`)}
                            className={cn(
                                'h-12 rounded-xl font-bold',
                                'shadow-lg shadow-[var(--color-primary-alpha-30)]'
                            )}
                        >
                            스터디 관리하기
                        </Button>
                    ) : (
                        <Button
                            variant="primary"
                            fullWidth
                            size="lg"
                            onClick={onApply}
                            disabled={isApplyDisabled}
                            className={cn(
                                'h-12 rounded-xl font-bold',
                                'shadow-lg shadow-[var(--color-primary-alpha-30)]'
                            )}
                        >
                            {getApplyButtonText()}
                        </Button>
                    )}
                    {/* 구분선 또는 안내 문구
                    <p className="text-xs text-center text-[var(--color-text-tertiary)] my-3">
                    궁금한 점이 있다면 아래로 문의해주세요.
                    </p> */}

                    {/* 문의 버튼들 (가로 배치) */}
                    <div className="flex gap-2">
                        {/* DM 문의 버튼 */}
                        <Button
                            variant="google-outline"
                            fullWidth
                            onClick={onInquiry}
                            leftIcon={<Send size={14} />}
                            className="h-11 rounded-xl font-semibold text-xs"
                        >
                            문의하기
                        </Button>

                        {/* 카카오톡 문의 버튼 */}
                        <Button
                            variant="google-outline"
                            fullWidth
                            onClick={handleKakaoInquiry}
                            leftIcon={<MessageCircle size={14} fill="#3C1E1E" />}
                            className={cn(
                                'h-11 rounded-xl font-semibold text-xs',
                                'bg-[#FEE500] hover:bg-[#FDD835]',
                                'text-[#3C1E1E] border-[#FEE500] hover:border-[#FDD835]'
                            )}
                        >
                            카카오톡
                        </Button>
                    </div>
                    </div>
                </div>

                {/* 모집 마감일 안내 - 카드 바깥, sticky 컨테이너 안 */}
                {recruitEndDate && (studyStatus === 'RECRUITING' || studyStatus === 'PENDING') && (
                    <div className={cn(
                        'mt-3 pt-2 rounded-xl',
                        'bg-[#FCE8E6]',
                        'flex flex-col items-center justify-center'
                    )}>
                        <p className="text-sm font-medium text-[var(--color-text-secondary)] mb-2 leading-none">
                            모집 마감까지 <span className="font-bold text-[#EA4335]">{getDday(recruitEndDate)}</span>
                        </p>
                        <p className="text-xs text-[var(--color-text-tertiary)] mb-2">
                            {formatDate(recruitEndDate)}
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default StudyLeaderCard;
