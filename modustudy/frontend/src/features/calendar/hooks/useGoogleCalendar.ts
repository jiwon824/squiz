import { useState, useEffect } from 'react';
import { useCalendarStore } from '../services/calendarStore';
import { useUIStore } from '@/store/uiStore';

/**
 * Google Calendar 연동 훅
 */
export const useGoogleCalendar = () => {
    const {
        googleConnected,
        googleEmail,
        googleLastSyncAt,
        connectGoogle,
        disconnectGoogle,
        syncGoogle,
        checkGoogleStatus
    } = useCalendarStore();
    const { showToast } = useUIStore();

    const [connecting, setConnecting] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [disconnecting, setDisconnecting] = useState(false);

    // 초기 상태 확인
    useEffect(() => {
        checkGoogleStatus();
    }, []);

    // Google 연동 시작 (리다이렉트 방식)
    const handleConnect = async () => {
        setConnecting(true);
        try {
            const authUrl = await connectGoogle();

            // OAuth 시작 전 현재 경로 저장 (콜백 후 복귀용)
            sessionStorage.setItem('oauth_redirect_path', '/calendar');

            // Google OAuth 페이지로 리다이렉트
            window.location.href = authUrl;
        } catch (error: any) {
            showToast(error.message || 'Google 연동에 실패했습니다.', 'error');
            setConnecting(false);
        }
    };

    // Google 연동 해제
    const handleDisconnect = async () => {
        if (!confirm('Google Calendar 연동을 해제하시겠습니까?\n동기화된 일정은 삭제됩니다.')) {
            return;
        }

        setDisconnecting(true);
        try {
            await disconnectGoogle();
            showToast('Google Calendar 연동이 해제되었습니다.', 'success');
        } catch (error: any) {
            showToast(error.message || '연동 해제에 실패했습니다.', 'error');
        } finally {
            setDisconnecting(false);
        }
    };

    // Google 동기화 트리거
    const handleSync = async () => {
        setSyncing(true);
        try {
            await syncGoogle();
            showToast('Google Calendar 동기화가 완료되었습니다.', 'success');
        } catch (error: any) {
            showToast(error.message || '동기화에 실패했습니다.', 'error');
        } finally {
            setSyncing(false);
        }
    };

    return {
        googleConnected,
        googleEmail,
        googleLastSyncAt,
        connecting,
        syncing,
        disconnecting,
        handleConnect,
        handleDisconnect,
        handleSync
    };
};
