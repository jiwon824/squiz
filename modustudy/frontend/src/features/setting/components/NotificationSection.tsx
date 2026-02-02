/**
 * NotificationSection 컴포넌트
 * 알림 설정 섹션을 담당합니다.
 * 각 알림 타입별 ON/OFF 토글을 제공합니다.
 */

import { Bell, Calendar, CheckCircle, BookOpen, HelpCircle, Settings, UserPlus } from 'lucide-react';
import { useSettingStore } from '../store/settingStore';
import { ToggleSwitch } from './ToggleSwitch';
import type { NotificationType } from '../types';

// 알림 타입별 아이콘 및 설명 매핑
const notificationConfig: Record<
    NotificationType,
    { icon: typeof Bell; color: string; description: string }
> = {
    CHAT: {
        icon: Bell,
        color: '#3b82f6',
        description: '새로운 채팅 메시지 알림',
    },
    SCHEDULE: {
        icon: Calendar,
        color: '#8b5cf6',
        description: '스터디 일정 및 리마인더 알림',
    },
    ATTENDANCE: {
        icon: CheckCircle,
        color: '#10b981',
        description: '출석 체크 및 출석 관련 알림',
    },
    STUDY_UPDATE: {
        icon: BookOpen,
        color: '#f59e0b',
        description: '스터디 공지사항 및 변경 알림',
    },
    QUIZ: {
        icon: HelpCircle,
        color: '#ec4899',
        description: '퀴즈 관련 알림',
    },
    FRIEND: {
        icon: UserPlus,
        color: '#06b6d4',
        description: '친구 요청 및 수락 알림',
    },
    SYSTEM: {
        icon: Settings,
        color: '#64748b',
        description: '서비스 공지 및 시스템 알림',
    },
};

export const NotificationSection = () => {
    const { notificationSettings, updateNotificationSetting, isSaving } = useSettingStore();

    // 알림 토글 핸들러
    const handleToggle = (type: NotificationType, isEnabled: boolean) => {
        updateNotificationSetting(type, isEnabled);
    };

    return (
        <section className="setting-section">
            {/* 섹션 헤더 */}
            <div className="section-header">
                <h2 className="section-title">
                    <Bell className="section-title-icon" />
                    알림 설정
                </h2>
                <p className="section-description">
                    수신할 알림 종류를 설정합니다. 필요한 알림만 켜두면 중요한 정보를 놓치지 않을 수 있습니다.
                </p>
            </div>

            {/* 알림 설정 목록 */}
            <div className="notification-list">
                {(notificationSettings || []).map((setting) => {
                    const config = notificationConfig[setting.type];
                    // config가 없는 경우 기본값 사용
                    if (!config) {
                        return null;
                    }
                    const Icon = config.icon;

                    return (
                        <div key={setting.type} className="setting-item">
                            <div className="setting-item-info">
                                <div
                                    className="setting-item-icon"
                                    style={{ color: config.color }}
                                >
                                    <Icon />
                                </div>
                                <div className="setting-item-text">
                                    <span className="setting-item-label">{setting.typeName}</span>
                                    <span className="setting-item-desc">{config.description}</span>
                                </div>
                            </div>
                            <ToggleSwitch
                                checked={setting.isEnabled}
                                onChange={(checked) => handleToggle(setting.type, checked)}
                                disabled={isSaving}
                                ariaLabel={`${setting.typeName} 알림 ${setting.isEnabled ? '끄기' : '켜기'}`}
                            />
                        </div>
                    );
                })}

                {/* 데이터가 없는 경우 */}
                {notificationSettings.length === 0 && (
                    <div className="setting-item">
                        <p style={{ color: '#94a3b8', textAlign: 'center', width: '100%' }}>
                            알림 설정을 불러오는 중입니다...
                        </p>
                    </div>
                )}
            </div>
        </section>
    );
};
