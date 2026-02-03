/**
 * 알림 전체 보기 페이지
 * 메일함 스타일의 알림 목록 UI
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotificationStore } from '../store/notificationStore';
import { NotificationItem, NotificationType } from '../api/notificationApi';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { cn } from '@/shared/utils/cn';
import { PageNavHeader } from '@/shared/components/layouts';
import {
    Bell,
    MessageSquare,
    Calendar,
    CheckCircle,
    Users,
    UserPlus,
    HelpCircle,
    Settings,
    CheckCheck,
    Inbox,
    ExternalLink,
    Play,
    CalendarPlus,
    UserCheck,
} from 'lucide-react';

// 알림 타입별 아이콘 및 색상
const notificationTypeConfig: Record<NotificationType, { icon: React.ReactNode; color: string; label: string }> = {
    CHAT: { icon: <MessageSquare size={18} />, color: 'text-blue-500 bg-blue-50', label: '채팅' },
    SCHEDULE: { icon: <Calendar size={18} />, color: 'text-purple-500 bg-purple-50', label: '일정' },
    ATTENDANCE: { icon: <CheckCircle size={18} />, color: 'text-green-500 bg-green-50', label: '출석' },
    STUDY_UPDATE: { icon: <Users size={18} />, color: 'text-orange-500 bg-orange-50', label: '스터디' },
    STUDY_APPLICATION: { icon: <UserPlus size={18} />, color: 'text-indigo-500 bg-indigo-50', label: '신청' },
    STUDY_RECRUITMENT_COMPLETE: { icon: <UserCheck size={18} />, color: 'text-emerald-500 bg-emerald-50', label: '모집완료' },
    STUDY_EXTENSION: { icon: <CalendarPlus size={18} />, color: 'text-amber-500 bg-amber-50', label: '모집연장' },
    STUDY_START: { icon: <Play size={18} />, color: 'text-green-500 bg-green-50', label: '스터디시작' },
    QUIZ: { icon: <HelpCircle size={18} />, color: 'text-pink-500 bg-pink-50', label: '퀴즈' },
    SYSTEM: { icon: <Settings size={18} />, color: 'text-gray-500 bg-gray-50', label: '시스템' },
    FRIEND: { icon: <UserPlus size={18} />, color: 'text-cyan-500 bg-cyan-50', label: '친구' },
};

// 필터 타입
type FilterType = 'all' | 'unread';

export const NotificationPage = () => {
    const navigate = useNavigate();
    const {
        notifications,
        unreadCount,
        isLoading,
        fetchNotifications,
        markNotificationAsRead,
        markAllNotificationsAsRead,
    } = useNotificationStore();

    const [selectedNotification, setSelectedNotification] = useState<NotificationItem | null>(null);
    const [filter, setFilter] = useState<FilterType>('all');

    // 초기 로드
    useEffect(() => {
        fetchNotifications(0, 50);
    }, [fetchNotifications]);

    // 필터링된 알림 목록
    const filteredNotifications = notifications.filter((n) => {
        if (filter === 'all') return true;
        if (filter === 'unread') return !n.isRead;
        return true;
    });

    // 알림 선택 핸들러
    const handleSelectNotification = (notification: NotificationItem) => {
        setSelectedNotification(notification);
        if (!notification.isRead) {
            markNotificationAsRead(notification.id);
        }
    };

    // 전체 읽음 처리
    const handleMarkAllAsRead = () => {
        markAllNotificationsAsRead();
    };

    // 해당 페이지로 이동
    const handleNavigate = (notification: NotificationItem) => {
        const { referenceType, referenceId, type } = notification;
        if (!referenceType || !referenceId) return;

        switch (referenceType) {
            case 'STUDY_APPLICATION':
                // 스터디 신청 알림 -> 스터디 관리 페이지의 지원자 관리 탭으로 이동
                // referenceId에 studyId가 저장되어 있음
                navigate(`/study/manage/${referenceId}?tab=applicants`);
                break;
            case 'STUDY':
                // 스터디 시작 알림 -> 워크스페이스로 이동
                if (type === 'STUDY_START') {
                    navigate(`/study/${referenceId}/workspace`);
                } else if (type === 'STUDY_EXTENSION') {
                    // 모집 연장 알림 -> 스터디 상세 페이지로 이동 (참가/불참 선택)
                    navigate(`/study/${referenceId}?action=extension`);
                } else {
                    navigate(`/study/${referenceId}`);
                }
                break;
            case 'STUDY_SESSION':
                // 세션 시작 알림 -> 해당 스터디 워크스페이스로 이동
                // referenceId에 studyId가 저장됨
                navigate(`/study/${referenceId}/workspace`);
                break;
            case 'MEETING':
                // 미팅 알림 -> 해당 스터디 워크스페이스로 이동
                // referenceId에 studyId가 저장됨
                navigate(`/study/${referenceId}/workspace`);
                break;
            case 'SCHEDULE':
                navigate('/calendar');
                break;
            case 'RECRUITMENT_POST':
                navigate(`/recruitment?postId=${referenceId}`);
                break;
            default:
                break;
        }
    };

    // 시간 포맷
    const formatTime = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now.getTime() - date.getTime();
        const minutes = Math.floor(diff / (1000 * 60));
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (minutes < 1) return '방금 전';
        if (minutes < 60) return `${minutes}분 전`;
        if (hours < 24) return `${hours}시간 전`;
        if (days < 7) return `${days}일 전`;
        return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
    };

    return (
        <UserLayoutV2>
            <div className="h-full flex flex-col bg-gray-50">
                {/* 헤더 */}
                <div className="bg-white border-b border-gray-200 px-6 py-4">
                    <PageNavHeader
                        title="알림"
                        breadcrumbs={[
                            { label: '알림' },
                        ]}
                        hideBackButton
                        badge={unreadCount > 0 ? { text: `${unreadCount}개 읽지 않음`, className: 'bg-red-100 text-red-600' } : undefined}
                        rightActions={
                            <button
                                onClick={handleMarkAllAsRead}
                                disabled={unreadCount === 0}
                                className={cn(
                                    'flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition-colors',
                                    unreadCount > 0
                                        ? 'text-study-blue hover:bg-study-blue/10'
                                        : 'text-gray-400 cursor-not-allowed'
                                )}
                            >
                                <CheckCheck size={16} />
                                모두 읽음
                            </button>
                        }
                        className="mb-4"
                    />

                    {/* 필터 탭 (전체 / 읽지 않음만) */}
                    <div className="flex items-center gap-2">
                        <FilterButton
                            active={filter === 'all'}
                            onClick={() => setFilter('all')}
                            icon={<Inbox size={14} />}
                            label="전체"
                            count={notifications.length}
                        />
                        <FilterButton
                            active={filter === 'unread'}
                            onClick={() => setFilter('unread')}
                            icon={<Bell size={14} />}
                            label="읽지 않음"
                            count={unreadCount}
                        />
                    </div>
                </div>

                {/* 메인 컨텐츠 */}
                <div className="flex-1 flex overflow-hidden">
                    {/* 알림 목록 (왼쪽) */}
                    <div className="w-full md:w-96 lg:w-[420px] border-r border-gray-200 bg-white overflow-y-auto">
                        {isLoading ? (
                            <div className="p-8 text-center text-gray-500">
                                <div className="animate-spin w-8 h-8 border-2 border-study-blue border-t-transparent rounded-full mx-auto mb-3" />
                                알림을 불러오는 중...
                            </div>
                        ) : filteredNotifications.length === 0 ? (
                            <div className="p-8 text-center text-gray-500">
                                <Inbox className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                                <p className="font-medium">알림이 없습니다</p>
                                <p className="text-sm mt-1">새로운 알림이 오면 여기에 표시됩니다</p>
                            </div>
                        ) : (
                            filteredNotifications.map((notification) => (
                                <NotificationListItem
                                    key={notification.id}
                                    notification={notification}
                                    isSelected={selectedNotification?.id === notification.id}
                                    onClick={() => handleSelectNotification(notification)}
                                    formatTime={formatTime}
                                />
                            ))
                        )}
                    </div>

                    {/* 알림 상세 (오른쪽) - 태블릿 이상에서만 */}
                    <div className="hidden md:flex flex-1 bg-white">
                        {selectedNotification ? (
                            <NotificationDetail
                                notification={selectedNotification}
                                onNavigate={() => handleNavigate(selectedNotification)}
                            />
                        ) : (
                            <div className="flex-1 flex items-center justify-center text-gray-400">
                                <div className="text-center">
                                    <Bell className="w-16 h-16 mx-auto mb-4 text-gray-200" />
                                    <p className="text-lg font-medium">알림을 선택하세요</p>
                                    <p className="text-sm mt-1">왼쪽 목록에서 알림을 클릭하면 상세 내용이 표시됩니다</p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </UserLayoutV2>
    );
};

