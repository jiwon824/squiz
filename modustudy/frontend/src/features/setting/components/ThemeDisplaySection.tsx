/**
 * ThemeDisplaySection 컴포넌트
 * 테마/디스플레이 설정 섹션을 담당합니다.
 *
 * TODO: 백엔드 API 구현 후 활성화 필요
 * - 테마 설정 (라이트/다크/시스템)
 * - 글자 크기 설정
 */

import { Palette, Sun, Moon, Monitor, Type, AlertCircle } from 'lucide-react';
import { Button } from '@/shared/components/Button';

export const ThemeDisplaySection = () => {
    // TODO: 테마 설정 상태 (API 구현 후 스토어로 이동)
    // const [theme, setTheme] = useState<'light' | 'dark' | 'system'>('light');
    // const [fontSize, setFontSize] = useState<'small' | 'medium' | 'large'>('medium');

    return (
        <section className="setting-section todo-section">
            {/* 섹션 헤더 */}
            <div className="section-header">
                <h2 className="section-title">
                    <Palette className="section-title-icon" />
                    테마 / 디스플레이
                    <span className="todo-badge">
                        <AlertCircle size={12} />
                        준비 중
                    </span>
                </h2>
                <p className="section-description">
                    화면 표시 설정을 관리합니다. 이 기능은 현재 준비 중입니다.
                </p>
            </div>

            {/* 안내 메시지 */}
            <div className="warning-message" style={{ marginBottom: '1.5rem' }}>
                <AlertCircle size={20} />
                <span>
                    테마 및 디스플레이 설정 기능은 현재 개발 중입니다. 곧 다양한 테마와 디스플레이 옵션을 제공할 예정입니다.
                </span>
            </div>

            {/* 테마 설정 (비활성화) */}
            <div className="setting-item" style={{ opacity: 0.5, pointerEvents: 'none' }}>
                <div className="setting-item-info">
                    <div className="setting-item-icon" style={{ color: '#f59e0b' }}>
                        <Sun />
                    </div>
                    <div className="setting-item-text">
                        <span className="setting-item-label">테마</span>
                        <span className="setting-item-desc">화면 테마를 선택합니다.</span>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button variant="secondary" size="sm" disabled leftIcon={<Sun size={14} />}>
                        라이트
                    </Button>
                    <Button variant="secondary" size="sm" disabled leftIcon={<Moon size={14} />}>
                        다크
                    </Button>
                    <Button variant="secondary" size="sm" disabled leftIcon={<Monitor size={14} />}>
                        시스템
                    </Button>
                </div>
            </div>

            {/* 글자 크기 설정 (비활성화) */}
            <div className="setting-item" style={{ opacity: 0.5, pointerEvents: 'none' }}>
                <div className="setting-item-info">
                    <div className="setting-item-icon" style={{ color: '#8b5cf6' }}>
                        <Type />
                    </div>
                    <div className="setting-item-text">
                        <span className="setting-item-label">글자 크기</span>
                        <span className="setting-item-desc">화면에 표시되는 글자 크기를 조절합니다.</span>
                    </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', minWidth: '200px' }}>
                    <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>작게</span>
                    <input
                        type="range"
                        min="1"
                        max="3"
                        defaultValue="2"
                        disabled
                        style={{ flex: 1 }}
                    />
                    <span style={{ fontSize: '0.875rem', color: '#94a3b8' }}>크게</span>
                </div>
            </div>

            {/* TODO 주석 */}
            {/*
            TODO: API 구현 후 아래 기능 활성화

            1. 테마 설정 API
               - GET /api/v1/users/me/settings/theme
               - PUT /api/v1/users/me/settings/theme
               - 값: 'light' | 'dark' | 'system'

            2. 글자 크기 설정 API
               - GET /api/v1/users/me/settings/font-size
               - PUT /api/v1/users/me/settings/font-size
               - 값: 'small' | 'medium' | 'large'

            3. 로컬 스토리지 연동
               - 테마 변경 시 즉시 적용
               - 페이지 로드 시 저장된 설정 복원
            */}
        </section>
    );
};
