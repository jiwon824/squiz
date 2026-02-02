import { Button } from '@/shared/components';
import { useGoogleCalendar } from '../hooks';
import { RefreshCw, Link2, Unlink } from 'lucide-react';

interface GoogleCalendarSyncProps {
    className?: string;
}

/**
 * Google Calendar 연동 UI 컴포넌트
 */
export const GoogleCalendarSync = ({ className = '' }: GoogleCalendarSyncProps) => {
    const {
        googleConnected,
        googleEmail,
        googleLastSyncAt,
        connecting,
        syncing,
        disconnecting,
        handleConnect,
        handleSync,
        handleDisconnect
    } = useGoogleCalendar();

    return (
        <div className={`bg-white rounded-2xl border border-border-light p-5 ${className}`}>
            {/* 헤더 */}
            <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                    <svg className="w-5 h-5 text-primary" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                        <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                        <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                        <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                    </svg>
                </div>
                <div>
                    <h3 className="font-bold text-text-primary text-sm">Google Calendar</h3>
                    <p className="text-xs text-text-tertiary">
                        {googleConnected ? '연동됨' : '연동되지 않음'}
                    </p>
                </div>
            </div>

            {/* 연동 상태 */}
            {googleConnected ? (
                <div className="space-y-4">
                    {/* 이메일 */}
                    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                        <div>
                            <p className="text-xs text-gray-500 mb-1">연동 계정</p>
                            <p className="text-sm font-medium text-gray-900">{googleEmail}</p>
                        </div>
                        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                    </div>

                    {/* 마지막 동기화 시간 */}
                    {googleLastSyncAt && (
                        <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                            <div>
                                <p className="text-xs text-gray-500 mb-1">마지막 동기화</p>
                                <p className="text-sm font-medium text-gray-900">
                                    {new Date(googleLastSyncAt).toLocaleString('ko-KR', {
                                        month: 'long',
                                        day: 'numeric',
                                        hour: '2-digit',
                                        minute: '2-digit'
                                    })}
                                </p>
                            </div>
                        </div>
                    )}

                    {/* 버튼 */}
                    <div className="flex gap-2">
                        <Button
                            variant="outline"
                            onClick={() => handleSync()}
                            isLoading={syncing}
                            leftIcon={<RefreshCw size={16} />}
                            className="flex-1"
                        >
                            동기화
                        </Button>
                        <Button
                            variant="outline"
                            onClick={handleDisconnect}
                            isLoading={disconnecting}
                            leftIcon={<Unlink size={16} />}
                            className="text-red-600 border-red-300 hover:bg-red-50"
                        >
                            연동 해제
                        </Button>
                    </div>

                    {/* 안내 메시지 */}
                    <p className="text-xs text-gray-500 leading-relaxed">
                        💡 Google Calendar와 양방향으로 동기화됩니다.
                        앱에서 만든 일정은 Google Calendar에도 자동 등록됩니다.
                    </p>
                </div>
            ) : (
                <div className="space-y-4">
                    {/* 연동 안내 */}
                    <div className="p-4 bg-blue-50 rounded-lg">
                        <p className="text-sm text-blue-900 leading-relaxed">
                            Google Calendar와 연동하여 모든 일정을 한곳에서 관리하세요.
                        </p>
                    </div>

                    {/* 연동 기능 설명 */}
                    <ul className="space-y-2">
                        <li className="flex items-start gap-2 text-sm text-gray-600">
                            <span className="text-green-500">✓</span>
                            <span>양방향 동기화 (읽기/쓰기 권한)</span>
                        </li>
                        <li className="flex items-start gap-2 text-sm text-gray-600">
                            <span className="text-green-500">✓</span>
                            <span>Google Calendar 일정 자동 반영</span>
                        </li>
                        <li className="flex items-start gap-2 text-sm text-gray-600">
                            <span className="text-green-500">✓</span>
                            <span>통합 캘린더에서 스마트한 일정 관리</span>
                        </li>
                    </ul>

                    {/* 연동 버튼 */}
                    <Button
                        variant="primary"
                        onClick={handleConnect}
                        isLoading={connecting}
                        leftIcon={<Link2 size={18} />}
                        className="w-full"
                    >
                        Google 계정으로 연동하기
                    </Button>

                    {/* 안내 메시지 */}
                    <p className="text-xs text-gray-500 leading-relaxed">
                        🔒 Google OAuth 2.0을 통해 안전하게 연동됩니다.
                        일정 관리를 위해 캘린더 접근 권한이 필요합니다.
                    </p>
                </div>
            )}
        </div>
    );
};