// 필터 버튼 컴포넌트
const FilterButton = ({
    active,
    onClick,
    icon,
    label,
    count,
}: {
    active: boolean;
    onClick: () => void;
    icon: React.ReactNode;
    label: string;
    count: number;
}) => (
    <button
        onClick={onClick}
        className={cn(
            'flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-full transition-colors whitespace-nowrap',
            active
                ? 'bg-study-blue text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        )}
    >
        {icon}
        <span>{label}</span>
        {count > 0 && (
            <span className={cn(
                'ml-1 px-1.5 py-0.5 text-xs rounded-full',
                active ? 'bg-white/20' : 'bg-gray-200'
            )}>
                {count}
            </span>
        )}
    </button>
);

// 알림 목록 아이템 컴포넌트
const NotificationListItem = ({
    notification,
    isSelected,
    onClick,
    formatTime,
}: {
    notification: NotificationItem;
    isSelected: boolean;
    onClick: () => void;
    formatTime: (date: string) => string;
}) => {
    const config = notificationTypeConfig[notification.type];

    return (
        <div
            onClick={onClick}
            className={cn(
                'px-4 py-3 border-b border-gray-100 cursor-pointer transition-colors',
                isSelected && 'bg-study-blue/5 border-l-2 border-l-study-blue',
                !isSelected && 'hover:bg-gray-50',
                !notification.isRead && !isSelected && 'bg-blue-50/30'
            )}
        >
            <div className="flex items-start gap-3">
                {/* 아이콘 */}
                <div className={cn('p-2 rounded-lg flex-shrink-0', config.color)}>
                    {config.icon}
                </div>

                {/* 내용 */}
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                        <h4 className={cn(
                            'text-sm line-clamp-1',
                            notification.isRead ? 'text-gray-700' : 'text-gray-900 font-semibold'
                        )}>
                            {notification.title}
                        </h4>
                        {!notification.isRead && (
                            <span className="w-2 h-2 bg-study-blue rounded-full flex-shrink-0" />
                        )}
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">
                        {notification.content}
                    </p>
                    <div className="flex items-center gap-2 mt-1.5">
                        <span className={cn(
                            'text-[10px] px-1.5 py-0.5 rounded',
                            config.color
                        )}>
                            {config.label}
                        </span>
                        <span className="text-[10px] text-gray-400">
                            {formatTime(notification.createdAt)}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

// 알림 상세 컴포넌트
const NotificationDetail = ({
    notification,
    onNavigate,
}: {
    notification: NotificationItem;
    onNavigate: () => void;
}) => {
    const config = notificationTypeConfig[notification.type];

    // 버튼 텍스트
    const getButtonText = () => {
        switch (notification.type) {
            case 'STUDY_START':
                return '워크스페이스로 이동';
            case 'STUDY_EXTENSION':
                return '참가 여부 확인하기';
            case 'STUDY_APPLICATION':
                return '지원자 관리로 이동';
            case 'STUDY_RECRUITMENT_COMPLETE':
                return '스터디 관리로 이동';
            default:
                return '해당 페이지로 이동';
        }
    };

    return (
        <div className="flex-1 p-6 overflow-y-auto">
            {/* 헤더 */}
            <div className="flex items-start gap-4 pb-4 border-b border-gray-100">
                <div className={cn('p-3 rounded-xl', config.color)}>
                    {config.icon}
                </div>
                <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                        <span className={cn('text-xs px-2 py-0.5 rounded-full', config.color)}>
                            {config.label}
                        </span>
                        <span className="text-xs text-gray-400">
                            {new Date(notification.createdAt).toLocaleString('ko-KR', {
                                year: 'numeric',
                                month: 'long',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit',
                            })}
                        </span>
                    </div>
                    <h2 className="text-xl font-bold text-gray-900">{notification.title}</h2>
                </div>
            </div>

            {/* 본문 */}
            <div className="py-6">
                <p className="text-gray-700 whitespace-pre-wrap leading-relaxed">
                    {notification.content}
                </p>
            </div>

            {/* 액션 버튼 */}
            {notification.referenceType && notification.referenceId && (
                <div className="pt-4 border-t border-gray-100">
                    <button
                        onClick={onNavigate}
                        className="flex items-center gap-2 px-4 py-2 bg-study-blue text-white text-sm font-medium rounded-lg hover:bg-study-blue-dark transition-colors"
                    >
                        <ExternalLink size={16} />
                        {getButtonText()}
                    </button>
                </div>
            )}
        </div>
    );
};

export default NotificationPage;
