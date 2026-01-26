import { useState, useEffect } from 'react';
import { useCalendarStore } from '../services/calendarStore';
import { useUIStore } from '@/store/uiStore';
import { formatDate } from '../utils';

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

    // Google 연동 시작
    const handleConnect = async () => {
        setConnecting(true);
        try {
            const authUrl = await connectGoogle();

            // 새 창으로 OAuth 페이지 열기
            const width = 600;
            const height = 700;
            const left = window.screen.width / 2 - width / 2;
            const top = window.screen.height / 2 - height / 2;

            const popup = window.open(
                authUrl,
                'Google Calendar 연동',
                `width=${width},height=${height},left=${left},top=${top}`
            );

            // 팝업 닫힘 감지 (OAuth 완료 후)
            const checkPopup = setInterval(() => {
                if (popup?.closed) {
                    clearInterval(checkPopup);
                    // 상태 재확인
                    setTimeout(() => {
                        checkGoogleStatus();
                    }, 1000);
                }
            }, 500);

        } catch (error: any) {
            showToast(error.message || 'Google 연동에 실패했습니다.', 'error');
        } finally {
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

    // Google 동기화
    const handleSync = async (currentDate: Date) => {
        setSyncing(true);
        try {
            const year = currentDate.getFullYear();
            const month = currentDate.getMonth();
            const startDate = formatDate(new Date(year, month, 1));
            const endDate = formatDate(new Date(year, month + 1, 0));

            await syncGoogle(startDate, endDate);
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
